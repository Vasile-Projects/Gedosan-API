package it.vszdev.gedosanapi.dto.disponibilita;

import java.time.LocalTime;

public record SlotDisponibileResponse(
        Long idSlot,
        LocalTime orario,
        boolean disponibile,
        int postiLiberi
) {
}
