package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.config.PrenotazioniProperties;
import it.vszdev.gedosanapi.config.TimeConfig;
import it.vszdev.gedosanapi.dto.PrenotazioneRequest;
import it.vszdev.gedosanapi.dto.donatore.DatiDonatoreRequest;
import it.vszdev.gedosanapi.dto.donatore.PrenotazioneResponse;
import it.vszdev.gedosanapi.dto.email.ConfermaPrenotazioneEmailPayload;
import it.vszdev.gedosanapi.enums.Sesso;
import it.vszdev.gedosanapi.events.PrenotazioneEffettuataEvent;
import it.vszdev.gedosanapi.exception.RegolaPrenotazioneException;
import it.vszdev.gedosanapi.exception.RisorsaNonTrovataException;
import it.vszdev.gedosanapi.models.Donatore;
import it.vszdev.gedosanapi.models.Prenotazione;
import it.vszdev.gedosanapi.models.SlotGiornaliero;
import it.vszdev.gedosanapi.repositories.PrenotazioneRepository;
import it.vszdev.gedosanapi.repositories.SlotGiornalieroRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class PrenotazioneService {

    private final DonatoreService donatoreService;
    private final SlotGiornalieroRepository slotGiornalieroRepository;
    private final PrenotazioneRepository prenotazioneRepository;
    private final PrenotazioniProperties prenotazioniProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final DisponibilitaService disponibilitaService;
    private final Clock clock;

    public PrenotazioneService(DonatoreService donatoreService,
                               SlotGiornalieroRepository slotGiornalieroRepository,
                               PrenotazioneRepository prenotazioneRepository,
                               PrenotazioniProperties prenotazioniProperties,
                               ApplicationEventPublisher eventPublisher,
                               DisponibilitaService disponibilitaService,
                               Clock clock) {
        this.donatoreService = donatoreService;
        this.slotGiornalieroRepository = slotGiornalieroRepository;
        this.prenotazioneRepository = prenotazioneRepository;
        this.prenotazioniProperties = prenotazioniProperties;
        this.eventPublisher = eventPublisher;
        this.disponibilitaService = disponibilitaService;
        this.clock = clock;
    }

    @Transactional
    public PrenotazioneResponse crea(PrenotazioneRequest request) {
        return new PrenotazioneResponse(creaPrenotazione(request, false));
    }

    @Transactional
    public Prenotazione creaPrenotazione(PrenotazioneRequest request, boolean flussoAdmin) {
        // Pre-check dell'età prima di toccare l'anagrafica: risolviDonatore committa in REQUIRES_NEW,
        // quindi un donatore non idoneo per età lascerebbe altrimenti una riga in `donatori`. Lettura
        // non bloccante dello slot (l'ordine dei lock resta donatore -> slot, la data non cambia).
        LocalDate dataSlot = slotGiornalieroRepository.findById(request.idSlot())
                .orElseThrow(() -> new RisorsaNonTrovataException("Slot non trovato"))
                .getData();
        validaEta(request.dataNascita(), dataSlot);

        Donatore donatore = donatoreService.risolviDonatore(datiDonatore(request));
        // Ordine di lock: prima il donatore, poi lo slot. Evita prenotazioni doppie concorrenti
        // dello stesso donatore che supererebbero entrambe il controllo dell'intervallo minimo.
        donatore = donatoreService.bloccaPerPrenotazione(donatore.getId());

        SlotGiornaliero slot = slotGiornalieroRepository.findWithLockById(request.idSlot())
                .orElseThrow(() -> new RisorsaNonTrovataException("Slot non trovato"));

        ricalcolaPostiOccupati(slot);
        validaSlotPrenotabile(slot, donatore, null, flussoAdmin);

        Prenotazione prenotazione = new Prenotazione(donatore, slot, request.tipoDonazione());
        prenotazioneRepository.save(prenotazione);
        ricalcolaPostiOccupati(slot);

        eventPublisher.publishEvent(new PrenotazioneEffettuataEvent(new ConfermaPrenotazioneEmailPayload(prenotazione)));

        return prenotazione;
    }

    private DatiDonatoreRequest datiDonatore(PrenotazioneRequest request) {
        return new DatiDonatoreRequest(
                request.nome(),
                request.cognome(),
                request.dataNascita(),
                request.sesso(),
                request.codiceFiscale(),
                request.email(),
                request.cellulare());
    }

    // Regole condivise tra flusso donatore e admin. Unica differenza: l'anticipo minimo (banco
    // accettazione) non si applica all'admin, che però non può comunque prenotare uno slot già passato.
    void validaSlotPrenotabile(SlotGiornaliero slot, Donatore donatore, Long idPrenotazioneEsclusa, boolean flussoAdmin) {
        LocalDate oggi = LocalDate.now(clock);

        if (slot.getData().isAfter(oggi.plusDays(prenotazioniProperties.getOrizzonteGiorni()))) {
            throw new RegolaPrenotazioneException("La data richiesta è troppo lontana: si può prenotare al massimo "
                    + prenotazioniProperties.getOrizzonteGiorni() + " giorni in anticipo.");
        }
        validaAnticipo(slot, flussoAdmin);
        if (!disponibilitaService.giornoAperto(slot.getTrasfusionale(), slot.getData())) {
            throw new RegolaPrenotazioneException(
                    "Il trasfusionale selezionato è chiuso nella data richiesta. Scegli un'altra data.");
        }
        if (slot.getPostiOccupati() >= prenotazioniProperties.getPostiPerSlot()) {
            throw new RegolaPrenotazioneException(
                    "Lo slot selezionato non è più disponibile. Scegli un altro orario.");
        }
        validaEta(donatore.getDataNascita(), slot.getData());
        validaIntervallo(donatore, slot.getData(), idPrenotazioneEsclusa);
    }

    // Anticipo minimo: l'inizio dello slot deve distare almeno anticipoMinimoMinuti da adesso
    // (confine incluso: 90 minuti esatti sono ammessi). L'admin è esente dall'anticipo ma non
    // può prenotare uno slot il cui orario è già passato. Il confronto è sulla timeline degli istanti
    // (ZonedDateTime): plusMinutes aggiunge minuti reali, corretto anche a cavallo del cambio ora.
    private void validaAnticipo(SlotGiornaliero slot, boolean flussoAdmin) {
        ZonedDateTime inizioSlot = LocalDateTime.of(slot.getData(), slot.getOrario().getOrario()).atZone(TimeConfig.ZONA);
        ZonedDateTime adesso = ZonedDateTime.now(clock);

        if (flussoAdmin) {
            if (inizioSlot.isBefore(adesso)) {
                throw new RegolaPrenotazioneException("L'orario selezionato è già trascorso.");
            }
            return;
        }

        int anticipo = prenotazioniProperties.getAnticipoMinimoMinuti();
        if (inizioSlot.isBefore(adesso.plusMinutes(anticipo))) {
            throw new RegolaPrenotazioneException("Puoi prenotare uno slot solo con almeno " + anticipo
                    + " minuti di anticipo rispetto al suo orario. Scegli un orario successivo.");
        }
    }

    // Ricalcola posti_occupati dal numero reale di prenotazioni sullo slot invece di incrementare/
    // decrementare il contatore: il valore resta sempre autoritativo e non può derivare (niente
    // decrementi sotto zero né doppi conteggi). Va chiamato DOPO save/delete/spostamento della
    // prenotazione; il flush rende visibili le modifiche pendenti al COUNT.
    void ricalcolaPostiOccupati(SlotGiornaliero slot) {
        prenotazioneRepository.flush();
        slot.setPostiOccupati(Math.toIntExact(prenotazioneRepository.countBySlotGiornaliero_Id(slot.getId())));
    }

    void validaEta(LocalDate dataNascita, LocalDate dataDonazione) {
        int eta = Period.between(dataNascita, dataDonazione).getYears();
        if (eta < prenotazioniProperties.getEtaMinima()) {
            throw new RegolaPrenotazioneException("Devi avere almeno "
                    + prenotazioniProperties.getEtaMinima() + " anni per prenotare una donazione.");
        }
        if (eta > prenotazioniProperties.getEtaMassima()) {
            throw new RegolaPrenotazioneException("Superata l'età massima di "
                    + prenotazioniProperties.getEtaMassima()
                    + " anni per prenotare online. Contatta il trasfusionale per valutare l'idoneità.");
        }
    }

    // Una prenotazione si può fare solo in avanti nel tempo rispetto alle proprie prenotazioni esistenti:
    // niente inserimenti tra due date già prenotate (per anticipare si riprogramma o si annulla). Vale
    // anche per l'admin. Il confronto con l'ultima prenotazione per data equivale quindi al confronto
    // con la più vicina.
    void validaIntervallo(Donatore donatore, LocalDate dataDonazione, Long idPrenotazioneEsclusa) {
        var ultima = idPrenotazioneEsclusa == null
                ? prenotazioneRepository.findFirstByDonatore_IdOrderBySlotGiornaliero_DataDesc(donatore.getId())
                : prenotazioneRepository.findFirstByDonatore_IdAndIdNotOrderBySlotGiornaliero_DataDesc(donatore.getId(), idPrenotazioneEsclusa);

        int intervalloMinimo = donatore.getSesso() == Sesso.M
                ? prenotazioniProperties.getIntervalloUominiGiorni()
                : prenotazioniProperties.getDistanzaMinimaDonneGiorni();

        ultima.ifPresent(ultimaPrenotazione -> {
            long giorniTrascorsi = ChronoUnit.DAYS.between(ultimaPrenotazione.getSlotGiornaliero().getData(), dataDonazione);
            if (giorniTrascorsi == 0) {
                throw new RegolaPrenotazioneException("Hai già una prenotazione per quel giorno.");
            }
            if (giorniTrascorsi < 0) {
                throw new RegolaPrenotazioneException("Hai già una prenotazione in una data successiva "
                        + "a quella scelta. Per anticiparla contatta il trasfusionale.");
            }
            if (giorniTrascorsi < intervalloMinimo) {
                throw new RegolaPrenotazioneException("Intervallo minimo tra due donazioni non rispettato. "
                        + "Contatta il trasfusionale per maggiori dettagli.");
            }
        });

        if (donatore.getSesso() == Sesso.F) {
            validaLimiteDonneNellaFinestra(donatore, dataDonazione, idPrenotazioneEsclusa);
        }
    }

    // Limite per le donatrici: nei 'finestraLimiteDonneGiorni' giorni che precedono la data richiesta
    // non possono esserci già 'maxDonazioniDonnePerFinestra' donazioni. Il conteggio "a ritroso" è
    // sufficiente perché il controllo del gap minimo impedisce di prenotare una data anteriore a una
    // prenotazione esistente: le prenotazioni sono quindi sempre in ordine cronologico.
    private void validaLimiteDonneNellaFinestra(Donatore donatore, LocalDate dataDonazione, Long idPrenotazioneEsclusa) {
        int finestra = prenotazioniProperties.getFinestraLimiteDonneGiorni();
        int massimo = prenotazioniProperties.getMaxDonazioniDonnePerFinestra();

        LocalDate da = dataDonazione.minusDays(finestra);
        LocalDate a = dataDonazione.minusDays(1);

        long donazioniPrecedenti = idPrenotazioneEsclusa == null
                ? prenotazioneRepository.countByDonatore_IdAndSlotGiornaliero_DataBetween(donatore.getId(), da, a)
                : prenotazioneRepository.countByDonatore_IdAndIdNotAndSlotGiornaliero_DataBetween(donatore.getId(), idPrenotazioneEsclusa, da, a);

        if (donazioniPrecedenti >= massimo) {
            throw new RegolaPrenotazioneException("Le donatrici possono effettuare al massimo " + massimo
                    + " donazioni di sangue intero in 12 mesi. Contatta il trasfusionale per maggiori informazioni.");
        }
    }
}
