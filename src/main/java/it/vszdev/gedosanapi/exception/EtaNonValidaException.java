package it.vszdev.gedosanapi.exception;

public class EtaNonValidaException extends RuntimeException {

    public EtaNonValidaException(int etaMinima) {
        super("Devi avere almeno " + etaMinima + " anni per prenotare una donazione.");
    }
}
