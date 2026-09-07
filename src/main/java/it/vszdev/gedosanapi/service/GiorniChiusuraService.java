package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.dto.admin.GiornoChiusuraResponse;
import it.vszdev.gedosanapi.enums.TipoChiusura;
import it.vszdev.gedosanapi.exception.RichiestaNonValidaException;
import it.vszdev.gedosanapi.models.FestivoRicorrente;
import it.vszdev.gedosanapi.models.VariazioneApertura;
import it.vszdev.gedosanapi.repositories.FestivoRicorrenteRepository;
import it.vszdev.gedosanapi.repositories.VariazioneAperturaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class GiorniChiusuraService {

    private final FestivoRicorrenteRepository festivoRicorrenteRepository;
    private final VariazioneAperturaRepository variazioneAperturaRepository;
    private final TrasfusionaleService trasfusionaleService;
    private final Clock clock;

    public GiorniChiusuraService(FestivoRicorrenteRepository festivoRicorrenteRepository,
                                 VariazioneAperturaRepository variazioneAperturaRepository,
                                 TrasfusionaleService trasfusionaleService,
                                 Clock clock) {
        this.festivoRicorrenteRepository = festivoRicorrenteRepository;
        this.variazioneAperturaRepository = variazioneAperturaRepository;
        this.trasfusionaleService = trasfusionaleService;
        this.clock = clock;
    }

    // Riepilogo dei giorni in cui il centro è chiuso, dall'oggi (per l'anno corrente) o dal 1 gennaio
    // (per un anno futuro) fino al 31 dicembre dell'anno richiesto. Regola di precedenza: una
    // variazione di apertura del centro vince sul festivo ordinario sulla stessa data.
    @Transactional(readOnly = true)
    public List<GiornoChiusuraResponse> elenco(Long idTrasfusionale, Integer annoRichiesto) {
        trasfusionaleService.trova(idTrasfusionale); // 404 se il centro non esiste

        int annoCorrente = LocalDate.now(clock).getYear();
        int anno = annoRichiesto != null ? annoRichiesto : annoCorrente;
        if (anno < annoCorrente) {
            throw new RichiestaNonValidaException("L'anno deve essere quello corrente o uno successivo");
        }

        LocalDate da = anno == annoCorrente ? LocalDate.now(clock) : LocalDate.of(anno, 1, 1);
        LocalDate a = LocalDate.of(anno, 12, 31);

        // TreeMap: le chiusure risultano già ordinate per data.
        Map<LocalDate, GiornoChiusuraResponse> chiusurePerData = new TreeMap<>();

        // Festivi ordinari risolti all'anno richiesto e ricadenti nell'intervallo.
        for (FestivoRicorrente festivo : festivoRicorrenteRepository.findAll()) {
            LocalDate data = risolvi(festivo, anno);
            if (data != null && !data.isBefore(da) && !data.isAfter(a)) {
                chiusurePerData.put(data, new GiornoChiusuraResponse(data, festivo.getDescrizione(), TipoChiusura.ORDINARIO));
            }
        }

        // Variazioni straordinarie del centro: per ogni data conta solo l'ultima inserita (id maggiore).
        Map<LocalDate, VariazioneApertura> effettivaPerData = new HashMap<>();
        for (VariazioneApertura variazione : variazioneAperturaRepository
                .findAllByTrasfusionale_IdAndDataVariazioneBetween(idTrasfusionale, da, a)) {
            effettivaPerData.merge(variazione.getDataVariazione(), variazione,
                    (esistente, nuova) -> nuova.getId() > esistente.getId() ? nuova : esistente);
        }

        effettivaPerData.forEach((data, variazione) -> {
            if (Boolean.FALSE.equals(variazione.getApertura())) {
                chiusurePerData.put(data, new GiornoChiusuraResponse(data, descrizioneStraordinaria(variazione), TipoChiusura.STRAORDINARIO));
            } else {
                // Apertura straordinaria: il centro è aperto, elimina l'eventuale festivo ordinario.
                chiusurePerData.remove(data);
            }
        });

        return List.copyOf(chiusurePerData.values());
    }

    private LocalDate risolvi(FestivoRicorrente festivo, int anno) {
        try {
            return LocalDate.of(anno, festivo.getMese(), festivo.getGiorno());
        } catch (DateTimeException e) {
            return null; // data non valida in tabella (es. 30/02) o 29/02 in anno non bisestile
        }
    }

    private String descrizioneStraordinaria(VariazioneApertura variazione) {
        String motivo = variazione.getMotivo();
        return motivo != null && !motivo.isBlank() ? motivo : "Chiusura straordinaria";
    }
}
