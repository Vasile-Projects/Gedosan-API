package it.vszdev.gedosanapi;

import it.vszdev.gedosanapi.dto.PrenotazioneRequest;
import it.vszdev.gedosanapi.enums.Sesso;
import it.vszdev.gedosanapi.enums.TipoDonazione;
import it.vszdev.gedosanapi.exception.RegolaPrenotazioneException;
import it.vszdev.gedosanapi.exception.RisorsaNonTrovataException;
import it.vszdev.gedosanapi.service.RichiestaPrenotazioneService;
import it.vszdev.gedosanapi.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Concorrenza reale sul database: più thread chiamano il service nello stesso istante e i lock
// pessimistici devono serializzarli senza overbooking né doppie conferme.
class ConcorrenzaPrenotazioneIntegrationTest extends IntegrationTestBase {

    @Autowired
    private RichiestaPrenotazioneService richiestaPrenotazioneService;

    @Test
    void doppioClickSulloStessoLink_unaSolaConferma() throws Exception {
        long slot = idSlot(dataPrenotabile(), "09:00:00");
        postJson("/api/prenotazioni", MARIO.json(slot)).andExpect(status().isAccepted());
        String token = token(emailInviate(1).getFirst());

        List<Object> esiti = inParallelo(List.of(
                () -> richiestaPrenotazioneService.conferma(token),
                () -> richiestaPrenotazioneService.conferma(token)));

        assertThat(esiti).filteredOn(e -> e instanceof RisorsaNonTrovataException).hasSize(1);
        assertThat(esiti).filteredOn(e -> !(e instanceof Exception)).hasSize(1);
        assertThat(conta("prenotazioni")).isEqualTo(1);
        assertThat(postiOccupati(slot)).isEqualTo(1);
    }

    @Test
    void dueRichiesteSullUltimoPosto_unaSolaPassa() throws Exception {
        long slot = idSlot(dataPrenotabile(), "09:00:00");
        postJson("/api/prenotazioni", MARIO.json(slot)).andExpect(status().isAccepted());
        assertThat(postiOccupati(slot)).isEqualTo(1);

        List<Object> esiti = inParallelo(List.of(
                () -> { richiestaPrenotazioneService.richiedi(richiesta(LUCA, slot)); return "ok"; },
                () -> { richiestaPrenotazioneService.richiedi(richiesta(ANNA, slot)); return "ok"; }));

        assertThat(esiti).filteredOn("ok"::equals).hasSize(1);
        assertThat(esiti).filteredOn(e -> e instanceof RegolaPrenotazioneException).hasSize(1);
        assertThat(conta("prenotazioni_in_attesa")).isEqualTo(2);
        assertThat(postiOccupati(slot)).isEqualTo(2);
    }

    private static PrenotazioneRequest richiesta(Persona p, long idSlot) {
        return new PrenotazioneRequest(p.nome(), p.cognome(), p.dataNascita(), p.donna() ? Sesso.F : Sesso.M,
                p.codiceFiscale(), p.email(), "+393331234567", idSlot, TipoDonazione.SI, null);
    }

    // Avvia tutti i compiti nello stesso istante e restituisce per ognuno il risultato o l'eccezione.
    private static List<Object> inParallelo(List<Callable<Object>> compiti) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(compiti.size());
        CountDownLatch via = new CountDownLatch(1);
        try {
            List<Future<Object>> futuri = new ArrayList<>();
            for (Callable<Object> compito : compiti) {
                futuri.add(executor.submit(() -> {
                    via.await();
                    try {
                        return compito.call();
                    } catch (Exception e) {
                        return e;
                    }
                }));
            }
            via.countDown();
            List<Object> esiti = new ArrayList<>();
            for (Future<Object> futuro : futuri) {
                esiti.add(futuro.get(30, TimeUnit.SECONDS));
            }
            return esiti;
        } finally {
            executor.shutdownNow();
        }
    }
}
