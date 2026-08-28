package it.vszdev.gedosanapi.dto.admin;

import it.vszdev.gedosanapi.models.VariazioneApertura;

import java.time.LocalDate;

public record VariazioneAperturaResponse(
        Long id,
        String nomeTrasfusionale,
        LocalDate dataVariazione,
        Boolean apertura,
        String motivo
) {
    public VariazioneAperturaResponse(VariazioneApertura v) {
        this(
                v.getId(),
                v.getTrasfusionale().getNome(),
                v.getDataVariazione(),
                v.getApertura(),
                v.getMotivo()
        );
    }
}
