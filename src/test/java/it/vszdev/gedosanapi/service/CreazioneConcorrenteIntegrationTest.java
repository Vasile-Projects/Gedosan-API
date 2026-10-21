package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.dto.donatore.DatiDonatoreRequest;
import it.vszdev.gedosanapi.enums.Sesso;
import it.vszdev.gedosanapi.models.Donatore;
import it.vszdev.gedosanapi.models.OrarioValido;
import it.vszdev.gedosanapi.models.SlotGiornaliero;
import it.vszdev.gedosanapi.models.Trasfusionale;
import it.vszdev.gedosanapi.repositories.OrarioValidoRepository;
import it.vszdev.gedosanapi.repositories.TrasfusionaleRepository;
import it.vszdev.gedosanapi.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// Ramo "chi perde rilegge" di risolviSlot e creaDonatore, provocato in modo deterministico: un'altra
// connessione inserisce la stessa riga senza committare, il service non la vede (READ COMMITTED), prova
// l'insert e resta in attesa sul vincolo UNIQUE. Al commit dell'altra connessione l'insert fallisce e il
// service deve restituire la riga del vincitore, non un errore.
class CreazioneConcorrenteIntegrationTest extends IntegrationTestBase {

    @Autowired
    private SlotGiornalieroService slotGiornalieroService;

    @Autowired
    private DonatoreService donatoreService;

    @Autowired
    private TrasfusionaleRepository trasfusionaleRepository;

    @Autowired
    private OrarioValidoRepository orarioValidoRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    void slotCreatoInParallelo_chiPerdeRiceveLoSlotDelVincitore() throws Exception {
        Trasfusionale centro = trasfusionaleRepository.findById(1L).orElseThrow();
        OrarioValido orario = orarioValidoRepository.findAllByOrderByOrarioAsc().getFirst();
        LocalDate data = LocalDate.now(ZONA).plusYears(5);

        Object esito = perdeLaCorsa(
                "INSERT INTO slot_giornalieri (id_centro, ora, data, posti_occupati) VALUES (1, ?, ?, 0)",
                new Object[]{orario.getOrario(), data},
                "insert into slot_giornalieri",
                () -> slotGiornalieroService.risolviSlot(centro, orario, data));

        assertThat(esito).isInstanceOf(SlotGiornaliero.class);
        Long idVincitore = jdbc.queryForObject("SELECT id FROM slot_giornalieri WHERE id_centro = 1 AND ora = ? AND data = ?",
                Long.class, orario.getOrario(), data);
        assertThat(((SlotGiornaliero) esito).getId()).isEqualTo(idVincitore);
    }

    @Test
    void donatoreCreatoInParallelo_chiPerdeRiceveIlDonatoreDelVincitore() throws Exception {
        DatiDonatoreRequest dati = new DatiDonatoreRequest(MARIO.nome(), MARIO.cognome(), MARIO.dataNascita(), Sesso.M,
                MARIO.codiceFiscale(), MARIO.email(), "+393331234567");

        Object esito = perdeLaCorsa(
                "INSERT INTO donatori (nome, cognome, data_nascita, sesso, codice_fiscale, email, cellulare) "
                        + "VALUES (?, ?, ?, 'M', ?, ?, '+393330000000')",
                new Object[]{MARIO.nome(), MARIO.cognome(), MARIO.dataNascita(), MARIO.codiceFiscale(), MARIO.email()},
                "insert into donatori",
                () -> donatoreService.creaDonatore(dati));

        assertThat(esito).isInstanceOf(Donatore.class);
        assertThat(((Donatore) esito).getCodiceFiscale()).isEqualTo(MARIO.codiceFiscale());
        assertThat(conta("donatori")).isEqualTo(1);
    }

    // L'insert del "vincitore" resta aperta; il service parte su un altro thread e, quando il suo insert
    // è in attesa del lock, il vincitore committa. Restituisce il risultato del service o l'eccezione.
    private Object perdeLaCorsa(String insertVincitore, Object[] parametri, String insertDelService,
                                Callable<Object> chiamata) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (Connection vincitore = dataSource.getConnection()) {
            vincitore.setAutoCommit(false);
            try (PreparedStatement ps = vincitore.prepareStatement(insertVincitore)) {
                for (int i = 0; i < parametri.length; i++) {
                    ps.setObject(i + 1, parametri[i]);
                }
                ps.executeUpdate();
            }

            Future<Object> futuro = executor.submit(() -> {
                try {
                    return chiamata.call();
                } catch (Exception e) {
                    return e;
                }
            });
            attendiInsertInAttesa(insertDelService);
            vincitore.commit();
            return futuro.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    // Senza privilegio PROCESS l'utente vede comunque le proprie connessioni nella processlist.
    private void attendiInsertInAttesa(String inizioQuery) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            Integer inAttesa = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE INFO LIKE ?", Integer.class, inizioQuery + "%");
            if (inAttesa > 0) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("L'insert del service non è mai arrivato ad aspettare il lock");
    }
}
