package it.vszdev.gedosanapi.dto.admin;

import it.vszdev.gedosanapi.enums.TipoDonazione;
import it.vszdev.gedosanapi.models.Prenotazione;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record PrenotazioneAdminResponse(
        Long id,
        String nomeDonatore,
        String cognomeDonatore,
        String emailDonatore,
        String cellulareDonatore,
        String nomeTrasfusionale,
        LocalDate dataPrenotazione,
        LocalTime orarioPrenotazione,
        TipoDonazione tipoDonazione,
        LocalDateTime createdAt
) {
    public PrenotazioneAdminResponse(Prenotazione p) {
        this(
                p.getId(),
                p.getDonatore().getNome(),
                p.getDonatore().getCognome(),
                p.getDonatore().getEmail(),
                p.getDonatore().getCellulare(),
                p.getSlotGiornaliero().getTrasfusionale().getNome(),
                p.getSlotGiornaliero().getData(),
                p.getSlotGiornaliero().getOrario().getOrario(),
                p.getTipoDonazione(),
                p.getCreatedAt()
        );
    }
}
