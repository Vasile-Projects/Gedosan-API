package it.vszdev.gedosanapi.exception;

public class IntervalloNonRispettatoException extends RuntimeException {

    public IntervalloNonRispettatoException() {
        super("Intervallo minimo tra due donazioni non rispettato. Contatta il trasfusionale trasfusionale per maggiori dettagli.");
    }
}
