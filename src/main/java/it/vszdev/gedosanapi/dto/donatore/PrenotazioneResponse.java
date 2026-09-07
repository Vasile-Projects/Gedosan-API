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
        String indirizzoTrasfusionale,
        Integer civicoTrasfusionale,
        String telefonoTrasfusionale,
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
                p.getSlotGiornaliero().getTrasfusionale().getIndirizzo(),
                p.getSlotGiornaliero().getTrasfusionale().getCivico(),
                p.getSlotGiornaliero().getTrasfusionale().getTelefono(),
                p.getSlotGiornaliero().getData(),
                p.getSlotGiornaliero().getOrario().getOrario(),
                p.getTipoDonazione()
        );
    }
}
