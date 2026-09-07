package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.config.PrenotazioniProperties;
import it.vszdev.gedosanapi.config.TimeConfig;
import it.vszdev.gedosanapi.dto.disponibilita.GiorniNonDisponibiliResponse;
import it.vszdev.gedosanapi.dto.disponibilita.SlotDisponibileResponse;
import it.vszdev.gedosanapi.exception.RegolaPrenotazioneException;
import it.vszdev.gedosanapi.models.OrarioValido;
import it.vszdev.gedosanapi.models.SlotGiornaliero;
import it.vszdev.gedosanapi.models.Trasfusionale;
import it.vszdev.gedosanapi.models.VariazioneApertura;
import it.vszdev.gedosanapi.repositories.FestivoRicorrenteRepository;
import it.vszdev.gedosanapi.repositories.OrarioValidoRepository;
import it.vszdev.gedosanapi.repositories.VariazioneAperturaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class DisponibilitaService {

    private final TrasfusionaleService trasfusionaleService;
    private final OrarioValidoRepository orarioValidoRepository;
    private final FestivoRicorrenteRepository festivoRicorrenteRepository;
    private final VariazioneAperturaRepository variazioneAperturaRepository;
    private final SlotGiornalieroService slotGiornalieroService;
    private final PrenotazioniProperties prenotazioniProperties;
    private final Clock clock;

    public DisponibilitaService(TrasfusionaleService trasfusionaleService,
                                OrarioValidoRepository orarioValidoRepository,
                                FestivoRicorrenteRepository festivoRicorrenteRepository,
                                VariazioneAperturaRepository variazioneAperturaRepository,
                                SlotGiornalieroService slotGiornalieroService,
                                PrenotazioniProperties prenotazioniProperties,
                                Clock clock) {
        this.trasfusionaleService = trasfusionaleService;
        this.orarioValidoRepository = orarioValidoRepository;
        this.festivoRicorrenteRepository = festivoRicorrenteRepository;
        this.variazioneAperturaRepository = variazioneAperturaRepository;
        this.slotGiornalieroService = slotGiornalieroService;
        this.prenotazioniProperties = prenotazioniProperties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public GiorniNonDisponibiliResponse giorniNonDisponibili(Long idTrasfusionale, YearMonth mese) {
        Trasfusionale trasfusionale = trasfusionaleService.trova(idTrasfusionale);

        List<LocalTime> orariValidi = orarioValidoRepository.findAllByOrderByOrarioAsc().stream()
                .map(OrarioValido::getOrario)
                .toList();

        List<LocalDate> giorniChiusi = mese.atDay(1).datesUntil(mese.atEndOfMonth().plusDays(1))
                .filter(data -> !entroOrizzonte(data)
                        || !giornoAperto(trasfusionale, data)
                        || nessunoSlotEntroAnticipo(data, orariValidi))
                .toList();

        return new GiorniNonDisponibiliResponse(giorniChiusi);
    }

    // Nonostante readOnly, risolviSlot crea gli slot mancanti in una transazione propria (REQUIRES_NEW).
    @Transactional(readOnly = true)
    public List<SlotDisponibileResponse> slotDisponibili(Long idTrasfusionale, LocalDate data) {
        Trasfusionale trasfusionale = trasfusionaleService.trova(idTrasfusionale);

        if (!entroOrizzonte(data)) {
            throw new RegolaPrenotazioneException("La data richiesta è troppo lontana: si può prenotare al massimo "
                    + prenotazioniProperties.getOrizzonteGiorni() + " giorni in anticipo.");
        }
        if (!giornoAperto(trasfusionale, data)) {
            throw new RegolaPrenotazioneException(
                    "Il trasfusionale selezionato è chiuso nella data richiesta. Scegli un'altra data.");
        }

        return orarioValidoRepository.findAllByOrderByOrarioAsc().stream()
                .map(orario -> {
                    SlotGiornaliero slot = slotGiornalieroService.risolviSlot(trasfusionale, orario, data);
                    int postiLiberi = prenotazioniProperties.getPostiPerSlot() - slot.getPostiOccupati();
                    boolean disponibile = postiLiberi > 0 && anticipoRispettato(data, orario.getOrario());
                    return new SlotDisponibileResponse(slot.getId(), orario.getOrario(), disponibile, Math.max(postiLiberi, 0));
                })
                .toList();
    }

    // Orizzonte di prenotabilità: si può prenotare al massimo orizzonteGiorni in anticipo rispetto a oggi.
    private boolean entroOrizzonte(LocalDate data) {
        return !data.isAfter(LocalDate.now(clock).plusDays(prenotazioniProperties.getOrizzonteGiorni()));
    }

    // Un giorno è già interamente non prenotabile (oggi passato un certo orario, o un giorno trascorso)
    // quando nessuno dei suoi orari validi rispetta più l'anticipo minimo: va grigiato nel calendario
    // esattamente come un giorno di chiusura. Per le date future la condizione è sempre falsa.
    private boolean nessunoSlotEntroAnticipo(LocalDate data, List<LocalTime> orariValidi) {
        return orariValidi.stream().noneMatch(orario -> anticipoRispettato(data, orario));
    }

    // Coerente con PrenotazioneService: lo slot resta disponibile solo finché il suo orario dista
    // almeno anticipoMinimoMinuti da adesso (confine incluso). Confronto sulla timeline degli istanti,
    // così i minuti di anticipo sono minuti reali anche a cavallo del cambio ora.
    private boolean anticipoRispettato(LocalDate data, LocalTime orario) {
        ZonedDateTime inizioSlot = LocalDateTime.of(data, orario).atZone(TimeConfig.ZONA);
        ZonedDateTime limiteAnticipo = ZonedDateTime.now(clock).plusMinutes(prenotazioniProperties.getAnticipoMinimoMinuti());
        return !inizioSlot.isBefore(limiteAnticipo);
    }

    // Una VariazioneApertura per quella data, se presente, vince sempre sul calendario dei festivi ricorrenti.
    public boolean giornoAperto(Trasfusionale trasfusionale, LocalDate data) {
        return variazioneAperturaRepository.findFirstByTrasfusionale_IdAndDataVariazioneOrderByIdDesc(trasfusionale.getId(), data)
                .map(VariazioneApertura::getApertura)
                .orElseGet(() -> !festivoRicorrenteRepository.existsByMeseAndGiorno(data.getMonthValue(), data.getDayOfMonth()));
    }
}
