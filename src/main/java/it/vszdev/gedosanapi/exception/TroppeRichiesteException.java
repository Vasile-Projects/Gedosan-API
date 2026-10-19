package it.vszdev.gedosanapi.exception;

// Limite applicativo superato (es. troppe richieste di codice dalla stessa chat). Mappata su 429.
public class TroppeRichiesteException extends RuntimeException {

    public TroppeRichiesteException(String message) {
        super(message);
    }
}
