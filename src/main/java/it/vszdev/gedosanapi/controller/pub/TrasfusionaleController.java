package it.vszdev.gedosanapi.controller.pub;

import it.vszdev.gedosanapi.dto.trasfusionale.TrasfusionaleResponse;
import it.vszdev.gedosanapi.service.TrasfusionaleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trasfusionali")
public class TrasfusionaleController {

    private final TrasfusionaleService trasfusionaleService;

    public TrasfusionaleController(TrasfusionaleService trasfusionaleService) {
        this.trasfusionaleService = trasfusionaleService;
    }

    @GetMapping
    public List<TrasfusionaleResponse> elenco() {
        return trasfusionaleService.elenco();
    }
}
