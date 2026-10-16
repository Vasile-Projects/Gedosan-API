package it.vszdev.gedosanapi.controller.pub;

import it.vszdev.gedosanapi.dto.PrenotazioneRequest;
import it.vszdev.gedosanapi.dto.donatore.ConfermaPrenotazioneRequest;
import it.vszdev.gedosanapi.dto.donatore.PrenotazioneResponse;
import it.vszdev.gedosanapi.dto.donatore.RichiestaPrenotazioneResponse;
import it.vszdev.gedosanapi.service.RichiestaPrenotazioneService;
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

    private final RichiestaPrenotazioneService richiestaPrenotazioneService;

    public PrenotazioneController(RichiestaPrenotazioneService richiestaPrenotazioneService) {
        this.richiestaPrenotazioneService = richiestaPrenotazioneService;
    }

    // 202: la richiesta è presa in carico ma la prenotazione esiste solo dopo la conferma via email.
    // Il corpo è sempre lo stesso, qualunque sia l'esito legato alla persona.
    @PostMapping
    public ResponseEntity<RichiestaPrenotazioneResponse> richiedi(@Valid @RequestBody PrenotazioneRequest request) {
        richiestaPrenotazioneService.richiedi(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(RichiestaPrenotazioneResponse.GENERICA);
    }

    // POST e mai GET: client di posta e antivirus aprono da soli i link delle email; una conferma su GET
    // verrebbe eseguita da loro. Il link porta a una pagina del frontend che invia questa richiesta.
    @PostMapping("/conferma")
    public ResponseEntity<PrenotazioneResponse> conferma(@Valid @RequestBody ConfermaPrenotazioneRequest request) {
        PrenotazioneResponse response = richiestaPrenotazioneService.conferma(request.token());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
