package it.vszdev.gedosanapi.support;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// Base dei test di integrazione: contesto completo su MySQL in container, migrazioni Flyway e dati
// demo (admin "demo", orari, due centri). L'invio email è sostituito da un mock da cui si leggono
// destinatari, testi e token dei link. Turnstile è disattivato (il servizio ha un test a parte).
@SpringBootTest(properties = {
        "jwt.secret=dGVzdC1zZWdyZXRvLWp3dC1zb2xvLXBlci1pLXRlc3QtZGktaW50ZWdyYXppb25lLWdlZG9zYW4=",
        "spring.mail.username=test",
        "spring.mail.password=test",
        "turnstile.enabled=false",
        "app.frontend-url=http://frontend.test",
        "bot.api-key=" + IntegrationTestBase.BOT_KEY,
        "bot.telegram-url=https://t.me/GedosanTestBot",
        // Il mittente email è un mock: l'health check della mail cercherebbe quello reale.
        "management.health.mail.enabled=false"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestBase {

    protected static final String BOT_KEY = "chiave-bot-di-test";
    protected static final ZoneId ZONA = ZoneId.of("Europe/Rome");
    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");
    // Ogni richiesta ha un "IP" diverso: il rate limit per IP non interferisce tra i test.
    private static final AtomicInteger IP = new AtomicInteger();

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @MockitoBean
    protected JavaMailSender mailSender;

    @BeforeEach
    void pulisciDatabase() {
        jdbc.update("DELETE FROM log_modifiche_donatori");
        jdbc.update("DELETE FROM log_modifiche_prenotazioni");
        jdbc.update("DELETE FROM codici_collegamento");
        jdbc.update("DELETE FROM collegamenti_telegram");
        jdbc.update("DELETE FROM log_email");
        jdbc.update("DELETE FROM prenotazioni");
        jdbc.update("DELETE FROM prenotazioni_in_attesa");
        jdbc.update("DELETE FROM donatori");
        jdbc.update("UPDATE slot_giornalieri SET posti_occupati = 0");
    }

    public record Persona(String nome, String cognome, LocalDate dataNascita, boolean donna, String email) {

        public String codiceFiscale() {
            return CodiceFiscaleTest.genera(cognome, nome, dataNascita, donna);
        }

        public String json(long idSlot) {
            return """
                    {"nome":"%s","cognome":"%s","dataNascita":"%s","sesso":"%s","codiceFiscale":"%s",
                     "email":"%s","cellulare":"+393331234567","idSlot":%d,"tipoDonazione":"SI"}
                    """.formatted(nome, cognome, dataNascita, donna ? "F" : "M", codiceFiscale(), email, idSlot);
        }

        public Persona conEmail(String altraEmail) {
            return new Persona(nome, cognome, dataNascita, donna, altraEmail);
        }
    }

    protected static final Persona MARIO = new Persona("Mario", "Rossi", LocalDate.of(1990, 5, 12), false, "mario@test.it");
    protected static final Persona LUCA = new Persona("Luca", "Bianchi", LocalDate.of(1985, 3, 3), false, "luca@test.it");
    protected static final Persona ANNA = new Persona("Anna", "Verdi", LocalDate.of(1992, 7, 21), true, "anna@test.it");

    protected ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(conIp(post(url)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected MockHttpServletRequestBuilder conIp(MockHttpServletRequestBuilder builder) {
        return builder.header("CF-Connecting-IP", "10.9.0." + IP.incrementAndGet());
    }

    // Primo giorno aperto del centro 1 ad almeno una settimana da oggi (evita festivi e anticipo minimo).
    protected LocalDate dataPrenotabile() throws Exception {
        LocalDate data = LocalDate.now(ZONA).plusDays(7);
        while (mvc.perform(conIp(get("/api/trasfusionali/1/slot").param("data", data.toString())))
                .andReturn().getResponse().getStatus() != 200) {
            data = data.plusDays(1);
        }
        return data;
    }

    // Id dello slot del centro 1 per data e orario ("09:00:00"); la consultazione crea lo slot se manca.
    protected long idSlot(LocalDate data, String orario) throws Exception {
        String risposta = mvc.perform(conIp(get("/api/trasfusionali/1/slot").param("data", data.toString())))
                .andReturn().getResponse().getContentAsString();
        List<Integer> id = JsonPath.read(risposta, "$[?(@.orario == '" + orario + "')].idSlot");
        return id.getFirst();
    }

    protected int postiOccupati(long idSlot) {
        return jdbc.queryForObject("SELECT posti_occupati FROM slot_giornalieri WHERE id = ?", Integer.class, idSlot);
    }

    protected int conta(String tabella) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabella, Integer.class);
    }

    // Donatore già registrato, senza prenotazioni.
    protected void registraDonatore(Persona p) {
        jdbc.update("INSERT INTO donatori (nome, cognome, data_nascita, sesso, codice_fiscale, email, cellulare) "
                        + "VALUES (?, ?, ?, ?, ?, ?, '+393330000000')",
                p.nome(), p.cognome(), p.dataNascita(), p.donna() ? "F" : "M", p.codiceFiscale(), p.email());
    }

    // Porta nel passato la scadenza delle attese, in ora italiana come le scrive l'applicazione.
    protected void faiScadereAttese() {
        jdbc.update("UPDATE prenotazioni_in_attesa SET scadenza = ?", LocalDateTime.now(ZONA).minusMinutes(1));
    }

    // Attende (le email partono in asincrono dopo il commit) e restituisce le email inviate finora.
    protected List<SimpleMailMessage> emailInviate(int attese) {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(5000).times(attese)).send(captor.capture());
        return captor.getAllValues();
    }

    protected static String token(SimpleMailMessage email) {
        Matcher m = TOKEN.matcher(email.getText());
        if (!m.find()) {
            throw new AssertionError("Nessun link di conferma nell'email: " + email.getSubject());
        }
        return m.group(1);
    }

    protected static String destinatario(SimpleMailMessage email) {
        return email.getTo()[0];
    }

    // --- Bot Telegram ---

    private static final Pattern CODICE = Pattern.compile("(?m)^\\s*(\\d{6})\\s*$");

    protected MockHttpServletRequestBuilder bot(MockHttpServletRequestBuilder builder) {
        return conIp(builder).header("X-Bot-Key", BOT_KEY);
    }

    protected ResultActions postBot(String url, String json) throws Exception {
        return mvc.perform(bot(post(url)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions richiediCodice(long chatId, String codiceFiscale, String email) throws Exception {
        return postBot("/api/bot/collegamento/richiesta",
                "{\"chatId\":%d,\"codiceFiscale\":\"%s\",\"email\":\"%s\"}".formatted(chatId, codiceFiscale, email));
    }

    protected ResultActions verificaCodice(long chatId, String codice) throws Exception {
        return postBot("/api/bot/collegamento/verifica", "{\"chatId\":%d,\"codice\":\"%s\"}".formatted(chatId, codice));
    }

    protected static String codice(SimpleMailMessage email) {
        Matcher m = CODICE.matcher(email.getText());
        if (!m.find()) {
            throw new AssertionError("Nessun codice nell'email: " + email.getSubject());
        }
        return m.group(1);
    }

    // Prenotazione già esistente in una data qualsiasi (anche passata), scritta direttamente a database.
    protected long prenotazioneDiretta(Persona p, LocalDate data) {
        jdbc.update("INSERT IGNORE INTO slot_giornalieri (id_centro, ora, data, posti_occupati) VALUES (1, '09:00:00', ?, 0)", data);
        Long idSlot = jdbc.queryForObject("SELECT id FROM slot_giornalieri WHERE id_centro = 1 AND ora = '09:00:00' AND data = ?",
                Long.class, data);
        jdbc.update("INSERT INTO prenotazioni (id_donatore, id_slot, tipo_donazione, canale) "
                        + "SELECT id, ?, 'SI', 'SITO' FROM donatori WHERE codice_fiscale = ?", idSlot, p.codiceFiscale());
        jdbc.update("UPDATE slot_giornalieri SET posti_occupati = posti_occupati + 1 WHERE id = ?", idSlot);
        return idSlot;
    }

    protected String tokenAdmin() throws Exception {
        String risposta = postJson("/api/auth/login", "{\"username\":\"demo\",\"password\":\"demo1234\"}")
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(risposta, "$.token");
    }
}
