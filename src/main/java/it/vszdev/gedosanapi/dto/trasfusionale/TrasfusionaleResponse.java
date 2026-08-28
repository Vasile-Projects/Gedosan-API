package it.vszdev.gedosanapi.dto.trasfusionale;

import it.vszdev.gedosanapi.models.Trasfusionale;

public record TrasfusionaleResponse(
        Long id,
        String nome,
        String citta,
        String indirizzo,
        Integer civico,
        String telefono
) {
    public TrasfusionaleResponse(Trasfusionale t) {
        this(
                t.getId(),
                t.getNome(),
                t.getCitta(),
                t.getIndirizzo(),
                t.getCivico(),
                t.getTelefono()
        );
    }
}
