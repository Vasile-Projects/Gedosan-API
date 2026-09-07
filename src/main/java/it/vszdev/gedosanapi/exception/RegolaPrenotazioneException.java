package it.vszdev.gedosanapi.exception;

// Una regola di business sulle prenotazioni non è rispettata (slot pieno, giorno chiuso, data fuori
// orizzonte, anticipo minimo, età, intervallo tra donazioni, limite donatrici, cambio centro, email
// già usata da un altro donatore...). Tutti questi casi tornano 409 "Prenotazione non consentita":
// il dettaglio sta nel messaggio passato dal punto in cui la regola viene verificata.
public class RegolaPrenotazioneException extends RuntimeException {

    public RegolaPrenotazioneException(String message) {
        super(message);
    }
}
