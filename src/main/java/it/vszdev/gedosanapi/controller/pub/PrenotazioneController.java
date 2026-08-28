package it.vszdev.gedosanapi.controller.pub;

import it.vszdev.gedosanapi.dto.PrenotazioneRequest;
import it.vszdev.gedosanapi.dto.donatore.PrenotazioneResponse;
import it.vszdev.gedosanapi.service.PrenotazioneService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prenotazioni")
public class PrenotazioneController {

    private final PrenotazioneService prenotazioneService;

    public PrenotazioneController(PrenotazioneService prenotazioneService) {
        this.prenotazioneService = prenotazioneService;
    }

    @PostMapping
    public ResponseEntity<PrenotazioneResponse> crea(@Valid @RequestBody PrenotazioneRequest request) {
        PrenotazioneResponse response = prenotazioneService.crea(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
