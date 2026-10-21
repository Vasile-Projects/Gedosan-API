package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.models.OrarioValido;
import it.vszdev.gedosanapi.models.SlotGiornaliero;
import it.vszdev.gedosanapi.models.Trasfusionale;
import it.vszdev.gedosanapi.repositories.SlotGiornalieroRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;

@Service
public class SlotGiornalieroService {

    private final SlotGiornalieroRepository slotGiornalieroRepository;
    private final TransactionTemplate nuovaTransazione;

    public SlotGiornalieroService(SlotGiornalieroRepository slotGiornalieroRepository,
                                  PlatformTransactionManager transactionManager) {
        this.slotGiornalieroRepository = slotGiornalieroRepository;
        this.nuovaTransazione = new TransactionTemplate(transactionManager);
        this.nuovaTransazione.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // Transazione propria (REQUIRES_NEW) per committare subito l'insert: il vincolo UNIQUE fa da arbitro
    // tra richieste concorrenti. Chi perde la corsa non può rileggere nella stessa transazione, perché
    // dopo l'eccezione la sessione di Hibernate è inutilizzabile: la transazione va in rollback e si
    // riprova in una nuova, che trova la riga del vincitore.
    public SlotGiornaliero risolviSlot(Trasfusionale trasfusionale, OrarioValido orario, LocalDate data) {
        try {
            return nuovaTransazione.execute(stato -> trovaOCrea(trasfusionale, orario, data));
        } catch (DataIntegrityViolationException e) {
            return nuovaTransazione.execute(stato -> trovaOCrea(trasfusionale, orario, data));
        }
    }

    private SlotGiornaliero trovaOCrea(Trasfusionale trasfusionale, OrarioValido orario, LocalDate data) {
        return slotGiornalieroRepository.findByTrasfusionale_IdAndOrario_OrarioAndData(trasfusionale.getId(), orario.getOrario(), data)
                .orElseGet(() -> slotGiornalieroRepository.save(new SlotGiornaliero(trasfusionale, orario, data, 0)));
    }
}
