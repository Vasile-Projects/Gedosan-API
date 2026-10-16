package it.vszdev.gedosanapi.events;

import it.vszdev.gedosanapi.service.EmailService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RichiestaPrenotazioneEmailListener {

    private final EmailService emailService;

    public RichiestaPrenotazioneEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    // AFTER_COMMIT: il link parte solo se l'attesa è davvero salvata; @Async come per la conferma finale.
    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLinkConferma(LinkConfermaEvent event) {
        emailService.inviaLinkConferma(event);
    }

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAvviso(AvvisoPrenotazioneEvent event) {
        emailService.inviaAvviso(event);
    }
}
