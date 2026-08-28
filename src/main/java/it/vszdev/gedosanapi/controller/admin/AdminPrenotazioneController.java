package it.vszdev.gedosanapi.controller.admin;

import it.vszdev.gedosanapi.dto.PrenotazioneRequest;
import it.vszdev.gedosanapi.dto.admin.PrenotazioneAdminResponse;
import it.vszdev.gedosanapi.dto.admin.RiprogrammazionePrenotazioneRequest;
import it.vszdev.gedosanapi.security.AdminPrincipal;
import it.vszdev.gedosanapi.service.AdminPrenotazioneService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminPrenotazioneController {

    private final AdminPrenotazioneService adminPrenotazioneService;

    public AdminPrenotazioneController(AdminPrenotazioneService adminPrenotazioneService) {
        this.adminPrenotazioneService = adminPrenotazioneService;
    }

    @GetMapping("/prenotazioni")
    public List<PrenotazioneAdminResponse> elenco(@RequestParam Long idTrasfusionale,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return adminPrenotazioneService.elenco(idTrasfusionale, data);
    }

    @GetMapping("/prenotazioni/{id}")
    public PrenotazioneAdminResponse dettaglio(@PathVariable Long id) {
        return adminPrenotazioneService.dettaglio(id);
    }

    @GetMapping("/prenotazioni/esportazione")
    public ResponseEntity<byte[]> esportaPdf(@RequestParam Long idTrasfusionale,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        byte[] pdf = adminPrenotazioneService.esportaPdf(idTrasfusionale, data);
        String nomeFile = "prenotazioni_" + idTrasfusionale + "_" + data + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nomeFile + "\"")
                .body(pdf);
    }

    @PostMapping("/prenotazioni")
    public ResponseEntity<PrenotazioneAdminResponse> crea(@Valid @RequestBody PrenotazioneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminPrenotazioneService.crea(request));
    }

    @PatchMapping("/prenotazioni/{id}")
    public PrenotazioneAdminResponse riprogramma(@PathVariable Long id,
                                                  @Valid @RequestBody RiprogrammazionePrenotazioneRequest request,
                                                  @AuthenticationPrincipal AdminPrincipal admin) {
        return adminPrenotazioneService.riprogramma(id, request.idSlotNuovo(), admin.id());
    }

    @DeleteMapping("/prenotazioni/{id}")
    public ResponseEntity<Void> elimina(@PathVariable Long id, @AuthenticationPrincipal AdminPrincipal admin) {
        adminPrenotazioneService.elimina(id, admin.id());
        return ResponseEntity.noContent().build();
    }
}
