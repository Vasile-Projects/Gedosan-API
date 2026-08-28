package it.vszdev.gedosanapi.exception;

public class GiornoChiusoException extends RuntimeException {

    public GiornoChiusoException() {
        super("Il trasfusionale selezionato è chiuso nella data richiesta. Scegli un'altra data.");
    }
}
