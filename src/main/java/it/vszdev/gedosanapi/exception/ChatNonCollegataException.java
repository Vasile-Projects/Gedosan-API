package it.vszdev.gedosanapi.exception;

// La chat Telegram non è collegata a nessun donatore (o il collegamento è scaduto). Mappata su 403:
// n8n la usa per proporre all'utente il collegamento.
public class ChatNonCollegataException extends RuntimeException {

    public ChatNonCollegataException() {
        super("Questa chat non è collegata a un donatore. Scrivi /collega per collegarla.");
    }
}
