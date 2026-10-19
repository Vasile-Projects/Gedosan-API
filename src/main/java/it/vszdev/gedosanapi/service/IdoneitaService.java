package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.config.PrenotazioniProperties;
import it.vszdev.gedosanapi.dto.bot.PrenotazioneBotResponse;
import it.vszdev.gedosanapi.dto.bot.StatoDonatoreResponse;
import it.vszdev.gedosanapi.enums.Sesso;
import it.vszdev.gedosanapi.models.Donatore;
import it.vszdev.gedosanapi.models.Prenotazione;
import it.vszdev.gedosanapi.repositories.PrenotazioneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.List;

// "Quando posso donare di nuovo?": la prima data che le regole di PrenotazioneService (età, intervallo
// minimo, limite annuo delle donatrici, solo date successive all'ultima prenotazione) accetterebbero.
// Non considera calendario e posti liberi, che dipendono dal centro: è la data "a partire dalla quale".
@Service
public class IdoneitaService {

    private final PrenotazioneRepository prenotazioneRepository;
    private final PrenotazioniProperties prenotazioniProperties;
    private final Clock clock;

    public IdoneitaService(PrenotazioneRepository prenotazioneRepository,
                           PrenotazioniProperties prenotazioniProperties,
                           Clock clock) {
        this.prenotazioneRepository = prenotazioneRepository;
        this.prenotazioniProperties = prenotazioniProperties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public StatoDonatoreResponse stato(Donatore donatore) {
        LocalDate oggi = LocalDate.now(clock);
        // Dalla più recente: per le regole le prenotazioni sono sempre in ordine cronologico.
        List<Prenotazione> prenotazioni = prenotazioneRepository
                .findAllByDonatore_IdOrderBySlotGiornaliero_DataDesc(donatore.getId());

        PrenotazioneBotResponse prossimoAppuntamento = prenotazioni.stream()
                .filter(p -> !p.getSlotGiornaliero().getData().isBefore(oggi))
                .min(Comparator.comparing(p -> p.getSlotGiornaliero().getData()))
                .map(PrenotazioneBotResponse::new)
                .orElse(null);

        LocalDate dataUtile = oggi;
        String motivo = "Puoi prenotare già da ora.";

        if (!prenotazioni.isEmpty()) {
            LocalDate ultima = prenotazioni.getFirst().getSlotGiornaliero().getData();
            int intervallo = donatore.getSesso() == Sesso.M
                    ? prenotazioniProperties.getIntervalloUominiGiorni()
                    : prenotazioniProperties.getDistanzaMinimaDonneGiorni();
            LocalDate dopoIntervallo = ultima.plusDays(intervallo);
            if (dopoIntervallo.isAfter(dataUtile)) {
                dataUtile = dopoIntervallo;
                motivo = (ultima.isBefore(oggi)
                        ? "L'ultima donazione è stata il " + ultima.format(FormatiData.DATA)
                        : "Hai già un appuntamento il " + ultima.format(FormatiData.DATA))
                        + " e tra due donazioni servono almeno " + intervallo + " giorni.";
            }
        }

        // Donatrici: al massimo N donazioni nei giorni [data - finestra, data - 1], estremi inclusi (come in
        // validaLimiteDonneNellaFinestra). La N-esima più recente deve uscire dalla finestra, quindi il primo
        // giorno utile è finestra + 1 giorni dopo; da lì le donazioni nella finestra sono al più N-1.
        int massimo = prenotazioniProperties.getMaxDonazioniDonnePerFinestra();
        if (donatore.getSesso() == Sesso.F && prenotazioni.size() >= massimo) {
            LocalDate fuoriFinestra = prenotazioni.get(massimo - 1).getSlotGiornaliero().getData()
                    .plusDays(prenotazioniProperties.getFinestraLimiteDonneGiorni() + 1L);
            if (fuoriFinestra.isAfter(dataUtile)) {
                dataUtile = fuoriFinestra;
                motivo = "Le donatrici possono fare al massimo " + massimo + " donazioni in 12 mesi.";
            }
        }

        LocalDate maggioreEta = primoGiornoConEta(donatore.getDataNascita(), prenotazioniProperties.getEtaMinima());
        if (maggioreEta.isAfter(dataUtile)) {
            dataUtile = maggioreEta;
            motivo = "Si può donare dai " + prenotazioniProperties.getEtaMinima() + " anni.";
        }

        LocalDate oltreEtaMassima = primoGiornoConEta(donatore.getDataNascita(), prenotazioniProperties.getEtaMassima() + 1);
        if (!dataUtile.isBefore(oltreEtaMassima)) {
            return new StatoDonatoreResponse(donatore.getNome(), null,
                    "Oltre i " + prenotazioniProperties.getEtaMassima() + " anni la prenotazione online non è possibile: "
                            + "contatta il tuo centro trasfusionale per valutare l'idoneità.",
                    prossimoAppuntamento);
        }

        return new StatoDonatoreResponse(donatore.getNome(), dataUtile, motivo, prossimoAppuntamento);
    }

    // Primo giorno in cui l'età compiuta (calcolata come in PrenotazioneService.validaEta) raggiunge
    // `anni`. Il ciclo copre i nati il 29 febbraio, per cui plusYears cade sul 28.
    private static LocalDate primoGiornoConEta(LocalDate nascita, int anni) {
        LocalDate data = nascita.plusYears(anni);
        while (Period.between(nascita, data).getYears() < anni) {
            data = data.plusDays(1);
        }
        return data;
    }
}
