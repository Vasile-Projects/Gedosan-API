package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.models.PrenotazioneInAttesa;
import it.vszdev.gedosanapi.repositories.PrenotazioneInAttesaRepository;
import it.vszdev.gedosanapi.repositories.SlotGiornalieroRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TreeSet;

// Cancella fisicamente le attese di conferma scadute e restituisce i posti agli slot. Fisicamente
// perché contengono dati personali non confermati e perché la FK RESTRICT sullo slot bloccherebbe
// PuliziaSlotGiornalieroJob. Tra la scadenza e il passaggio del job il conteggio degli slot resta al
// più un minuto sovrastimato; i ricalcoli dei flussi di scrittura le ignorano già.
@Component
public class ScadenzaPrenotazioniInAttesaJob {

    private static final Logger log = LoggerFactory.getLogger(ScadenzaPrenotazioniInAttesaJob.class);

    private final PrenotazioneInAttesaRepository prenotazioneInAttesaRepository;
    private final SlotGiornalieroRepository slotGiornalieroRepository;
    private final PrenotazioneService prenotazioneService;
    private final Clock clock;

    public ScadenzaPrenotazioniInAttesaJob(PrenotazioneInAttesaRepository prenotazioneInAttesaRepository,
                                           SlotGiornalieroRepository slotGiornalieroRepository,
                                           PrenotazioneService prenotazioneService,
                                           Clock clock) {
        this.prenotazioneInAttesaRepository = prenotazioneInAttesaRepository;
        this.slotGiornalieroRepository = slotGiornalieroRepository;
        this.prenotazioneService = prenotazioneService;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void eliminaAtteseScadute() {
        // Ordine dei lock come nei flussi di richiesta e conferma: prima le attese, poi gli slot.
        List<PrenotazioneInAttesa> scadute = prenotazioneInAttesaRepository
                .findByScadenzaLessThanEqualOrderByIdAsc(LocalDateTime.now(clock));
        if (scadute.isEmpty()) {
            return;
        }

        TreeSet<Long> idSlot = new TreeSet<>();
        scadute.forEach(attesa -> idSlot.add(attesa.getSlotGiornaliero().getId()));
        idSlot.forEach(id -> slotGiornalieroRepository.findWithLockById(id));

        prenotazioneInAttesaRepository.deleteAll(scadute);
        idSlot.forEach(id -> slotGiornalieroRepository.findById(id).ifPresent(prenotazioneService::ricalcolaPostiOccupati));

        log.info("Scadenza attese di conferma: eliminate {} attese su {} slot", scadute.size(), idSlot.size());
    }
}
