package it.vszdev.gedosanapi.dto.bot;

import java.time.LocalDate;

// Solo il nome di battesimo: basta al bot per salutare.
public record CollegamentoResponse(String nome, LocalDate validoFinoAl) {
}
