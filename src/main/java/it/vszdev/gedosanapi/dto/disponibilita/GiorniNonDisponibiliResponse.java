package it.vszdev.gedosanapi.dto.disponibilita;

import java.time.LocalDate;
import java.util.List;

public record GiorniNonDisponibiliResponse(
        List<LocalDate> giorniNonDisponibili
) {
}
