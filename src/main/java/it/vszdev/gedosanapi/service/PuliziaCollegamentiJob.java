package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.repositories.CodiceCollegamentoRepository;
import it.vszdev.gedosanapi.repositories.CollegamentoTelegramRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

// Cancella codici e collegamenti Telegram scaduti. Le letture li ignorano già: qui si tolgono solo
// righe che legano una chat a un donatore e non servono più.
@Component
public class PuliziaCollegamentiJob {

    private static final Logger log = LoggerFactory.getLogger(PuliziaCollegamentiJob.class);

    private final CodiceCollegamentoRepository codiceCollegamentoRepository;
    private final CollegamentoTelegramRepository collegamentoTelegramRepository;
    private final Clock clock;

    public PuliziaCollegamentiJob(CodiceCollegamentoRepository codiceCollegamentoRepository,
                                  CollegamentoTelegramRepository collegamentoTelegramRepository,
                                  Clock clock) {
        this.codiceCollegamentoRepository = codiceCollegamentoRepository;
        this.collegamentoTelegramRepository = collegamentoTelegramRepository;
        this.clock = clock;
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "Europe/Rome")
    @Transactional
    public void eliminaScaduti() {
        LocalDateTime adesso = LocalDateTime.now(clock);
        int codici = codiceCollegamentoRepository.eliminaScaduti(adesso);
        int collegamenti = collegamentoTelegramRepository.eliminaScaduti(adesso);
        log.info("Pulizia Telegram: eliminati {} codici e {} collegamenti scaduti", codici, collegamenti);
    }
}
