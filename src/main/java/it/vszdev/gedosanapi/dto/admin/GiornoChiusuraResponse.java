package it.vszdev.gedosanapi.dto.admin;

import it.vszdev.gedosanapi.enums.TipoChiusura;

import java.time.LocalDate;

public record GiornoChiusuraResponse(
        LocalDate data,
        String descrizione,
        TipoChiusura tipo
) {
}
