package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.dto.donatore.DatiDonatoreRequest;
import it.vszdev.gedosanapi.exception.RegolaPrenotazioneException;
import it.vszdev.gedosanapi.exception.RisorsaNonTrovataException;
import it.vszdev.gedosanapi.models.Donatore;
import it.vszdev.gedosanapi.repositories.DonatoreRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DonatoreService {

    private final DonatoreRepository donatoreRepository;

    public DonatoreService(DonatoreRepository donatoreRepository) {
        this.donatoreRepository = donatoreRepository;
    }

    // Il match è sul codice fiscale, non sull'email: la stessa persona può tornare con contatti diversi.
    // REQUIRES_NEW (come SlotGiornalieroService.risolviSlot): l'insert del donatore viene committato
    // subito, così il vincolo uq_donatori_codice_fiscale fa da arbitro tra due prime prenotazioni
    // concorrenti dello stesso CF e chi perde la corsa rilegge la riga altrui invece di ricevere un
    // errore. Di conseguenza la risoluzione/aggiornamento dell'anagrafica persiste anche se la
    // prenotazione successiva fallisce una validazione; un tentativo corretto successivo la riallinea.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Donatore risolviDonatore(DatiDonatoreRequest dati) {
        String codiceFiscale = Donatore.normalizzaCodiceFiscale(dati.codiceFiscale());
        String email = Donatore.normalizzaEmail(dati.email());

        return donatoreRepository.findByCodiceFiscale(codiceFiscale)
                .map(donatoreEsistente -> aggiornaDonatore(donatoreEsistente, dati, email))
                .orElseGet(() -> creaDonatoreSicuro(dati, codiceFiscale, email));
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

    // Stesso pattern di SlotGiornalieroService.creaSlotSicuro: se due richieste concorrenti provano a
    // creare lo stesso CF, il vincolo UNIQUE ne fa fallire una; chi perde rilegge la riga già creata.
    private Donatore creaDonatoreSicuro(DatiDonatoreRequest dati, String codiceFiscale, String email) {
        if (emailAppartieneAdAltroDonatore(email, null)) {
            throw new RegolaPrenotazioneException("L'email inserita è già associata a un altro donatore.");
        }

        try {
            return donatoreRepository.save(new Donatore(
                    dati.nome(),
                    dati.cognome(),
                    dati.dataNascita(),
                    dati.sesso(),
                    codiceFiscale,
                    dati.cellulare(),
                    email,
                    null));
        } catch (DataIntegrityViolationException e) {
            return donatoreRepository.findByCodiceFiscale(codiceFiscale).orElseThrow(() -> e);
        }
    }

    private boolean emailAppartieneAdAltroDonatore(String email, Long idDonatoreCorrente) {
        return donatoreRepository.findByEmail(email)
                .map(Donatore::getId)
                .filter(id -> !id.equals(idDonatoreCorrente))
                .isPresent();
    }
}
