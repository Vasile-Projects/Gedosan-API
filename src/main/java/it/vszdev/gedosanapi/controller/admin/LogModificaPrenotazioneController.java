package it.vszdev.gedosanapi.controller.admin;

import it.vszdev.gedosanapi.dto.admin.LogModificaPrenotazioneResponse;
import it.vszdev.gedosanapi.service.LogModificaPrenotazioneService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/log-modifiche")
public class LogModificaPrenotazioneController {

    private final LogModificaPrenotazioneService logModificaPrenotazioneService;

    public LogModificaPrenotazioneController(LogModificaPrenotazioneService logModificaPrenotazioneService) {
        this.logModificaPrenotazioneService = logModificaPrenotazioneService;
    }

    @GetMapping
    public List<LogModificaPrenotazioneResponse> elenco() {
        return logModificaPrenotazioneService.elenco();
    }
}
