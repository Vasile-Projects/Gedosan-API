package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.config.PrenotazioniProperties;
import it.vszdev.gedosanapi.dto.PrenotazioneRequest;
import it.vszdev.gedosanapi.dto.donatore.DatiDonatoreRequest;
import it.vszdev.gedosanapi.dto.donatore.PrenotazioneResponse;
import it.vszdev.gedosanapi.dto.email.ConfermaPrenotazioneEmailPayload;
import it.vszdev.gedosanapi.enums.Sesso;
import it.vszdev.gedosanapi.events.PrenotazioneEffettuataEvent;
import it.vszdev.gedosanapi.exception.EtaNonValidaException;
import it.vszdev.gedosanapi.exception.GiornoChiusoException;
import it.vszdev.gedosanapi.exception.IntervalloNonRispettatoException;
import it.vszdev.gedosanapi.exception.RisorsaNonTrovataException;
import it.vszdev.gedosanapi.exception.SlotEsauritoException;
import it.vszdev.gedosanapi.models.Donatore;
import it.vszdev.gedosanapi.models.Prenotazione;
import it.vszdev.gedosanapi.models.SlotGiornaliero;
import it.vszdev.gedosanapi.repositories.PrenotazioneRepository;
import it.vszdev.gedosanapi.repositories.SlotGiornalieroRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

@Service
public class PrenotazioneService {

    private final DonatoreService donatoreService;
    private final SlotGiornalieroRepository slotGiornalieroRepository;
    private final PrenotazioneRepository prenotazioneRepository;
    private final PrenotazioniProperties prenotazioniProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final DisponibilitaService disponibilitaService;

    public PrenotazioneService(DonatoreService donatoreService,
                               SlotGiornalieroRepository slotGiornalieroRepository,
                               PrenotazioneRepository prenotazioneRepository,
                               PrenotazioniProperties prenotazioniProperties,
                               ApplicationEventPublisher eventPublisher,
                               DisponibilitaService disponibilitaService) {
        this.donatoreService = donatoreService;
        this.slotGiornalieroRepository = slotGiornalieroRepository;
        this.prenotazioneRepository = prenotazioneRepository;
        this.prenotazioniProperties = prenotazioniProperties;
        this.eventPublisher = eventPublisher;
        this.disponibilitaService = disponibilitaService;
    }

    @Transactional
    public PrenotazioneResponse crea(PrenotazioneRequest request) {
        return new PrenotazioneResponse(creaPrenotazione(request));
    }

    @Transactional
    public Prenotazione creaPrenotazione(PrenotazioneRequest request) {
        Donatore donatore = donatoreService.risolviDonatore(datiDonatore(request));

        SlotGiornaliero slot = slotGiornalieroRepository.findWithLockById(request.idSlot())
                .orElseThrow(() -> new RisorsaNonTrovataException("Slot non trovato"));

        validaSlotPrenotabile(slot, donatore, null);

        Prenotazione prenotazione = new Prenotazione(donatore, slot, request.tipoDonazione());
        prenotazioneRepository.save(prenotazione);
        slot.setPostiOccupati(slot.getPostiOccupati() + 1);

        eventPublisher.publishEvent(new PrenotazioneEffettuataEvent(new ConfermaPrenotazioneEmailPayload(prenotazione)));

        return prenotazione;
    }

    private DatiDonatoreRequest datiDonatore(PrenotazioneRequest request) {
        return new DatiDonatoreRequest(
                request.nome(),
                request.cognome(),
                request.dataNascita(),
                request.sesso(),
                request.codiceFiscale(),
                request.email(),
                request.cellulare());
    }

    // Stesse regole per flusso donatore e per l'admin: nessun bypass lato admin.
    void validaSlotPrenotabile(SlotGiornaliero slot, Donatore donatore, Long idPrenotazioneEsclusa) {
        if (!disponibilitaService.giornoAperto(slot.getTrasfusionale(), slot.getData())) {
            throw new GiornoChiusoException();
        }
        if (slot.getPostiOccupati() >= prenotazioniProperties.getPostiPerSlot()) {
            throw new SlotEsauritoException();
        }
        validaEta(donatore.getDataNascita(), slot.getData());
        validaIntervallo(donatore, slot.getData(), idPrenotazioneEsclusa);
    }

    void validaEta(LocalDate dataNascita, LocalDate dataDonazione) {
        int eta = Period.between(dataNascita, dataDonazione).getYears();
        if (eta < prenotazioniProperties.getEtaMinima()) {
            throw new EtaNonValidaException(prenotazioniProperties.getEtaMinima());
        }
    }

    // In riprogrammazione va esclusa la prenotazione spostata, altrimenti verrebbe confrontata con se stessa.
    void validaIntervallo(Donatore donatore, LocalDate dataDonazione, Long idPrenotazioneEsclusa) {
        var ultima = idPrenotazioneEsclusa == null
                ? prenotazioneRepository.findFirstByDonatore_IdOrderBySlotGiornaliero_DataDesc(donatore.getId())
                : prenotazioneRepository.findFirstByDonatore_IdAndIdNotOrderBySlotGiornaliero_DataDesc(donatore.getId(), idPrenotazioneEsclusa);

        ultima.ifPresent(ultimaPrenotazione -> {
            long giorniTrascorsi = ChronoUnit.DAYS.between(ultimaPrenotazione.getSlotGiornaliero().getData(), dataDonazione);
            int intervalloMinimo = donatore.getSesso() == Sesso.M
                    ? prenotazioniProperties.getIntervalloUominiGiorni()
                    : prenotazioniProperties.getIntervalloDonneGiorni();
            if (giorniTrascorsi < intervalloMinimo) {
                throw new IntervalloNonRispettatoException();
            }
        });
    }
}
