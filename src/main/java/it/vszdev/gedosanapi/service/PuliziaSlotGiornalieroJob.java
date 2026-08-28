package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.repositories.SlotGiornalieroRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class PuliziaSlotGiornalieroJob {

    private static final int GIORNI_RITENZIONE = 30;

    private static final Logger log = LoggerFactory.getLogger(PuliziaSlotGiornalieroJob.class);

    private final SlotGiornalieroRepository slotGiornalieroRepository;

    public PuliziaSlotGiornalieroJob(SlotGiornalieroRepository slotGiornalieroRepository) {
        this.slotGiornalieroRepository = slotGiornalieroRepository;
    }

    // Elimina gli slot "fantasma": generati dalla sola consultazione della disponibilità e mai prenotati.
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void pulisciSlotVuotiPassati() {
        LocalDate sogliaData = LocalDate.now().minusDays(GIORNI_RITENZIONE);
        long eliminati = slotGiornalieroRepository.deleteByDataBeforeAndPostiOccupati(sogliaData, 0);
        log.info("Pulizia slot giornalieri: eliminati {} slot vuoti con data antecedente al {}", eliminati, sogliaData);
    }
}