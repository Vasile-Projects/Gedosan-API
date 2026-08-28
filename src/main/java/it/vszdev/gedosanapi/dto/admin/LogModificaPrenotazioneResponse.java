package it.vszdev.gedosanapi.dto.admin;

import it.vszdev.gedosanapi.enums.AzioneAdmin;
import it.vszdev.gedosanapi.models.LogModificaPrenotazione;

import java.time.LocalDateTime;

public record LogModificaPrenotazioneResponse(
        Long id,
        AzioneAdmin azione,
        Long idPrenotazione,
        Long idSlotVecchio,
        Long idSlotNuovo,
        String nomeDonatore,
        String cognomeDonatore,
        String usernameAdmin,
        LocalDateTime timestamp
) {
    public LogModificaPrenotazioneResponse(LogModificaPrenotazione l) {
        this(
                l.getId(),
                l.getAzione(),
                l.getIdPrenotazione(),
                l.getIdSlotVecchio(),
                l.getIdSlotNuovo(),
                l.getDonatoreNome(),
                l.getDonatoreCognome(),
                l.getAdmin().getUsername(),
                l.getTimestamp()
        );
    }
}
