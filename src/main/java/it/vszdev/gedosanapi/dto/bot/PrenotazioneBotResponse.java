package it.vszdev.gedosanapi.dto.bot;

import it.vszdev.gedosanapi.models.Prenotazione;

import java.time.LocalDate;
import java.time.LocalTime;

public record PrenotazioneBotResponse(
        Long id,
        String centro,
        String indirizzo,
        String telefono,
        LocalDate data,
        LocalTime orario
) {
    public PrenotazioneBotResponse(Prenotazione p) {
        this(
                p.getId(),
                p.getSlotGiornaliero().getTrasfusionale().getNome(),
                p.getSlotGiornaliero().getTrasfusionale().getIndirizzo() + ", "
                        + p.getSlotGiornaliero().getTrasfusionale().getCivico(),
                p.getSlotGiornaliero().getTrasfusionale().getTelefono(),
                p.getSlotGiornaliero().getData(),
                p.getSlotGiornaliero().getOrario().getOrario()
        );
    }
}
