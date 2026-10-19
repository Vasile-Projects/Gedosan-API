package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.dto.email.ConfermaPrenotazioneEmailPayload;
import it.vszdev.gedosanapi.models.Prenotazione;
import it.vszdev.gedosanapi.repositories.PrenotazioneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Promemoria via email il giorno prima della donazione, per tutte le prenotazioni (sito, admin, bot):
// l'email è l'unico contatto verificato. Gira ogni ora dalle 17 alle 21: i passaggi successivi al primo
// ritentano solo le email non partite, grazie a promemoria_inviato_at.
@Component
public class PromemoriaJob {

    private static final Logger log = LoggerFactory.getLogger(PromemoriaJob.class);

    private final PrenotazioneRepository prenotazioneRepository;
    private final EmailService emailService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public PromemoriaJob(PrenotazioneRepository prenotazioneRepository,
                         EmailService emailService,
                         TransactionTemplate transactionTemplate,
                         Clock clock) {
        this.prenotazioneRepository = prenotazioneRepository;
        this.emailService = emailService;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 17-21 * * *", zone = "Europe/Rome")
    public void inviaPromemoriaDiDomani() {
        LocalDate domani = LocalDate.now(clock).plusDays(1);
        List<Prenotazione> daAvvisare = prenotazioneRepository
                .findBySlotGiornaliero_DataAndPromemoriaInviatoAtIsNullOrderByIdAsc(domani);
        if (daAvvisare.isEmpty()) {
            return;
        }

        int inviati = 0;
        for (Prenotazione prenotazione : daAvvisare) {
            // Un'email per volta, ognuna segnata subito: un errore a metà non fa ripartire le già inviate.
            if (emailService.inviaPromemoria(new ConfermaPrenotazioneEmailPayload(prenotazione))) {
                transactionTemplate.executeWithoutResult(stato ->
                        prenotazioneRepository.segnaPromemoriaInviato(prenotazione.getId(), LocalDateTime.now(clock)));
                inviati++;
            }
        }
        log.info("Promemoria per il {}: inviati {} su {}", domani, inviati, daAvvisare.size());
    }
}
