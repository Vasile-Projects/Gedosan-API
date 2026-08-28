package it.vszdev.gedosanapi.exception;

public class EmailAssociataAdAltroDonatoreException extends RuntimeException {

    public EmailAssociataAdAltroDonatoreException() {
        super("L'email inserita è già associata a un altro donatore.");
    }
}
