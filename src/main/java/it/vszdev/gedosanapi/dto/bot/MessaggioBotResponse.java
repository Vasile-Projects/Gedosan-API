package it.vszdev.gedosanapi.dto.bot;

public record MessaggioBotResponse(String messaggio) {

    // Risposta alla richiesta di codice: identica che il donatore esista o no, così il bot non rivela
    // chi è registrato. Il link al modulo serve a chi dona per la prima volta.
    public static MessaggioBotResponse richiestaCodice(String urlPrenotazione) {
        return new MessaggioBotResponse("Se i dati corrispondono a un donatore registrato, ti abbiamo inviato "
                + "un codice di 6 cifre via email. Scrivilo qui: è valido 10 minuti. "
                + "Se è la tua prima donazione, prenota dal sito: " + urlPrenotazione);
    }
}
