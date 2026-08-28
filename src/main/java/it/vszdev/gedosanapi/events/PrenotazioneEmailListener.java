package it.vszdev.gedosanapi.events;

import it.vszdev.gedosanapi.service.EmailService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PrenotazioneEmailListener {

    private final EmailService emailService;

    public PrenotazioneEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    // AFTER_COMMIT: non inviare conferme per prenotazioni poi annullate da un rollback; @Async per non pagare la latenza SMTP nella risposta HTTP.
    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPrenotazioneEffettuata(PrenotazioneEffettuataEvent event) {
        emailService.inviaConfermaPrenotazione(event.payload());
    }
}
