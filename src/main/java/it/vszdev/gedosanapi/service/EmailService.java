package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.dto.email.ConfermaPrenotazioneEmailPayload;
import it.vszdev.gedosanapi.enums.EsitoInvioEmail;
import it.vszdev.gedosanapi.models.LogEmail;
import it.vszdev.gedosanapi.repositories.LogEmailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final int LUNGHEZZA_MASSIMA_ERRORE = 500;

    private final JavaMailSender mailSender;
    private final LogEmailRepository logEmailRepository;
    private final String mittente;

    public EmailService(JavaMailSender mailSender,
                         LogEmailRepository logEmailRepository,
                         @Value("${mail.mittente}") String mittente) {
        this.mailSender = mailSender;
        this.logEmailRepository = logEmailRepository;
        this.mittente = mittente;
    }

    // La prenotazione è già committata: un fallimento SMTP va solo loggato in log_email, mai propagato.
    public void inviaConfermaPrenotazione(ConfermaPrenotazioneEmailPayload payload) {
        try {
            mailSender.send(costruisciMessaggio(payload));
            logEmailRepository.save(new LogEmail(payload.idPrenotazione(), EsitoInvioEmail.OK, null));
        } catch (MailException e) {
            log.warn("Invio email di conferma fallito per la prenotazione {}", payload.idPrenotazione(), e);
            logEmailRepository.save(new LogEmail(payload.idPrenotazione(), EsitoInvioEmail.KO, tronca(e.getMessage())));
        }
    }

    private SimpleMailMessage costruisciMessaggio(ConfermaPrenotazioneEmailPayload payload) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mittente);
        message.setTo(payload.emailDonatore());
        message.setSubject("Conferma prenotazione donazione - " + payload.nomeTrasfusionale());
        message.setText(corpoMessaggio(payload));
        return message;
    }

    private String corpoMessaggio(ConfermaPrenotazioneEmailPayload payload) {
        return """
                Gentile %s,

                la tua prenotazione per una donazione di %s è confermata.

                Centro trasfusionale: %s
                Indirizzo: %s, %d
                Telefono: %s
                Data: %s
                Orario: %s

                Ti aspettiamo, grazie per il tuo gesto di solidarietà.
                """.formatted(
                payload.nomeDonatore(),
                payload.tipoDonazione(),
                payload.nomeTrasfusionale(),
                payload.indirizzoTrasfusionale(),
                payload.civicoTrasfusionale(),
                payload.telefonoTrasfusionale(),
                payload.dataPrenotazione().format(FormatiData.DATA),
                payload.orarioPrenotazione().format(FormatiData.ORARIO)
        );
    }

    private String tronca(String messaggio) {
        if (messaggio == null) {
            return null;
        }
        return messaggio.length() > LUNGHEZZA_MASSIMA_ERRORE ? messaggio.substring(0, LUNGHEZZA_MASSIMA_ERRORE) : messaggio;
    }
}
