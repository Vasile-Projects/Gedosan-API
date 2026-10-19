package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.dto.bot.StatoDonatoreResponse;
import it.vszdev.gedosanapi.exception.RegolaPrenotazioneException;
import it.vszdev.gedosanapi.models.Donatore;
import it.vszdev.gedosanapi.repositories.DonatoreRepository;
import it.vszdev.gedosanapi.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// La data proposta dal bot deve essere esattamente la prima che le regole di prenotazione accettano:
// accettata quel giorno, rifiutata il giorno prima.
class IdoneitaServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private IdoneitaService idoneitaService;

    @Autowired
    private PrenotazioneService prenotazioneService;

    @Autowired
    private DonatoreRepository donatoreRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void donatore_dopoUnaDonazionePassata() {
        registraDonatore(MARIO);
        prenotazioneDiretta(MARIO, oggi().minusDays(40));

        verificaPrimaDataAccettata(MARIO, oggi().plusDays(50));
    }

    @Test
    void donatore_conAppuntamentoFuturo() {
        registraDonatore(MARIO);
        prenotazioneDiretta(MARIO, oggi().plusDays(10));

        StatoDonatoreResponse stato = verificaPrimaDataAccettata(MARIO, oggi().plusDays(100));
        assertThat(stato.prossimoAppuntamento().data()).isEqualTo(oggi().plusDays(10));
        assertThat(stato.motivo()).contains("Hai già un appuntamento");
    }

    @Test
    void donatrice_limiteDiDueDonazioniInDodiciMesi() {
        registraDonatore(ANNA);
        prenotazioneDiretta(ANNA, oggi().minusDays(200));
        prenotazioneDiretta(ANNA, oggi().minusDays(100));

        StatoDonatoreResponse stato = verificaPrimaDataAccettata(ANNA, oggi().minusDays(200).plusDays(366));
        assertThat(stato.motivo()).contains("al massimo 2 donazioni");
    }

    @Test
    void oltreEtaMassima_nessunaData() {
        Persona anziano = new Persona("Carlo", "Gialli", oggi().minusYears(66), false, "carlo@test.it");
        registraDonatore(anziano);

        StatoDonatoreResponse stato = idoneitaService.stato(donatore(anziano));
        assertThat(stato.prossimaDataUtile()).isNull();
        assertThat(stato.motivo()).contains("contatta il tuo centro");
    }

    private StatoDonatoreResponse verificaPrimaDataAccettata(Persona p, LocalDate attesa) {
        Donatore donatore = donatore(p);
        StatoDonatoreResponse stato = idoneitaService.stato(donatore);

        assertThat(stato.prossimaDataUtile()).isEqualTo(attesa);
        assertThatCode(() -> validaIntervallo(donatore, attesa)).doesNotThrowAnyException();
        assertThatThrownBy(() -> validaIntervallo(donatore, attesa.minusDays(1)))
                .isInstanceOf(RegolaPrenotazioneException.class);
        return stato;
    }

    // In esercizio le regole girano sempre dentro la transazione della prenotazione.
    private void validaIntervallo(Donatore donatore, LocalDate data) {
        transactionTemplate.executeWithoutResult(t -> prenotazioneService.validaIntervallo(donatore, data, null));
    }

    private Donatore donatore(Persona p) {
        return donatoreRepository.findByCodiceFiscale(p.codiceFiscale()).orElseThrow();
    }

    private static LocalDate oggi() {
        return LocalDate.now(ZONA);
    }
}
