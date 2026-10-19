package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.dto.bot.PrenotazioneBotResponse;
import it.vszdev.gedosanapi.dto.bot.SlotBotResponse;
import it.vszdev.gedosanapi.dto.bot.StatoDonatoreResponse;
import it.vszdev.gedosanapi.exception.RisorsaNonTrovataException;
import it.vszdev.gedosanapi.models.Donatore;
import it.vszdev.gedosanapi.repositories.PrenotazioneRepository;
import it.vszdev.gedosanapi.repositories.SlotGiornalieroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Funzioni personali del bot: ognuna parte dal donatore collegato alla chat, mai da un id scelto dal
// chiamante. Così anche un workflow n8n sbagliato (o un LLM che inventa parametri) vede solo i dati di
// chi sta scrivendo.
@Service
public class BotService {

    private static final int MAX_PRENOTAZIONI_ELENCO = 10;

    private final CollegamentoTelegramService collegamentoTelegramService;
    private final IdoneitaService idoneitaService;
    private final PrenotazioneService prenotazioneService;
    private final PrenotazioneRepository prenotazioneRepository;
    private final SlotGiornalieroRepository slotGiornalieroRepository;

    public BotService(CollegamentoTelegramService collegamentoTelegramService,
                      IdoneitaService idoneitaService,
                      PrenotazioneService prenotazioneService,
                      PrenotazioneRepository prenotazioneRepository,
                      SlotGiornalieroRepository slotGiornalieroRepository) {
        this.collegamentoTelegramService = collegamentoTelegramService;
        this.idoneitaService = idoneitaService;
        this.prenotazioneService = prenotazioneService;
        this.prenotazioneRepository = prenotazioneRepository;
        this.slotGiornalieroRepository = slotGiornalieroRepository;
    }

    public StatoDonatoreResponse stato(long chatId) {
        return idoneitaService.stato(collegamentoTelegramService.donatoreCollegato(chatId));
    }

    @Transactional(readOnly = true)
    public List<PrenotazioneBotResponse> prenotazioni(long chatId) {
        Donatore donatore = collegamentoTelegramService.donatoreCollegato(chatId);
        return prenotazioneRepository.findAllByDonatore_IdOrderBySlotGiornaliero_DataDesc(donatore.getId()).stream()
                .limit(MAX_PRENOTAZIONI_ELENCO)
                .map(PrenotazioneBotResponse::new)
                .toList();
    }

    // La risposta si costruisce dentro la transazione: centro e orario dello slot sono LAZY.
    @Transactional
    public PrenotazioneBotResponse prenota(long chatId, Long idSlot) {
        Donatore donatore = collegamentoTelegramService.donatoreCollegato(chatId);
        return new PrenotazioneBotResponse(prenotazioneService.creaPrenotazioneDaBot(donatore.getId(), idSlot));
    }

    // Riepilogo per il messaggio di conferma: dal pulsante Telegram arriva solo l'id dello slot.
    @Transactional(readOnly = true)
    public SlotBotResponse slot(Long idSlot) {
        return slotGiornalieroRepository.findById(idSlot)
                .map(SlotBotResponse::new)
                .orElseThrow(() -> new RisorsaNonTrovataException("Slot non trovato"));
    }
}
