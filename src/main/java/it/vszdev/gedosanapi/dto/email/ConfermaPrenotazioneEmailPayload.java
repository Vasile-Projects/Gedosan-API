package it.vszdev.gedosanapi.dto.email;

import it.vszdev.gedosanapi.enums.TipoDonazione;
import it.vszdev.gedosanapi.models.Prenotazione;

import java.time.LocalDate;
import java.time.LocalTime;

public record ConfermaPrenotazioneEmailPayload(
        Long idPrenotazione,
        String emailDonatore,
        String nomeDonatore,
        String nomeTrasfusionale,
        String indirizzoTrasfusionale,
        Integer civicoTrasfusionale,
        String telefonoTrasfusionale,
        LocalDate dataPrenotazione,
        LocalTime orarioPrenotazione,
        TipoDonazione tipoDonazione
) {
    public ConfermaPrenotazioneEmailPayload(Prenotazione p) {
        this(
                p.getId(),
                p.getDonatore().getEmail(),
                p.getDonatore().getNome(),
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
