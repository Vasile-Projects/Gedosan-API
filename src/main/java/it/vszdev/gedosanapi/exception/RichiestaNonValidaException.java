package it.vszdev.gedosanapi.exception;

// Parametri della richiesta semanticamente non validi, oltre quanto esprimibile con le annotazioni
// di validazione (es. un anno nel passato). Mappata su 400.
public class RichiestaNonValidaException extends RuntimeException {

    public RichiestaNonValidaException(String message) {
        super(message);
    }
}
