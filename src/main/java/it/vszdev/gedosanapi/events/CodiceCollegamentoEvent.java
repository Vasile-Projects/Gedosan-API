package it.vszdev.gedosanapi.events;

// Il codice viaggia in chiaro solo dentro l'evento, fino all'invio dell'email: a database c'è l'hash.
// email e nome sono quelli REGISTRATI del donatore.
public record CodiceCollegamentoEvent(
        String email,
        String nome,
        String codice,
        int minutiValidita
) {
}
