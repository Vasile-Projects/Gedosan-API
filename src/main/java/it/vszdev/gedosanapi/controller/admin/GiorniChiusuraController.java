package it.vszdev.gedosanapi.controller.admin;

import it.vszdev.gedosanapi.dto.admin.GiornoChiusuraResponse;
import it.vszdev.gedosanapi.service.GiorniChiusuraService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/giorni-chiusura")
public class GiorniChiusuraController {

    private final GiorniChiusuraService giorniChiusuraService;

    public GiorniChiusuraController(GiorniChiusuraService giorniChiusuraService) {
        this.giorniChiusuraService = giorniChiusuraService;
    }

    // Riepilogo dei giorni di chiusura del centro per l'anno indicato (default: anno corrente,
    // sono ammessi solo l'anno corrente e quelli successivi).
    @GetMapping
    public List<GiornoChiusuraResponse> elenco(@RequestParam Long idTrasfusionale,
                                               @RequestParam(required = false) Integer anno) {
        return giorniChiusuraService.elenco(idTrasfusionale, anno);
    }
}
