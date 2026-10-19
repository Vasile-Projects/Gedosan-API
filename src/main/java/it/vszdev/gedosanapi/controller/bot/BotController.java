package it.vszdev.gedosanapi.controller.bot;

import it.vszdev.gedosanapi.dto.bot.CollegamentoResponse;
import it.vszdev.gedosanapi.dto.bot.MessaggioBotResponse;
import it.vszdev.gedosanapi.dto.bot.NuovaPrenotazioneBotRequest;
import it.vszdev.gedosanapi.dto.bot.PrenotazioneBotResponse;
import it.vszdev.gedosanapi.dto.bot.RichiestaCodiceRequest;
import it.vszdev.gedosanapi.dto.bot.SlotBotResponse;
import it.vszdev.gedosanapi.dto.bot.StatoDonatoreResponse;
import it.vszdev.gedosanapi.dto.bot.VerificaCodiceRequest;
import it.vszdev.gedosanapi.service.BotService;
import it.vszdev.gedosanapi.service.CollegamentoTelegramService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Endpoint chiamati solo dal workflow n8n del bot Telegram (header X-Bot-Key, vedi BotApiKeyFilter).
// Il chatId lo prende n8n dal messaggio ricevuto da Telegram, mai dal testo dell'utente o dall'LLM.
@RestController
@RequestMapping("/api/bot")
public class BotController {

    private final CollegamentoTelegramService collegamentoTelegramService;
    private final BotService botService;
    private final String urlPrenotazione;

    public BotController(CollegamentoTelegramService collegamentoTelegramService,
                         BotService botService,
                         @Value("${app.frontend-url}") String frontendUrl) {
        this.collegamentoTelegramService = collegamentoTelegramService;
        this.botService = botService;
        this.urlPrenotazione = (frontendUrl.endsWith("/") ? frontendUrl : frontendUrl + "/") + "prenota";
    }

    // 202 con lo stesso messaggio che il donatore esista o no.
    @PostMapping("/collegamento/richiesta")
    public ResponseEntity<MessaggioBotResponse> richiediCodice(@Valid @RequestBody RichiestaCodiceRequest request) {
        collegamentoTelegramService.richiediCodice(request.chatId(), request.codiceFiscale(), request.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(MessaggioBotResponse.richiestaCodice(urlPrenotazione));
    }

    @PostMapping("/collegamento/verifica")
    public CollegamentoResponse verificaCodice(@Valid @RequestBody VerificaCodiceRequest request) {
        return collegamentoTelegramService.verificaCodice(request.chatId(), request.codice());
    }

    @DeleteMapping("/collegamento/{chatId}")
    public ResponseEntity<Void> scollega(@PathVariable long chatId) {
        collegamentoTelegramService.scollega(chatId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/chat/{chatId}/stato")
    public StatoDonatoreResponse stato(@PathVariable long chatId) {
        return botService.stato(chatId);
    }

    @GetMapping("/chat/{chatId}/prenotazioni")
    public List<PrenotazioneBotResponse> prenotazioni(@PathVariable long chatId) {
        return botService.prenotazioni(chatId);
    }

    @PostMapping("/chat/{chatId}/prenotazioni")
    public ResponseEntity<PrenotazioneBotResponse> prenota(@PathVariable long chatId,
                                                           @Valid @RequestBody NuovaPrenotazioneBotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(botService.prenota(chatId, request.idSlot()));
    }

    @GetMapping("/slot/{idSlot}")
    public SlotBotResponse slot(@PathVariable Long idSlot) {
        return botService.slot(idSlot);
    }
}
