package it.vszdev.gedosanapi;

import it.vszdev.gedosanapi.service.PromemoriaJob;
import it.vszdev.gedosanapi.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BotTelegramIntegrationTest extends IntegrationTestBase {

    // Chat diverse per ogni test: i limiti per chat sono in memoria e il contesto Spring è condiviso.
    private static final AtomicLong CHAT_ID = new AtomicLong(100_000_000L);

    private final long CHAT = CHAT_ID.addAndGet(10);
    private final long ALTRA_CHAT = CHAT + 1;

    @Autowired
    private PromemoriaJob promemoriaJob;

    // --- Collegamento ---

    @Test
    void cfEdEmailScrittiComeCapita_codiceAllEmailRegistrata_chatCollegata() throws Exception {
        registraDonatore(MARIO);

        String cfMinuscolo = " " + MARIO.codiceFiscale().toLowerCase(Locale.ROOT) + " ";
        richiediCodice(CHAT, cfMinuscolo, "  MARIO@Test.IT ")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.messaggio", containsString("http://frontend.test/prenota")));

        SimpleMailMessage email = emailInviate(1).getFirst();
        assertThat(destinatario(email)).isEqualTo(MARIO.email());
        String codice = codice(email);
        assertThat(jdbc.queryForObject("SELECT codice_hash FROM codici_collegamento", String.class))
                .as("in DB solo l'hash").doesNotContain(codice);

        verificaCodice(CHAT, codice)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Mario"))
                .andExpect(jsonPath("$.validoFinoAl").value(LocalDate.now(ZONA).plusDays(180).toString()));

        assertThat(conta("collegamenti_telegram")).isEqualTo(1);
        assertThat(conta("codici_collegamento")).isZero();
    }

    @Test
    void datiNonCorrispondenti_stessaRisposta_nessunaEmail() throws Exception {
        registraDonatore(MARIO);
        String attesa = richiediCodice(CHAT, MARIO.codiceFiscale(), MARIO.email())
                .andReturn().getResponse().getContentAsString();
        emailInviate(1);

        String emailSbagliata = richiediCodice(ALTRA_CHAT, MARIO.codiceFiscale(), "altro@test.it")
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        String cfInesistente = richiediCodice(ALTRA_CHAT + 5, LUCA.codiceFiscale(), LUCA.email())
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();

        assertThat(emailSbagliata).isEqualTo(attesa);
        assertThat(cfInesistente).isEqualTo(attesa);
        emailInviate(1);
        assertThat(conta("codici_collegamento")).isEqualTo(1);
    }

    @Test
    void formatoNonLeggibile_400ConEsempio() throws Exception {
        richiediCodice(CHAT, "NONUNCF", "mario@test.it")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messaggio", containsString("separati da una virgola")));
        richiediCodice(CHAT, MARIO.codiceFiscale(), "mario-senza-chiocciola")
                .andExpect(status().isBadRequest());
        verify(mailSender, after(300).never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void codiceErrato_dopoCinqueTentativiAncheQuelloGiustoNonVale() throws Exception {
        registraDonatore(MARIO);
        richiediCodice(CHAT, MARIO.codiceFiscale(), MARIO.email());
        String codice = codice(emailInviate(1).getFirst());
        String sbagliato = codice.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 5; i++) {
            verificaCodice(CHAT, sbagliato)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.messaggio", containsString("non valido o scaduto")));
        }
        verificaCodice(CHAT, codice).andExpect(status().isBadRequest());
        assertThat(conta("collegamenti_telegram")).isZero();
    }

    @Test
    void codiceValeSoloPerLaChatCheLoHaChiesto() throws Exception {
        registraDonatore(MARIO);
        richiediCodice(CHAT, MARIO.codiceFiscale(), MARIO.email());
        String codice = codice(emailInviate(1).getFirst());

        verificaCodice(ALTRA_CHAT, codice).andExpect(status().isBadRequest());
        verificaCodice(CHAT, codice).andExpect(status().isOk());
    }

    @Test
    void codiceScaduto_nonValido() throws Exception {
        registraDonatore(MARIO);
        richiediCodice(CHAT, MARIO.codiceFiscale(), MARIO.email());
        String codice = codice(emailInviate(1).getFirst());
        jdbc.update("UPDATE codici_collegamento SET scadenza = ?", LocalDateTime.now(ZONA).minusMinutes(1));

        verificaCodice(CHAT, codice).andExpect(status().isBadRequest());
    }

    // --- Sicurezza ---

    @Test
    void senzaChiaveDelBot_401_eIlJwtAdminNonBasta() throws Exception {
        mvc.perform(conIp(get("/api/bot/chat/" + CHAT + "/stato"))).andExpect(status().isUnauthorized());
        mvc.perform(conIp(get("/api/bot/chat/" + CHAT + "/stato")).header("X-Bot-Key", "sbagliata"))
                .andExpect(status().isUnauthorized());
        mvc.perform(conIp(get("/api/bot/chat/" + CHAT + "/stato")).header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void chiaveDelBot_nonApreLAreaAdmin() throws Exception {
        mvc.perform(bot(get("/api/admin/prenotazioni"))).andExpect(status().is4xxClientError());
    }

    @Test
    void chatNonCollegataOScaduta_403() throws Exception {
        mvc.perform(bot(get("/api/bot/chat/" + CHAT + "/stato")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.messaggio", containsString("/collega")));

        collega(MARIO, CHAT);
        mvc.perform(bot(get("/api/bot/chat/" + CHAT + "/stato"))).andExpect(status().isOk());

        jdbc.update("UPDATE collegamenti_telegram SET scadenza = ?", LocalDateTime.now(ZONA).minusMinutes(1));
        mvc.perform(bot(get("/api/bot/chat/" + CHAT + "/stato"))).andExpect(status().isForbidden());
    }

    @Test
    void esci_scollegaLaChat() throws Exception {
        collega(MARIO, CHAT);

        mvc.perform(bot(delete("/api/bot/collegamento/" + CHAT))).andExpect(status().isNoContent());

        mvc.perform(bot(get("/api/bot/chat/" + CHAT + "/prenotazioni"))).andExpect(status().isForbidden());
    }

    // --- Funzioni personali ---

    @Test
    void stato_senzaDonazioni_puoiPrenotareDaOggi() throws Exception {
        collega(MARIO, CHAT);

        mvc.perform(bot(get("/api/bot/chat/" + CHAT + "/stato")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Mario"))
                .andExpect(jsonPath("$.prossimaDataUtile").value(LocalDate.now(ZONA).toString()));
    }

    @Test
    void stato_dopoUnaDonazione_novantaGiorniEMotivo() throws Exception {
        collega(MARIO, CHAT);
        LocalDate ultima = LocalDate.now(ZONA).minusDays(30);
        prenotazioneDiretta(MARIO, ultima);

        mvc.perform(bot(get("/api/bot/chat/" + CHAT + "/stato")))
                .andExpect(jsonPath("$.prossimaDataUtile").value(ultima.plusDays(90).toString()))
                .andExpect(jsonPath("$.motivo", containsString("90 giorni")))
                .andExpect(jsonPath("$.prossimoAppuntamento").isEmpty());
    }

    @Test
    void prenotaDalBot_subitoConfermata_emailConAvviso() throws Exception {
        collega(MARIO, CHAT);
        long slot = idSlot(dataPrenotabile(), "09:00:00");

        mvc.perform(bot(get("/api/bot/slot/" + slot)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orario").value("09:00:00"));

        postBot("/api/bot/chat/" + CHAT + "/prenotazioni", "{\"idSlot\":" + slot + "}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orario").value("09:00:00"));

        assertThat(conta("prenotazioni")).isEqualTo(1);
        assertThat(conta("prenotazioni_in_attesa")).isZero();
        assertThat(jdbc.queryForObject("SELECT canale FROM prenotazioni", String.class)).isEqualTo("TELEGRAM");
        assertThat(postiOccupati(slot)).isEqualTo(1);

        SimpleMailMessage conferma = emailInviate(1).getFirst();
        assertThat(destinatario(conferma)).isEqualTo(MARIO.email());
        assertThat(conferma.getText()).contains("Se non sei stato tu");

        mvc.perform(bot(get("/api/bot/chat/" + CHAT + "/prenotazioni")))
                .andExpect(jsonPath("$[0].orario").value("09:00:00"));
    }

    @Test
    void prenotaDalBot_regoleApplicate_messaggioEsplicito() throws Exception {
        collega(MARIO, CHAT);
        LocalDate data = dataPrenotabile();
        postBot("/api/bot/chat/" + CHAT + "/prenotazioni", "{\"idSlot\":" + idSlot(data, "09:00:00") + "}")
                .andExpect(status().isCreated());

        postBot("/api/bot/chat/" + CHAT + "/prenotazioni", "{\"idSlot\":" + idSlot(data, "10:00:00") + "}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.messaggio", containsString("Hai già una prenotazione per quel giorno")));
    }

    @Test
    void prenotaDalBot_nonModificaLAnagrafica() throws Exception {
        collega(MARIO, CHAT);
        String prima = jdbc.queryForObject("SELECT CONCAT(nome, cognome, email, cellulare) FROM donatori", String.class);

        postBot("/api/bot/chat/" + CHAT + "/prenotazioni", "{\"idSlot\":" + idSlot(dataPrenotabile(), "09:00:00") + "}")
                .andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT CONCAT(nome, cognome, email, cellulare) FROM donatori", String.class))
                .isEqualTo(prima);
    }

    // --- Promemoria ed email del sito ---

    @Test
    void promemoria_unaSolaVoltaPerLaPrenotazioneDiDomani() throws Exception {
        registraDonatore(MARIO);
        prenotazioneDiretta(MARIO, LocalDate.now(ZONA).plusDays(1));
        registraDonatore(LUCA);
        prenotazioneDiretta(LUCA, LocalDate.now(ZONA).plusDays(2));

        promemoriaJob.inviaPromemoriaDiDomani();
        promemoriaJob.inviaPromemoriaDiDomani();

        SimpleMailMessage promemoria = emailInviate(1).getFirst();
        assertThat(destinatario(promemoria)).isEqualTo(MARIO.email());
        assertThat(promemoria.getSubject()).startsWith("Promemoria");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM prenotazioni WHERE promemoria_inviato_at IS NOT NULL",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void confermaDalSito_invitaATelegram() throws Exception {
        long slot = idSlot(dataPrenotabile(), "09:00:00");
        postJson("/api/prenotazioni", MARIO.json(slot)).andExpect(status().isAccepted());
        postJson("/api/prenotazioni/conferma", "{\"token\":\"" + token(emailInviate(1).getFirst()) + "\"}")
                .andExpect(status().isCreated());

        assertThat(emailInviate(2).get(1).getText()).contains("https://t.me/GedosanTestBot");
        assertThat(jdbc.queryForObject("SELECT canale FROM prenotazioni", String.class)).isEqualTo("SITO");
    }

    @Test
    void swaggerPubblico() throws Exception {
        mvc.perform(conIp(get("/v3/api-docs")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/bot/chat/{chatId}/stato']").exists());
    }

    // Collega la chat al donatore (registrandolo) con il flusso reale del codice via email.
    private void collega(Persona p, long chatId) throws Exception {
        registraDonatore(p);
        richiediCodice(chatId, p.codiceFiscale(), p.email()).andExpect(status().isAccepted());
        verificaCodice(chatId, codice(emailInviate(1).getFirst())).andExpect(status().isOk());
        clearInvocations(mailSender);
    }
}
