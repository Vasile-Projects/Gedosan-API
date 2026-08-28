package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.config.PrenotazioniProperties;
import it.vszdev.gedosanapi.dto.disponibilita.GiorniNonDisponibiliResponse;
import it.vszdev.gedosanapi.dto.disponibilita.SlotDisponibileResponse;
import it.vszdev.gedosanapi.exception.GiornoChiusoException;
import it.vszdev.gedosanapi.models.SlotGiornaliero;
import it.vszdev.gedosanapi.models.Trasfusionale;
import it.vszdev.gedosanapi.models.VariazioneApertura;
import it.vszdev.gedosanapi.repositories.FestivoRicorrenteRepository;
import it.vszdev.gedosanapi.repositories.OrarioValidoRepository;
import it.vszdev.gedosanapi.repositories.VariazioneAperturaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class DisponibilitaService {

    private final TrasfusionaleService trasfusionaleService;
    private final OrarioValidoRepository orarioValidoRepository;
    private final FestivoRicorrenteRepository festivoRicorrenteRepository;
    private final VariazioneAperturaRepository variazioneAperturaRepository;
    private final SlotGiornalieroService slotGiornalieroService;
    private final PrenotazioniProperties prenotazioniProperties;

    public DisponibilitaService(TrasfusionaleService trasfusionaleService,
                                OrarioValidoRepository orarioValidoRepository,
                                FestivoRicorrenteRepository festivoRicorrenteRepository,
                                VariazioneAperturaRepository variazioneAperturaRepository,
                                SlotGiornalieroService slotGiornalieroService,
                                PrenotazioniProperties prenotazioniProperties) {
        this.trasfusionaleService = trasfusionaleService;
        this.orarioValidoRepository = orarioValidoRepository;
        this.festivoRicorrenteRepository = festivoRicorrenteRepository;
        this.variazioneAperturaRepository = variazioneAperturaRepository;
        this.slotGiornalieroService = slotGiornalieroService;
        this.prenotazioniProperties = prenotazioniProperties;
    }

    @Transactional(readOnly = true)
    public GiorniNonDisponibiliResponse giorniNonDisponibili(Long idTrasfusionale, YearMonth mese) {
        Trasfusionale trasfusionale = trasfusionaleService.trova(idTrasfusionale);

        List<LocalDate> giorniChiusi = mese.atDay(1).datesUntil(mese.atEndOfMonth().plusDays(1))
                .filter(data -> !giornoAperto(trasfusionale, data))
                .toList();

        return new GiorniNonDisponibiliResponse(giorniChiusi);
    }

    // Nonostante readOnly, risolviSlot crea gli slot mancanti in una transazione propria (REQUIRES_NEW).
    @Transactional(readOnly = true)
    public List<SlotDisponibileResponse> slotDisponibili(Long idTrasfusionale, LocalDate data) {
        Trasfusionale trasfusionale = trasfusionaleService.trova(idTrasfusionale);

        if (!giornoAperto(trasfusionale, data)) {
            throw new GiornoChiusoException();
        }

        return orarioValidoRepository.findAllByOrderByOrarioAsc().stream()
                .map(orario -> {
                    SlotGiornaliero slot = slotGiornalieroService.risolviSlot(trasfusionale, orario, data);
                    int postiLiberi = prenotazioniProperties.getPostiPerSlot() - slot.getPostiOccupati();
                    return new SlotDisponibileResponse(slot.getId(), orario.getOrario(), postiLiberi > 0, Math.max(postiLiberi, 0));
                })
                .toList();
    }

    // Una VariazioneApertura per quella data, se presente, vince sempre sul calendario dei festivi ricorrenti.
    public boolean giornoAperto(Trasfusionale trasfusionale, LocalDate data) {
        return variazioneAperturaRepository.findFirstByTrasfusionale_IdAndDataVariazioneOrderByIdDesc(trasfusionale.getId(), data)
                .map(VariazioneApertura::getApertura)
                .orElseGet(() -> !festivoRicorrenteRepository.existsByMeseAndGiorno(data.getMonthValue(), data.getDayOfMonth()));
    }
}
