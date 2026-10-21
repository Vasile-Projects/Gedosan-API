package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.dto.donatore.DatiDonatoreRequest;
import it.vszdev.gedosanapi.exception.RegolaPrenotazioneException;
import it.vszdev.gedosanapi.exception.RisorsaNonTrovataException;
import it.vszdev.gedosanapi.models.Donatore;
import it.vszdev.gedosanapi.repositories.DonatoreRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Service
public class DonatoreService {

    private final DonatoreRepository donatoreRepository;
    private final TransactionTemplate nuovaTransazione;

    public DonatoreService(DonatoreRepository donatoreRepository, PlatformTransactionManager transactionManager) {
        this.donatoreRepository = donatoreRepository;
        this.nuovaTransazione = new TransactionTemplate(transactionManager);
        this.nuovaTransazione.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // Solo flusso admin (operatore fidato): un donatore esistente viene aggiornato con i dati inseriti.
    // Il flusso pubblico non passa di qui e non aggiorna mai un'anagrafica esistente (vedi creaDonatore).
    // Il match è sul codice fiscale, non sull'email: la stessa persona può tornare con contatti diversi.
    // Transazione propria (vedi inNuovaTransazione): l'insert del donatore viene committato subito,
    // così il vincolo uq_donatori_codice_fiscale fa da arbitro tra due prime prenotazioni concorrenti
    // dello stesso CF e chi perde la corsa ritrova la riga altrui invece di ricevere un errore. Di
    // conseguenza la risoluzione/aggiornamento dell'anagrafica persiste anche se la prenotazione
    // successiva fallisce una validazione; un tentativo corretto successivo la riallinea.
    public Donatore risolviDonatore(DatiDonatoreRequest dati) {
        String codiceFiscale = Donatore.normalizzaCodiceFiscale(dati.codiceFiscale());
        String email = Donatore.normalizzaEmail(dati.email());

        return inNuovaTransazione(() -> donatoreRepository.findByCodiceFiscale(codiceFiscale)
                .map(donatoreEsistente -> aggiornaDonatore(donatoreEsistente, dati, email))
                .orElseGet(() -> inserisciDonatore(dati, codiceFiscale, email)));
    }

    // Flusso pubblico, alla conferma via email: crea il donatore se il CF non esiste. Mai aggiornamenti.
    // Stessa transazione propria di risolviDonatore, con il vincolo UNIQUE sul CF come arbitro: se
    // un'altra richiesta crea lo stesso CF in parallelo si restituisce la riga altrui, e il chiamante
    // verifica che l'email coincida prima di usarla.
    public Donatore creaDonatore(DatiDonatoreRequest dati) {
        String codiceFiscale = Donatore.normalizzaCodiceFiscale(dati.codiceFiscale());
        String email = Donatore.normalizzaEmail(dati.email());

        return inNuovaTransazione(() -> donatoreRepository.findByCodiceFiscale(codiceFiscale)
                .orElseGet(() -> inserisciDonatore(dati, codiceFiscale, email)));
    }

    // Lock pessimistico sulla riga del donatore: serializza le prenotazioni concorrenti dello stesso
    // donatore, così il controllo dell'intervallo minimo vede sempre lo stato committato dalle altre.
    // Va acquisito PRIMA del lock sugli slot in ogni percorso di scrittura (ordine donatore -> slot).
    @Transactional
    public Donatore bloccaPerPrenotazione(Long idDonatore) {
        return donatoreRepository.findWithLockById(idDonatore)
                .orElseThrow(() -> new RisorsaNonTrovataException("Donatore non trovato"));
    }

    private Donatore aggiornaDonatore(Donatore donatore, DatiDonatoreRequest dati, String email) {
        if (emailAppartieneAdAltroDonatore(email, donatore.getId())) {
            throw new RegolaPrenotazioneException("L'email inserita è già associata a un altro donatore.");
        }

        donatore.setNome(dati.nome());
        donatore.setCognome(dati.cognome());
        donatore.setDataNascita(dati.dataNascita());
        donatore.setSesso(dati.sesso());
        donatore.setCellulare(dati.cellulare());
        donatore.setEmail(email);
        return donatoreRepository.save(donatore);
    }

    // Come SlotGiornalieroService.risolviSlot: REQUIRES_NEW, e se due richieste concorrenti creano lo
    // stesso CF il vincolo UNIQUE ne fa fallire una. Chi perde non può rileggere nella stessa
    // transazione (dopo l'eccezione la sessione di Hibernate è inutilizzabile): rollback e un secondo
    // tentativo in una transazione nuova, che trova la riga del vincitore.
    private Donatore inNuovaTransazione(Supplier<Donatore> operazione) {
        try {
            return nuovaTransazione.execute(stato -> operazione.get());
        } catch (DataIntegrityViolationException e) {
            return nuovaTransazione.execute(stato -> operazione.get());
        }
    }

    private Donatore inserisciDonatore(DatiDonatoreRequest dati, String codiceFiscale, String email) {
        if (emailAppartieneAdAltroDonatore(email, null)) {
            throw new RegolaPrenotazioneException("L'email inserita è già associata a un altro donatore.");
        }

        return donatoreRepository.save(new Donatore(
                dati.nome(),
                dati.cognome(),
                dati.dataNascita(),
                dati.sesso(),
                codiceFiscale,
                dati.cellulare(),
                email,
                null));
    }

    private boolean emailAppartieneAdAltroDonatore(String email, Long idDonatoreCorrente) {
        return donatoreRepository.findByEmail(email)
                .map(Donatore::getId)
                .filter(id -> !id.equals(idDonatoreCorrente))
                .isPresent();
    }
}
