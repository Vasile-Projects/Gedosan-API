package it.vszdev.gedosanapi.exception;

public class CredenzialiNonValideException extends RuntimeException {

    public CredenzialiNonValideException() {
        super("Username o password non validi.");
    }
}
