package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.dto.email.ConfermaPrenotazioneEmailPayload;
import it.vszdev.gedosanapi.enums.EsitoInvioEmail;
import it.vszdev.gedosanapi.models.LogEmail;
import it.vszdev.gedosanapi.models.Prenotazione;
import it.vszdev.gedosanapi.repositories.LogEmailRepository;
import it.vszdev.gedosanapi.repositories.PrenotazioneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Ritenta le email di conferma non recapitate: copre un'interruzione SMTP transitoria. Il donatore
// ha comunque tutti i dati nella risposta HTTP, quindi qui non serve nessuna garanzia forte.
@Component
public class RitentativoEmailJob {

    private static final int FINESTRA_ORE = 2;
    private static final int MAX_TENTATIVI = 10;

    private static final Logger log = LoggerFactory.getLogger(RitentativoEmailJob.class);

    private final LogEmailRepository logEmailRepository;
    private final PrenotazioneRepository prenotazioneRepository;
    private final EmailService emailService;
    private final Clock clock;

    public RitentativoEmailJob(LogEmailRepository logEmailRepository,
                               PrenotazioneRepository prenotazioneRepository,
                               EmailService emailService,
                               Clock clock) {
        this.logEmailRepository = logEmailRepository;
        this.prenotazioneRepository = prenotazioneRepository;
        this.emailService = emailService;
        this.clock = clock;
    }

    @Scheduled(cron = "0 */20 * * * *")
    public void ritentaEmailNonRecapitate() {
        LocalDateTime soglia = LocalDateTime.now(clock).minusHours(FINESTRA_ORE);

        Map<Long, List<LogEmail>> perPrenotazione = logEmailRepository.findByTimestampAfter(soglia.minusHours(1)).stream()
                .collect(Collectors.groupingBy(LogEmail::getIdPrenotazione));

        List<Long> idDaRitentare = perPrenotazione.entrySet().stream()
                .filter(voce -> daRitentare(voce.getValue(), soglia))
                .map(Map.Entry::getKey)
                .toList();

        if (idDaRitentare.isEmpty()) {
            return;
        }

        List<Prenotazione> prenotazioni = prenotazioneRepository.findByIdIn(idDaRitentare);
        prenotazioni.forEach(p ->
                emailService.inviaConfermaPrenotazione(new ConfermaPrenotazioneEmailPayload(p)));

        log.info("Ritentativo email di conferma: {} prenotazioni ripianificate", prenotazioni.size());
    }

    private boolean daRitentare(List<LogEmail> righe, LocalDateTime soglia) {
        boolean giaInviata = righe.stream().anyMatch(l -> l.getEsito() == EsitoInvioEmail.OK);
        List<LogEmail> falliti = righe.stream().filter(l -> l.getEsito() == EsitoInvioEmail.KO).toList();

        boolean primoFallimentoRecente = falliti.stream()
                .map(LogEmail::getTimestamp)
                .min(Comparator.naturalOrder())
                .filter(primoKo -> primoKo.isAfter(soglia))
                .isPresent();

        return !giaInviata
                && !falliti.isEmpty()
                && falliti.size() < MAX_TENTATIVI
                && primoFallimentoRecente;
    }
}
