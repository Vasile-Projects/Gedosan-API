package it.vszdev.gedosanapi.events;

import it.vszdev.gedosanapi.dto.email.ConfermaPrenotazioneEmailPayload;

public record PrenotazioneEffettuataEvent(ConfermaPrenotazioneEmailPayload payload) {
}
