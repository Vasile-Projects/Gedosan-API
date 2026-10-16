package it.vszdev.gedosanapi.controller.admin;

import it.vszdev.gedosanapi.dto.admin.DonatoreAdminResponse;
import it.vszdev.gedosanapi.dto.admin.DonatoreDettaglioResponse;
import it.vszdev.gedosanapi.dto.admin.ModificaDonatoreRequest;
import it.vszdev.gedosanapi.security.AdminPrincipal;
import it.vszdev.gedosanapi.service.AdminDonatoreService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/donatori")
public class AdminDonatoreController {

    private final AdminDonatoreService adminDonatoreService;

    public AdminDonatoreController(AdminDonatoreService adminDonatoreService) {
        this.adminDonatoreService = adminDonatoreService;
    }

    // q: email esatta, codice fiscale esatto oppure prefisso del cognome (almeno 2 caratteri).
    @GetMapping
    public List<DonatoreAdminResponse> cerca(@RequestParam String q) {
        return adminDonatoreService.cerca(q);
    }

    @GetMapping("/{id}")
    public DonatoreDettaglioResponse dettaglio(@PathVariable Long id) {
        return adminDonatoreService.dettaglio(id);
    }

    // PATCH (metodo già ammesso da CORS) con l'anagrafica completa: vedi ModificaDonatoreRequest.
    @PatchMapping("/{id}")
    public DonatoreAdminResponse modifica(@PathVariable Long id,
                                          @Valid @RequestBody ModificaDonatoreRequest request,
                                          @AuthenticationPrincipal AdminPrincipal admin) {
        return adminDonatoreService.modifica(id, request, admin.id());
    }
}
