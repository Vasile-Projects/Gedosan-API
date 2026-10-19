package it.vszdev.gedosanapi.events;

import it.vszdev.gedosanapi.service.EmailService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class CollegamentoEmailListener {

    private final EmailService emailService;

    public CollegamentoEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    // AFTER_COMMIT: il codice parte solo se è davvero salvato; @Async come le altre email.
    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCodiceCollegamento(CodiceCollegamentoEvent event) {
        emailService.inviaCodiceCollegamento(event);
    }
}
