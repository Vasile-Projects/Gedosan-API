package it.vszdev.gedosanapi.exception;

import java.util.List;

public record Errore(
        int status,
        String errore,
        String messaggio,
        String path,
        List<ErroreValidazione> erroriValidazione
) {
    public Errore(int status, String errore, String messaggio, String path) {
        this(status, errore, messaggio, path, null);
    }
}
