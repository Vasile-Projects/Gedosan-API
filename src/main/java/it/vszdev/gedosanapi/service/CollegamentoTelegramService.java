package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.config.BotProperties;
import it.vszdev.gedosanapi.dto.bot.CollegamentoResponse;
import it.vszdev.gedosanapi.events.CodiceCollegamentoEvent;
import it.vszdev.gedosanapi.exception.ChatNonCollegataException;
import it.vszdev.gedosanapi.exception.RichiestaNonValidaException;
import it.vszdev.gedosanapi.exception.TroppeRichiesteException;
import it.vszdev.gedosanapi.models.CodiceCollegamento;
import it.vszdev.gedosanapi.models.CollegamentoTelegram;
import it.vszdev.gedosanapi.models.Donatore;
import it.vszdev.gedosanapi.repositories.CodiceCollegamentoRepository;
import it.vszdev.gedosanapi.repositories.CollegamentoTelegramRepository;
import it.vszdev.gedosanapi.repositories.DonatoreRepository;
import it.vszdev.gedosanapi.security.RateLimiterService;
import it.vszdev.gedosanapi.security.TokenConferma;
import it.vszdev.gedosanapi.validation.CodiceFiscaleUtil;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

// Collegamento tra una chat Telegram e un donatore già registrato. L'identità si dimostra come nel flusso
// pubblico, con il possesso dell'email registrata, ma una volta sola: il codice ricevuto per email
// collega la chat per giorniValiditaCollegamento giorni. Da quel momento il chatId (fornito da Telegram
// tramite n8n, mai scelto dall'utente) identifica il donatore.
@Service
public class CollegamentoTelegramService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern FORMATO_EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private static final Pattern FORMATO_CODICE = Pattern.compile("\\d{6}");
    private static final String CODICE_NON_VALIDO =
            "Codice non valido o scaduto. Riprova oppure scrivi /collega per riceverne uno nuovo.";

    private final DonatoreRepository donatoreRepository;
    private final CodiceCollegamentoRepository codiceCollegamentoRepository;
    private final CollegamentoTelegramRepository collegamentoTelegramRepository;
    private final RateLimiterService rateLimiterService;
    private final BotProperties botProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public CollegamentoTelegramService(DonatoreRepository donatoreRepository,
                                       CodiceCollegamentoRepository codiceCollegamentoRepository,
                                       CollegamentoTelegramRepository collegamentoTelegramRepository,
                                       RateLimiterService rateLimiterService,
                                       BotProperties botProperties,
                                       ApplicationEventPublisher eventPublisher,
                                       Clock clock) {
        this.donatoreRepository = donatoreRepository;
        this.codiceCollegamentoRepository = codiceCollegamentoRepository;
        this.collegamentoTelegramRepository = collegamentoTelegramRepository;
        this.rateLimiterService = rateLimiterService;
        this.botProperties = botProperties;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    // Errori espliciti solo per il formato e per il limite della chat, che non dicono nulla sulla persona.
    // Per il resto il chiamante risponde sempre allo stesso modo: il codice parte solo se CF ed email
    // corrispondono allo stesso donatore e quel donatore non ha già ricevuto troppi codici nell'ultima ora.
    @Transactional
    public void richiediCodice(long chatId, String codiceFiscaleScritto, String emailScritta) {
        String codiceFiscale = Donatore.normalizzaCodiceFiscale(codiceFiscaleScritto);
        String email = Donatore.normalizzaEmail(emailScritta);
        if (!CodiceFiscaleUtil.formatoValido(codiceFiscale) || !FORMATO_EMAIL.matcher(email).matches()) {
            throw new RichiestaNonValidaException("Non riesco a leggere i dati. Scrivi codice fiscale ed email "
                    + "separati da una virgola, ad esempio: RSSMRA85M01H501Z, mario.rossi@email.it");
        }
        if (!rateLimiterService.tryConsumeCodiceCollegamento(chatId)) {
            throw new TroppeRichiesteException("Hai chiesto troppi codici. Riprova tra un'ora.");
        }

        // Una nuova richiesta annulla sempre il codice precedente della chat, qualunque sia l'esito.
        codiceCollegamentoRepository.eliminaPerChat(chatId);

        Optional<Donatore> donatore = donatoreRepository.findByCodiceFiscale(codiceFiscale)
                .filter(d -> d.getEmail().equals(email))
                .filter(d -> rateLimiterService.tryConsumeCodicePerDonatore(d.getId()));
        if (donatore.isEmpty()) {
            return;
        }

        String codice = "%06d".formatted(RANDOM.nextInt(1_000_000));
        int minuti = botProperties.getMinutiValiditaCodice();
        codiceCollegamentoRepository.save(new CodiceCollegamento(chatId, donatore.get(), hash(chatId, codice),
                LocalDateTime.now(clock).plusMinutes(minuti)));
        eventPublisher.publishEvent(new CodiceCollegamentoEvent(
                donatore.get().getEmail(), donatore.get().getNome(), codice, minuti));
    }

    // noRollbackFor: il tentativo errato va contato anche se la richiesta termina con un errore.
    @Transactional(noRollbackFor = RichiestaNonValidaException.class)
    public CollegamentoResponse verificaCodice(long chatId, String codiceScritto) {
        String codice = codiceScritto.strip();
        LocalDateTime adesso = LocalDateTime.now(clock);

        CodiceCollegamento atteso = codiceCollegamentoRepository.findWithLockByChatId(chatId)
                .filter(c -> !c.scadutoAl(adesso))
                .filter(c -> c.getTentativi() < botProperties.getMaxTentativiCodice())
                .orElseThrow(() -> new RichiestaNonValidaException(CODICE_NON_VALIDO));

        if (!FORMATO_CODICE.matcher(codice).matches() || !atteso.getCodiceHash().equals(hash(chatId, codice))) {
            atteso.registraTentativoErrato();
            throw new RichiestaNonValidaException(CODICE_NON_VALIDO);
        }

        Donatore donatore = atteso.getDonatore();
        codiceCollegamentoRepository.delete(atteso);
        collegamentoTelegramRepository.eliminaPerChat(chatId);
        CollegamentoTelegram collegamento = collegamentoTelegramRepository.save(new CollegamentoTelegram(
                chatId, donatore, adesso.plusDays(botProperties.getGiorniValiditaCollegamento())));

        return new CollegamentoResponse(donatore.getNome(), collegamento.getScadenza().toLocalDate());
    }

    @Transactional
    public void scollega(long chatId) {
        codiceCollegamentoRepository.eliminaPerChat(chatId);
        collegamentoTelegramRepository.eliminaPerChat(chatId);
    }

    // Donatore collegato alla chat, se il collegamento è ancora valido. Applica anche il limite per chat
    // a tutte le funzioni personali del bot.
    @Transactional(readOnly = true)
    public Donatore donatoreCollegato(long chatId) {
        if (!rateLimiterService.tryConsumeChat(chatId)) {
            throw new TroppeRichiesteException("Stai inviando troppe richieste. Riprova tra un minuto.");
        }
        return collegamentoTelegramRepository.findByChatIdAndScadenzaAfter(chatId, LocalDateTime.now(clock))
                .map(CollegamentoTelegram::getDonatore)
                .orElseThrow(ChatNonCollegataException::new);
    }

    // Il chatId entra nell'hash: lo stesso codice non vale per un'altra chat.
    private static String hash(long chatId, String codice) {
        return TokenConferma.hash(chatId + ":" + codice);
    }
}
