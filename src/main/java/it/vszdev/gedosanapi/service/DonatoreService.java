package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.dto.donatore.DatiDonatoreRequest;
import it.vszdev.gedosanapi.exception.EmailAssociataAdAltroDonatoreException;
import it.vszdev.gedosanapi.models.Donatore;
import it.vszdev.gedosanapi.repositories.DonatoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DonatoreService {

    private final DonatoreRepository donatoreRepository;

    public DonatoreService(DonatoreRepository donatoreRepository) {
        this.donatoreRepository = donatoreRepository;
    }

    // Il match è sul codice fiscale, non sull'email: la stessa persona può tornare con contatti diversi.
    @Transactional
    public Donatore risolviDonatore(DatiDonatoreRequest dati) {
        String codiceFiscale = Donatore.normalizzaCodiceFiscale(dati.codiceFiscale());
        String email = Donatore.normalizzaEmail(dati.email());

        return donatoreRepository.findByCodiceFiscale(codiceFiscale)
                .map(donatoreEsistente -> aggiornaDonatore(donatoreEsistente, dati, email))
                .orElseGet(() -> creaDonatore(dati, codiceFiscale, email));
    }

    private Donatore aggiornaDonatore(Donatore donatore, DatiDonatoreRequest dati, String email) {
        if (emailAppartieneAdAltroDonatore(email, donatore.getId())) {
            throw new EmailAssociataAdAltroDonatoreException();
        }

        donatore.setNome(dati.nome());
        donatore.setCognome(dati.cognome());
        donatore.setDataNascita(dati.dataNascita());
        donatore.setSesso(dati.sesso());
        donatore.setCellulare(dati.cellulare());
        donatore.setEmail(email);
        return donatoreRepository.save(donatore);
    }

    private Donatore creaDonatore(DatiDonatoreRequest dati, String codiceFiscale, String email) {
        if (emailAppartieneAdAltroDonatore(email, null)) {
            throw new EmailAssociataAdAltroDonatoreException();
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
