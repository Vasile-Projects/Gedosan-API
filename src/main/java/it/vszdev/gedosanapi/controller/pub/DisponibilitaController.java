package it.vszdev.gedosanapi.controller.pub;

import it.vszdev.gedosanapi.dto.disponibilita.GiorniNonDisponibiliResponse;
import it.vszdev.gedosanapi.dto.disponibilita.SlotDisponibileResponse;
import it.vszdev.gedosanapi.service.DisponibilitaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/trasfusionali/{idTrasfusionale}")
public class DisponibilitaController {

    private final DisponibilitaService disponibilitaService;

    public DisponibilitaController(DisponibilitaService disponibilitaService) {
        this.disponibilitaService = disponibilitaService;
    }

    @GetMapping("/giorni-non-disponibili")
    public GiorniNonDisponibiliResponse giorniNonDisponibili(@PathVariable Long idTrasfusionale,
                                                              @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mese) {
        return disponibilitaService.giorniNonDisponibili(idTrasfusionale, mese);
    }

    @GetMapping("/slot")
    public List<SlotDisponibileResponse> slotDisponibili(@PathVariable Long idTrasfusionale,
                                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return disponibilitaService.slotDisponibili(idTrasfusionale, data);
    }
}
