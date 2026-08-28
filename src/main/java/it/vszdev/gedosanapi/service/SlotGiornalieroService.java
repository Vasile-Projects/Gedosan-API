package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.models.OrarioValido;
import it.vszdev.gedosanapi.models.SlotGiornaliero;
import it.vszdev.gedosanapi.models.Trasfusionale;
import it.vszdev.gedosanapi.repositories.SlotGiornalieroRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class SlotGiornalieroService {

    private final SlotGiornalieroRepository slotGiornalieroRepository;

    public SlotGiornalieroService(SlotGiornalieroRepository slotGiornalieroRepository) {
        this.slotGiornalieroRepository = slotGiornalieroRepository;
    }

    // REQUIRES_NEW per committare subito l'insert: così il vincolo UNIQUE fa da arbitro tra richieste concorrenti e chi perde la corsa rilegge la riga altrui.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SlotGiornaliero risolviSlot(Trasfusionale trasfusionale, OrarioValido orario, LocalDate data) {
        return slotGiornalieroRepository.findByTrasfusionale_IdAndOrario_OrarioAndData(trasfusionale.getId(), orario.getOrario(), data)
                .orElseGet(() -> creaSlotSicuro(trasfusionale, orario, data));
    }

    private SlotGiornaliero creaSlotSicuro(Trasfusionale trasfusionale, OrarioValido orario, LocalDate data) {
        try {
            return slotGiornalieroRepository.save(new SlotGiornaliero(trasfusionale, orario, data, 0));
        } catch (DataIntegrityViolationException e) {
            return slotGiornalieroRepository.findByTrasfusionale_IdAndOrario_OrarioAndData(trasfusionale.getId(), orario.getOrario(), data)
                    .orElseThrow(() -> e);
        }
    }
}
