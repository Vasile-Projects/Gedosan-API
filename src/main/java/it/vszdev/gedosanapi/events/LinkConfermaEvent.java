package it.vszdev.gedosanapi.events;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// Il token viaggia in chiaro solo dentro l'evento, fino all'invio dell'email: a database c'è l'hash.
public record LinkConfermaEvent(
        String email,
        String nome,
        String token,
        String nomeTrasfusionale,
        LocalDate data,
        LocalTime orario,
        LocalDateTime scadenza
) {
}
