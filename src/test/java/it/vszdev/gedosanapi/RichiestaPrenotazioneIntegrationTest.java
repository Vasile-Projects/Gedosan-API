package it.vszdev.gedosanapi;

import it.vszdev.gedosanapi.service.ScadenzaPrenotazioniInAttesaJob;
import it.vszdev.gedosanapi.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RichiestaPrenotazioneIntegrationTest extends IntegrationTestBase {

    private static final String MESSAGGIO_GENERICO = "Se i dati sono corretti riceverai a breve un'email";

    @Autowired
    private ScadenzaPrenotazioniInAttesaJob scadenzaJob;

    @Test
    void cfNuovo_richiestaEConferma_creanoDonatoreEPrenotazione() throws Exception {
        long slot = idSlot(dataPrenotabile(), "09:00:00");

        postJson("/api/prenotazioni", MARIO.json(slot))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.messaggio", containsString(MESSAGGIO_GENERICO)));

        assertThat(conta("donatori")).isZero();
        assertThat(conta("prenotazioni_in_attesa")).isEqualTo(1);
        assertThat(postiOccupati(slot)).isEqualTo(1);

        SimpleMailMessage link = emailInviate(1).getFirst();
        assertThat(destinatario(link)).isEqualTo(MARIO.email());
        assertThat(link.getText()).contains("http://frontend.test/conferma?token=");
        String token = token(link);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM prenotazioni_in_attesa WHERE token_hash = SHA2(?, 256)",
                Integer.class, token)).as("in DB solo l'hash del token").isEqualTo(1);

        postJson("/api/prenotazioni/conferma", "{\"token\":\"" + token + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomeDonatore").value("Mario"));

        assertThat(conta("donatori")).isEqualTo(1);
        assertThat(conta("prenotazioni")).isEqualTo(1);
        assertThat(conta("prenotazioni_in_attesa")).isZero();
        assertThat(postiOccupati(slot)).isEqualTo(1);
        assertThat(emailInviate(2).get(1).getSubject()).startsWith("Conferma prenotazione donazione");
    }

    @Test
    void tokenRiusatoOInesistente_404() throws Exception {
        long slot = idSlot(dataPrenotabile(), "09:00:00");
        postJson("/api/prenotazioni", MARIO.json(slot)).andExpect(status().isAccepted());
        String token = token(emailInviate(1).getFirst());

        postJson("/api/prenotazioni/conferma", "{\"token\":\"" + token + "\"}").andExpect(status().isCreated());
        postJson("/api/prenotazioni/conferma", "{\"token\":\"" + token + "\"}").andExpect(status().isNotFound());
        postJson("/api/prenotazioni/conferma", "{\"token\":\"inesistente\"}").andExpect(status().isNotFound());
        postJson("/api/prenotazioni/conferma", "{\"token\":\"\"}").andExpect(status().isBadRequest());
    }

    @Test
    void attesaScaduta_linkNonValido_eJobLiberaIlPosto() throws Exception {
        long slot = idSlot(dataPrenotabile(), "09:00:00");
        postJson("/api/prenotazioni", MARIO.json(slot)).andExpect(status().isAccepted());
        String token = token(emailInviate(1).getFirst());
        assertThat(postiOccupati(slot)).isEqualTo(1);

        faiScadereAttese();
        postJson("/api/prenotazioni/conferma", "{\"token\":\"" + token + "\"}").andExpect(status().isNotFound());

        scadenzaJob.eliminaAtteseScadute();
        assertThat(conta("prenotazioni_in_attesa")).isZero();
        assertThat(postiOccupati(slot)).isZero();
        assertThat(conta("donatori")).isZero();
    }

    @Test
    void cfRegistratoConStessaEmail_riceveIlLink_eLaConfermaUsaIlDonatoreEsistente() throws Exception {
        registraDonatore(MARIO);
        long slot = idSlot(dataPrenotabile(), "09:00:00");

        postJson("/api/prenotazioni", MARIO.json(slot)).andExpect(status().isAccepted());
        String token = token(emailInviate(1).getFirst());
        postJson("/api/prenotazioni/conferma", "{\"token\":\"" + token + "\"}").andExpect(status().isCreated());

        assertThat(conta("donatori")).isEqualTo(1);
        assertThat(conta("prenotazioni")).isEqualTo(1);
    }

    @Test
    void cfRegistratoConAltraEmail_rispostaGenerica_nessunaModifica_avvisoAllEmailRegistrata() throws Exception {
        registraDonatore(MARIO);
        long slot = idSlot(dataPrenotabile(), "09:00:00");

        postJson("/api/prenotazioni", MARIO.conEmail("attaccante@evil.test").json(slot))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.messaggio", containsString(MESSAGGIO_GENERICO)));

        assertThat(conta("prenotazioni_in_attesa")).isZero();
        assertThat(postiOccupati(slot)).isZero();
        assertThat(jdbc.queryForObject("SELECT email FROM donatori", String.class)).isEqualTo(MARIO.email());

        SimpleMailMessage avviso = emailInviate(1).getFirst();
        assertThat(destinatario(avviso)).isEqualTo(MARIO.email());
        assertThat(avviso.getText()).contains("indirizzo\nemail diverso da quello registrato");
    }

    @Test
    void emailDiUnAltroDonatore_rispostaGenerica_avvisoAlProprietario() throws Exception {
        registraDonatore(MARIO);
        long slot = idSlot(dataPrenotabile(), "09:00:00");

        postJson("/api/prenotazioni", LUCA.conEmail(MARIO.email()).json(slot)).andExpect(status().isAccepted());

        assertThat(conta("prenotazioni_in_attesa")).isZero();
        assertThat(conta("donatori")).isEqualTo(1);
        SimpleMailMessage avviso = emailInviate(1).getFirst();
        assertThat(destinatario(avviso)).isEqualTo(MARIO.email());
        assertThat(avviso.getText()).contains("associato a un altro codice fiscale");
    }

    @Test
    void regolaSullaStoriaNonRispettata_rispostaGenerica_spiegazioneSoloViaEmail() throws Exception {
        LocalDate data = dataPrenotabile();
        long slot9 = idSlot(data, "09:00:00");
        long slot10 = idSlot(data, "10:00:00");
        postJson("/api/prenotazioni", MARIO.json(slot9)).andExpect(status().isAccepted());
        postJson("/api/prenotazioni/conferma", "{\"token\":\"" + token(emailInviate(1).getFirst()) + "\"}")
                .andExpect(status().isCreated());

        // Stesso giorno: il messaggio della regola non deve comparire nella risposta HTTP.
        postJson("/api/prenotazioni", MARIO.json(slot10))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.messaggio", containsString(MESSAGGIO_GENERICO)));

        assertThat(conta("prenotazioni_in_attesa")).isZero();
        SimpleMailMessage avviso = emailInviate(3).get(2);
        assertThat(destinatario(avviso)).isEqualTo(MARIO.email());
        assertThat(avviso.getText()).contains("Hai già una prenotazione per quel giorno");
    }

    @Test
    void confermataPiuAttesaRiempionoLoSlot_terzaRichiestaRifiutataSubito() throws Exception {
        long slot = idSlot(dataPrenotabile(), "09:00:00");
        postJson("/api/prenotazioni", MARIO.json(slot)).andExpect(status().isAccepted());
        postJson("/api/prenotazioni/conferma", "{\"token\":\"" + token(emailInviate(1).getFirst()) + "\"}")
                .andExpect(status().isCreated());
        postJson("/api/prenotazioni", LUCA.json(slot)).andExpect(status().isAccepted());
        assertThat(postiOccupati(slot)).isEqualTo(2);

        postJson("/api/prenotazioni", ANNA.json(slot))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.messaggio", containsString("non è più disponibile")));
    }

    @Test
    void nuovaRichiestaDelloStessoCf_sostituisceLaPrecedente() throws Exception {
        LocalDate data = dataPrenotabile();
        long slot9 = idSlot(data, "09:00:00");
        long slot10 = idSlot(data, "10:00:00");

        postJson("/api/prenotazioni", LUCA.json(slot9)).andExpect(status().isAccepted());
        String vecchioToken = token(emailInviate(1).getFirst());
        postJson("/api/prenotazioni", LUCA.json(slot10)).andExpect(status().isAccepted());
        List<SimpleMailMessage> email = emailInviate(2);

        assertThat(conta("prenotazioni_in_attesa")).isEqualTo(1);
        assertThat(postiOccupati(slot9)).isZero();
        assertThat(postiOccupati(slot10)).isEqualTo(1);
        postJson("/api/prenotazioni/conferma", "{\"token\":\"" + vecchioToken + "\"}").andExpect(status().isNotFound());
        postJson("/api/prenotazioni/conferma", "{\"token\":\"" + token(email.get(1)) + "\"}").andExpect(status().isCreated());
    }

    @Test
    void erroriCheNonRiguardanoLaPersona_sonoImmediati() throws Exception {
        long slot = idSlot(dataPrenotabile(), "09:00:00");
        Persona minorenne = new Persona("Giulia", "Neri", LocalDate.now(ZONA).minusYears(16), true, "giulia@test.it");

        postJson("/api/prenotazioni", minorenne.json(slot))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.messaggio", containsString("almeno 18 anni")));
        postJson("/api/prenotazioni", MARIO.json(999_999))
                .andExpect(status().isNotFound());
        assertThat(conta("prenotazioni_in_attesa")).isZero();
    }
}
