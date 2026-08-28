package it.vszdev.gedosanapi.dto.donatore;

import it.vszdev.gedosanapi.enums.TipoDonazione;
import it.vszdev.gedosanapi.models.Prenotazione;

import java.time.LocalDate;
import java.time.LocalTime;

public record PrenotazioneResponse(
        Long id,
        String nomeDonatore,
        String cognomeDonatore,
        String nomeTrasfusionale,
        LocalDate dataPrenotazione,
        LocalTime orarioPrenotazione,
        TipoDonazione tipoDonazione
) {
    public PrenotazioneResponse(Prenotazione p) {
        this(
                p.getId(),
                p.getDonatore().getNome(),
                p.getDonatore().getCognome(),
                p.getSlotGiornaliero().getTrasfusionale().getNome(),
                p.getSlotGiornaliero().getData(),
                p.getSlotGiornaliero().getOrario().getOrario(),
                p.getTipoDonazione()
        );
    }
}
