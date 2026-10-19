package it.vszdev.gedosanapi.dto.bot;

import it.vszdev.gedosanapi.models.SlotGiornaliero;

import java.time.LocalDate;
import java.time.LocalTime;

// Riepilogo di uno slot prima della conferma: il bot riceve dal pulsante solo l'id.
public record SlotBotResponse(
        Long idSlot,
        Long idTrasfusionale,
        String centro,
        String indirizzo,
        LocalDate data,
        LocalTime orario
) {
    public SlotBotResponse(SlotGiornaliero s) {
        this(
                s.getId(),
                s.getTrasfusionale().getId(),
                s.getTrasfusionale().getNome(),
                s.getTrasfusionale().getIndirizzo() + ", " + s.getTrasfusionale().getCivico(),
                s.getData(),
                s.getOrario().getOrario()
        );
    }
}
