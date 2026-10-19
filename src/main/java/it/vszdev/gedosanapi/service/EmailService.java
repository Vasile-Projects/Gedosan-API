package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.dto.email.ConfermaPrenotazioneEmailPayload;
import it.vszdev.gedosanapi.enums.CanalePrenotazione;
import it.vszdev.gedosanapi.events.AvvisoPrenotazioneEvent;
import it.vszdev.gedosanapi.events.CodiceCollegamentoEvent;
import it.vszdev.gedosanapi.events.LinkConfermaEvent;
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
    private final String frontendUrl;
    private final String telegramUrl;

    public EmailService(JavaMailSender mailSender,
                         LogEmailRepository logEmailRepository,
                         @Value("${mail.mittente}") String mittente,
                         @Value("${app.frontend-url}") String frontendUrl,
                         @Value("${bot.telegram-url:}") String telegramUrl) {
        this.mailSender = mailSender;
        this.logEmailRepository = logEmailRepository;
        this.mittente = mittente;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        this.telegramUrl = telegramUrl;
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

    // Link e avvisi non finiscono in log_email (legato a una prenotazione confermata) e non vengono
    // ritentati: il link scade comunque in pochi minuti. Nei log nessun dato personale.
    public void inviaLinkConferma(LinkConfermaEvent event) {
        String link = frontendUrl + "/conferma?token=" + event.token();
        String corpo = """
                Gentile %s,

                abbiamo ricevuto la tua richiesta di prenotazione per una donazione di sangue intero.

                Centro trasfusionale: %s
                Data: %s
                Orario: %s

                Per completarla apri questo link e premi "Conferma prenotazione" entro le ore %s:
                %s

                Il posto resta riservato fino a quell'ora. Senza conferma la richiesta viene annullata.
                Se non hai richiesto tu questa prenotazione, ignora questa email: non verrà registrato nulla.
                """.formatted(
                event.nome(),
                event.nomeTrasfusionale(),
                event.data().format(FormatiData.DATA),
                event.orario().format(FormatiData.ORARIO),
                event.scadenza().toLocalTime().format(FormatiData.ORARIO),
                link);
        invia(event.email(), "Conferma la tua prenotazione - " + event.nomeTrasfusionale(), corpo, "link di conferma");
    }

    public void inviaAvviso(AvvisoPrenotazioneEvent event) {
        String corpo = switch (event.tipo()) {
            case DATI_DIVERSI -> """
                    Gentile %s,

                    abbiamo ricevuto una richiesta di prenotazione con il tuo codice fiscale ma con un indirizzo
                    email diverso da quello registrato. Per sicurezza la richiesta non è stata accettata.

                    Se eri tu e hai cambiato indirizzo email, contatta il tuo centro trasfusionale per aggiornare
                    i dati. Se non eri tu, puoi ignorare questa email: i tuoi dati non sono stati modificati.
                    """.formatted(event.nome());
            case EMAIL_IN_USO -> """
                    Gentile %s,

                    abbiamo ricevuto una richiesta di prenotazione con questo indirizzo email, che però risulta
                    associato a un altro codice fiscale. Per sicurezza la richiesta non è stata accettata.

                    Se eri tu, contatta il tuo centro trasfusionale per verificare i dati. Se non eri tu, puoi
                    ignorare questa email: i tuoi dati non sono stati modificati.
                    """.formatted(event.nome());
            case REGOLA_NON_RISPETTATA -> """
                    Gentile %s,

                    non è stato possibile accettare la tua richiesta di prenotazione:
                    %s

                    Se non hai richiesto tu questa prenotazione, ignora questa email.
                    """.formatted(event.nome(), event.dettaglio());
        };
        invia(event.email(), "Richiesta di prenotazione non accettata", corpo, "avviso " + event.tipo());
    }

    public void inviaCodiceCollegamento(CodiceCollegamentoEvent event) {
        String corpo = """
                Gentile %s,

                il tuo codice per collegare Telegram all'assistente Gedosan è:

                %s

                Scrivilo nella chat entro %d minuti. Dopo il collegamento potrai vedere le tue prenotazioni
                e prenotare direttamente da Telegram.

                Se non l'hai richiesto tu, ignora questa email: senza il codice nessuna chat viene collegata.
                Non comunicare il codice a nessuno.
                """.formatted(event.nome(), event.codice(), event.minutiValidita());
        invia(event.email(), "Il tuo codice per Telegram: " + event.codice(), corpo, "codice di collegamento");
    }

    // Promemoria del giorno prima (PromemoriaJob). Restituisce l'esito per non segnarlo come inviato
    // se l'SMTP fallisce: il job lo ritenta al passaggio successivo.
    public boolean inviaPromemoria(ConfermaPrenotazioneEmailPayload payload) {
        String corpo = """
                Gentile %s,

                ti ricordiamo l'appuntamento di domani per la donazione di sangue intero.

                Centro trasfusionale: %s
                Indirizzo: %s, %d
                Data: %s
                Orario: %s

                Porta un documento d'identità e la tessera sanitaria. Se non puoi venire, avvisa il centro
                al numero %s: il posto potrà essere offerto a un altro donatore.
                """.formatted(
                payload.nomeDonatore(),
                payload.nomeTrasfusionale(),
                payload.indirizzoTrasfusionale(),
                payload.civicoTrasfusionale(),
                payload.dataPrenotazione().format(FormatiData.DATA),
                payload.orarioPrenotazione().format(FormatiData.ORARIO),
                payload.telefonoTrasfusionale());
        return invia(payload.emailDonatore(), "Promemoria: domani la tua donazione - " + payload.nomeTrasfusionale(),
                corpo, "promemoria");
    }

    private boolean invia(String destinatario, String oggetto, String corpo, String descrizione) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mittente);
        message.setTo(destinatario);
        message.setSubject(oggetto);
        message.setText(corpo);
        try {
            mailSender.send(message);
            return true;
        } catch (MailException e) {
            // Solo il tipo di eccezione: il messaggio SMTP può contenere l'indirizzo del destinatario.
            log.warn("Invio email non riuscito ({}): {}", descrizione, e.getClass().getSimpleName());
            return false;
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
        return corpoConferma(payload) + notaCanale(payload.canale());
    }

    // Dal bot la prenotazione non passa dal link via email: questa email è anche l'avviso per il donatore
    // nel caso qualcun altro usasse la sua chat. Dal sito invece si suggerisce il bot per la volta dopo.
    private String notaCanale(CanalePrenotazione canale) {
        if (canale == CanalePrenotazione.TELEGRAM) {
            return """

                    Hai prenotato tramite l'assistente Gedosan su Telegram. Se non sei stato tu, contatta
                    il centro trasfusionale e scrivi /esci nella chat per scollegarla.
                    """;
        }
        if (canale == CanalePrenotazione.SITO && telegramUrl != null && !telegramUrl.isBlank()) {
            return """

                    La prossima volta puoi prenotare in pochi secondi dall'assistente Gedosan su Telegram:
                    %s
                    """.formatted(telegramUrl);
        }
        return "";
    }

    private String corpoConferma(ConfermaPrenotazioneEmailPayload payload) {
        return """
                Gentile %s,

                la tua prenotazione per una donazione di sangue intero è confermata.

                Centro trasfusionale: %s
                Indirizzo: %s, %d
                Telefono: %s
                Data: %s
                Orario: %s

                Ti aspettiamo, grazie per il tuo gesto di solidarietà.
                """.formatted(
                payload.nomeDonatore(),
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
