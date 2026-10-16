package it.vszdev.gedosanapi;

import it.vszdev.gedosanapi.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminDonatoreIntegrationTest extends IntegrationTestBase {

    private String token;
    private long idMario;

    @BeforeEach
    void preparaDonatori() throws Exception {
        registraDonatore(MARIO);
        registraDonatore(LUCA);
        idMario = jdbc.queryForObject("SELECT id FROM donatori WHERE email = ?", Long.class, MARIO.email());
        token = tokenAdmin();
    }

    @Test
    void senzaToken_accessoNegato() throws Exception {
        mvc.perform(conIp(get("/api/admin/donatori/" + idMario))).andExpect(status().isForbidden());
        mvc.perform(conIp(patch("/api/admin/donatori/" + idMario)).contentType(MediaType.APPLICATION_JSON)
                .content(modifica(MARIO.dataNascita().toString(), "nuova@test.it"))).andExpect(status().isForbidden());
    }

    @Test
    void ricerca_perCognomeCfEdEmail() throws Exception {
        mvc.perform(conIp(get("/api/admin/donatori").param("q", "ros")).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cognome").value("Rossi"));
        mvc.perform(conIp(get("/api/admin/donatori").param("q", LUCA.codiceFiscale().toLowerCase())).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].cognome").value("Bianchi"));
        mvc.perform(conIp(get("/api/admin/donatori").param("q", "MARIO@test.it")).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].cognome").value("Rossi"));
        mvc.perform(conIp(get("/api/admin/donatori").param("q", "r")).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void modifica_cfIncoerente_400() throws Exception {
        mvc.perform(conIp(patch("/api/admin/donatori/" + idMario)).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(modifica("1991-05-12", MARIO.email())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erroriValidazione[0].campo").value("codiceFiscale"));
        assertThat(conta("log_modifiche_donatori")).isZero();
    }

    @Test
    void modifica_emailDiUnAltroDonatore_409() throws Exception {
        mvc.perform(conIp(patch("/api/admin/donatori/" + idMario)).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(modifica(MARIO.dataNascita().toString(), LUCA.email())))
                .andExpect(status().isConflict());
        assertThat(conta("log_modifiche_donatori")).isZero();
    }

    @Test
    void modificaValida_aggiornaERegistraSoloICampiCambiati() throws Exception {
        mvc.perform(conIp(patch("/api/admin/donatori/" + idMario)).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(modifica(MARIO.dataNascita().toString(), "Mario.Nuova@Test.it")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("mario.nuova@test.it"));

        // Stessi dati: nessuna nuova riga di log.
        mvc.perform(conIp(patch("/api/admin/donatori/" + idMario)).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(modifica(MARIO.dataNascita().toString(), "mario.nuova@test.it")))
                .andExpect(status().isOk());

        mvc.perform(conIp(get("/api/admin/donatori/" + idMario)).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modifiche", hasSize(1)))
                .andExpect(jsonPath("$.modifiche[0].campiModificati").value("email,cellulare"))
                .andExpect(jsonPath("$.modifiche[0].usernameAdmin").value("demo"));
    }

    private static String modifica(String dataNascita, String email) {
        return """
                {"nome":"Mario","cognome":"Rossi","dataNascita":"%s","sesso":"M","codiceFiscale":"%s",
                 "email":"%s","cellulare":"+393339999999"}
                """.formatted(dataNascita, MARIO.codiceFiscale(), email);
    }
}
