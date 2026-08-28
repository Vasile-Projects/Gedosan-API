package it.vszdev.gedosanapi.controller.admin;

import it.vszdev.gedosanapi.dto.admin.VariazioneAperturaRequest;
import it.vszdev.gedosanapi.dto.admin.VariazioneAperturaResponse;
import it.vszdev.gedosanapi.service.VariazioneAperturaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/variazioni-apertura")
public class VariazioneAperturaController {

    private final VariazioneAperturaService variazioneAperturaService;

    public VariazioneAperturaController(VariazioneAperturaService variazioneAperturaService) {
        this.variazioneAperturaService = variazioneAperturaService;
    }

    @GetMapping
    public List<VariazioneAperturaResponse> elenco(@RequestParam(required = false) Long idTrasfusionale) {
        return variazioneAperturaService.elenco(idTrasfusionale);
    }

    @PostMapping
    public ResponseEntity<List<VariazioneAperturaResponse>> crea(@Valid @RequestBody VariazioneAperturaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(variazioneAperturaService.crea(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> elimina(@PathVariable Long id) {
        variazioneAperturaService.elimina(id);
        return ResponseEntity.noContent().build();
    }
}
