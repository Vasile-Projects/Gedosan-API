package it.vszdev.gedosanapi.dto.bot;

import java.time.LocalDate;

// prossimaDataUtile è null quando la prenotazione online non è più possibile (limite di età).
public record StatoDonatoreResponse(
        String nome,
        LocalDate prossimaDataUtile,
        String motivo,
        PrenotazioneBotResponse prossimoAppuntamento
) {
}
