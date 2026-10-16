package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.config.PrenotazioniProperties;
import it.vszdev.gedosanapi.dto.PrenotazioneRequest;
import it.vszdev.gedosanapi.dto.donatore.DatiDonatoreRequest;
import it.vszdev.gedosanapi.dto.donatore.PrenotazioneResponse;
import it.vszdev.gedosanapi.dto.email.ConfermaPrenotazioneEmailPayload;
import it.vszdev.gedosanapi.enums.TipoAvviso;
import it.vszdev.gedosanapi.events.AvvisoPrenotazioneEvent;
import it.vszdev.gedosanapi.events.LinkConfermaEvent;
import it.vszdev.gedosanapi.events.PrenotazioneEffettuataEvent;
import it.vszdev.gedosanapi.exception.RegolaPrenotazioneException;
import it.vszdev.gedosanapi.exception.RisorsaNonTrovataException;
import it.vszdev.gedosanapi.models.Donatore;
import it.vszdev.gedosanapi.models.Prenotazione;
import it.vszdev.gedosanapi.models.PrenotazioneInAttesa;
import it.vszdev.gedosanapi.models.SlotGiornaliero;
import it.vszdev.gedosanapi.repositories.DonatoreRepository;
import it.vszdev.gedosanapi.repositories.PrenotazioneInAttesaRepository;
import it.vszdev.gedosanapi.repositories.PrenotazioneRepository;
import it.vszdev.gedosanapi.repositories.SlotGiornalieroRepository;
import it.vszdev.gedosanapi.security.TokenConferma;
import it.vszdev.gedosanapi.security.TurnstileService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

// Flusso pubblico di prenotazione in due passi. Il donatore non ha un account: l'identità si dimostra
// con il possesso dell'email. La richiesta crea solo un'attesa che tiene il posto; donatore e
// prenotazione nascono quando il link inviato all'email viene confermato.
// Ordine dei lock in ogni percorso di scrittura: attesa -> donatore -> slot (slot per id crescente).
@Service
public class RichiestaPrenotazioneService {

    private final PrenotazioneService prenotazioneService;
    private final DonatoreService donatoreService;
    private final TurnstileService turnstileService;
    private final DonatoreRepository donatoreRepository;
    private final SlotGiornalieroRepository slotGiornalieroRepository;
    private final PrenotazioneRepository prenotazioneRepository;
    private final PrenotazioneInAttesaRepository prenotazioneInAttesaRepository;
    private final PrenotazioniProperties prenotazioniProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public RichiestaPrenotazioneService(PrenotazioneService prenotazioneService,
                                        DonatoreService donatoreService,
                                        TurnstileService turnstileService,
                                        DonatoreRepository donatoreRepository,
                                        SlotGiornalieroRepository slotGiornalieroRepository,
                                        PrenotazioneRepository prenotazioneRepository,
                                        PrenotazioneInAttesaRepository prenotazioneInAttesaRepository,
                                        PrenotazioniProperties prenotazioniProperties,
                                        ApplicationEventPublisher eventPublisher,
                                        Clock clock) {
        this.prenotazioneService = prenotazioneService;
        this.donatoreService = donatoreService;
        this.turnstileService = turnstileService;
        this.donatoreRepository = donatoreRepository;
        this.slotGiornalieroRepository = slotGiornalieroRepository;
        this.prenotazioneRepository = prenotazioneRepository;
        this.prenotazioneInAttesaRepository = prenotazioneInAttesaRepository;
        this.prenotazioniProperties = prenotazioniProperties;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    // Passo 1. Errori immediati (eccezioni) solo per ciò che non riguarda la storia della persona:
    // anti-bot, slot inesistente, calendario, anticipo, capienza, età calcolata dai dati del modulo.
    // Tutto il resto (CF registrato con altra email, email di un altro donatore, regole sulla storia
    // delle donazioni) termina senza eccezioni con un avviso via email al proprietario dell'indirizzo:
    // il chiamante risponde sempre con lo stesso messaggio generico.
    @Transactional
    public void richiedi(PrenotazioneRequest request) {
        turnstileService.verifica(request.turnstileToken());

        // Pre-controllo senza lock: risposta rapida per gli errori evidenti, ripetuto sotto lock più avanti.
        SlotGiornaliero slotLetto = trovaSlot(request.idSlot());
        prenotazioneService.validaSlotDisponibile(slotLetto, false);
        prenotazioneService.validaEta(request.dataNascita(), slotLetto.getData());

        String codiceFiscale = Donatore.normalizzaCodiceFiscale(request.codiceFiscale());
        String email = Donatore.normalizzaEmail(request.email());

        Optional<Donatore> esistente = donatoreRepository.findByCodiceFiscale(codiceFiscale);
        if (esistente.isPresent() && !esistente.get().getEmail().equals(email)) {
            avvisa(esistente.get(), TipoAvviso.DATI_DIVERSI, null);
            return;
        }
        if (esistente.isEmpty()) {
            Optional<Donatore> proprietarioEmail = donatoreRepository.findByEmail(email);
            if (proprietarioEmail.isPresent()) {
                avvisa(proprietarioEmail.get(), TipoAvviso.EMAIL_IN_USO, null);
                return;
            }
        }
        if (esistente.isPresent()) {
            // Controllo anticipato sulla storia del donatore, senza lock: evita di sostituire un'attesa
            // valida con una richiesta destinata a fallire. Alla conferma le regole si ripetono sotto lock.
            try {
                prenotazioneService.validaIntervallo(esistente.get(), slotLetto.getData(), null);
            } catch (RegolaPrenotazioneException e) {
                avvisa(esistente.get(), TipoAvviso.REGOLA_NON_RISPETTATA, e.getMessage());
                return;
            }
        }

        // Una sola attesa per CF: la nuova richiesta sostituisce la precedente. Lock dell'attesa prima
        // degli slot; se la precedente era su un altro slot vanno bloccati entrambi, per id crescente.
        Optional<PrenotazioneInAttesa> precedente = prenotazioneInAttesaRepository.findWithLockByCodiceFiscale(codiceFiscale);
        Long idSlotPrecedente = precedente.map(a -> a.getSlotGiornaliero().getId()).orElse(null);
        SlotGiornaliero slot = bloccaSlot(request.idSlot(), idSlotPrecedente);

        precedente.ifPresent(attesa -> {
            prenotazioneInAttesaRepository.delete(attesa);
            if (!attesa.getSlotGiornaliero().getId().equals(slot.getId())) {
                prenotazioneService.ricalcolaPostiOccupati(attesa.getSlotGiornaliero());
            }
        });

        // Controllo autoritativo sotto lock: un errore qui annulla anche la cancellazione della precedente.
        prenotazioneService.ricalcolaPostiOccupati(slot);
        prenotazioneService.validaSlotDisponibile(slot, false);

        String token = TokenConferma.genera();
        LocalDateTime scadenza = LocalDateTime.now(clock).plusMinutes(prenotazioniProperties.getMinutiAttesaConferma());
        PrenotazioneInAttesa attesa = prenotazioneInAttesaRepository.save(new PrenotazioneInAttesa(
                slot,
                request.nome(),
                request.cognome(),
                request.dataNascita(),
                request.sesso(),
                codiceFiscale,
                email,
                request.cellulare(),
                request.tipoDonazione(),
                TokenConferma.hash(token),
                scadenza));
        prenotazioneService.ricalcolaPostiOccupati(slot);

        // Per un donatore già registrato il saluto usa il nome salvato, non quello digitato nel modulo.
        String nome = esistente.map(Donatore::getNome).orElse(attesa.getNome());
        eventPublisher.publishEvent(new LinkConfermaEvent(
                email,
                nome,
                token,
                slot.getTrasfusionale().getNome(),
                slot.getData(),
                slot.getOrario().getOrario(),
                scadenza));
    }

    // Passo 2. Chi arriva qui possiede l'email: i messaggi delle regole possono essere espliciti.
    @Transactional
    public PrenotazioneResponse conferma(String token) {
        PrenotazioneInAttesa attesa = prenotazioneInAttesaRepository.findWithLockByTokenHash(TokenConferma.hash(token))
                .filter(a -> !a.scadutaAl(LocalDateTime.now(clock)))
                .orElseThrow(() -> new RisorsaNonTrovataException(
                        "Il link di conferma non è valido o è scaduto. Ripeti la prenotazione."));

        // Il donatore si crea solo adesso. Se il CF esiste già, l'email deve coincidere ancora: l'admin
        // potrebbe averla cambiata dopo la richiesta, o un'altra conferma aver creato lo stesso CF.
        Donatore donatore = donatoreRepository.findByCodiceFiscale(attesa.getCodiceFiscale())
                .orElseGet(() -> donatoreService.creaDonatore(datiDonatore(attesa)));
        if (!donatore.getEmail().equals(attesa.getEmail())) {
            throw new RegolaPrenotazioneException("I dati registrati per questo codice fiscale sono diversi da quelli "
                    + "della richiesta. Contatta il tuo centro trasfusionale per aggiornarli.");
        }
        donatore = donatoreService.bloccaPerPrenotazione(donatore.getId());

        SlotGiornaliero slot = slotGiornalieroRepository.findWithLockById(attesa.getSlotGiornaliero().getId())
                .orElseThrow(() -> new RisorsaNonTrovataException("Slot non trovato"));

        // L'attesa libera il proprio posto prima della verifica, altrimenti conterebbe se stessa.
        prenotazioneInAttesaRepository.delete(attesa);
        prenotazioneService.ricalcolaPostiOccupati(slot);
        prenotazioneService.validaSlotPrenotabile(slot, donatore, null, false);

        Prenotazione prenotazione = new Prenotazione(donatore, slot, attesa.getTipoDonazione());
        prenotazioneRepository.save(prenotazione);
        prenotazioneService.ricalcolaPostiOccupati(slot);

        eventPublisher.publishEvent(new PrenotazioneEffettuataEvent(new ConfermaPrenotazioneEmailPayload(prenotazione)));

        return new PrenotazioneResponse(prenotazione);
    }

    private void avvisa(Donatore destinatario, TipoAvviso tipo, String dettaglio) {
        eventPublisher.publishEvent(new AvvisoPrenotazioneEvent(destinatario.getEmail(), destinatario.getNome(), tipo, dettaglio));
    }

    private SlotGiornaliero trovaSlot(Long idSlot) {
        return slotGiornalieroRepository.findById(idSlot)
                .orElseThrow(() -> new RisorsaNonTrovataException("Slot non trovato"));
    }

    // Blocca lo slot richiesto e, se diverso, quello dell'attesa precedente, in ordine di id crescente
    // (stesso criterio di AdminPrenotazioneService.riprogramma) per evitare deadlock tra richieste.
    private SlotGiornaliero bloccaSlot(Long idSlotRichiesto, Long idSlotPrecedente) {
        if (idSlotPrecedente != null && !idSlotPrecedente.equals(idSlotRichiesto)) {
            Long idPrimo = Math.min(idSlotRichiesto, idSlotPrecedente);
            Long idSecondo = Math.max(idSlotRichiesto, idSlotPrecedente);
            SlotGiornaliero primo = bloccaSlot(idPrimo);
            SlotGiornaliero secondo = bloccaSlot(idSecondo);
            return idSlotRichiesto.equals(idPrimo) ? primo : secondo;
        }
        return bloccaSlot(idSlotRichiesto);
    }

    private SlotGiornaliero bloccaSlot(Long id) {
        return slotGiornalieroRepository.findWithLockById(id)
                .orElseThrow(() -> new RisorsaNonTrovataException("Slot non trovato"));
    }

    private DatiDonatoreRequest datiDonatore(PrenotazioneInAttesa attesa) {
        return new DatiDonatoreRequest(
                attesa.getNome(),
                attesa.getCognome(),
                attesa.getDataNascita(),
                attesa.getSesso(),
                attesa.getCodiceFiscale(),
                attesa.getEmail(),
                attesa.getCellulare());
    }
}
