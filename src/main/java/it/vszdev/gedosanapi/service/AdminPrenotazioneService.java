package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.dto.PrenotazioneRequest;
import it.vszdev.gedosanapi.dto.admin.PrenotazioneAdminResponse;
import it.vszdev.gedosanapi.enums.AzioneAdmin;
import it.vszdev.gedosanapi.exception.RegolaPrenotazioneException;
import it.vszdev.gedosanapi.exception.RisorsaNonTrovataException;
import it.vszdev.gedosanapi.models.Admin;
import it.vszdev.gedosanapi.models.Donatore;
import it.vszdev.gedosanapi.models.LogModificaPrenotazione;
import it.vszdev.gedosanapi.models.Prenotazione;
import it.vszdev.gedosanapi.models.SlotGiornaliero;
import it.vszdev.gedosanapi.repositories.AdminRepository;
import it.vszdev.gedosanapi.repositories.LogModificaPrenotazioneRepository;
import it.vszdev.gedosanapi.repositories.PrenotazioneRepository;
import it.vszdev.gedosanapi.repositories.SlotGiornalieroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AdminPrenotazioneService {

    private final PrenotazioneRepository prenotazioneRepository;
    private final SlotGiornalieroRepository slotGiornalieroRepository;
    private final LogModificaPrenotazioneRepository logModificaPrenotazioneRepository;
    private final AdminRepository adminRepository;
    private final PrenotazioneService prenotazioneService;
    private final PrenotazionePdfService prenotazionePdfService;
    private final TrasfusionaleService trasfusionaleService;
    private final DonatoreService donatoreService;

    public AdminPrenotazioneService(PrenotazioneRepository prenotazioneRepository,
                                     SlotGiornalieroRepository slotGiornalieroRepository,
                                     LogModificaPrenotazioneRepository logModificaPrenotazioneRepository,
                                     AdminRepository adminRepository,
                                     PrenotazioneService prenotazioneService,
                                     PrenotazionePdfService prenotazionePdfService,
                                     TrasfusionaleService trasfusionaleService,
                                     DonatoreService donatoreService) {
        this.prenotazioneRepository = prenotazioneRepository;
        this.slotGiornalieroRepository = slotGiornalieroRepository;
        this.logModificaPrenotazioneRepository = logModificaPrenotazioneRepository;
        this.adminRepository = adminRepository;
        this.prenotazioneService = prenotazioneService;
        this.prenotazionePdfService = prenotazionePdfService;
        this.trasfusionaleService = trasfusionaleService;
        this.donatoreService = donatoreService;
    }

    @Transactional(readOnly = true)
    public List<PrenotazioneAdminResponse> elenco(Long idTrasfusionale, LocalDate data) {
        return prenotazioneRepository
                .findAllBySlotGiornaliero_Trasfusionale_IdAndSlotGiornaliero_DataOrderBySlotGiornaliero_Orario_OrarioAsc(idTrasfusionale, data)
                .stream()
                .map(PrenotazioneAdminResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public PrenotazioneAdminResponse dettaglio(Long id) {
        return new PrenotazioneAdminResponse(trovaPrenotazione(id));
    }

    @Transactional
    public PrenotazioneAdminResponse crea(PrenotazioneRequest request) {
        return new PrenotazioneAdminResponse(prenotazioneService.creaPrenotazione(request, true));
    }

    @Transactional
    public PrenotazioneAdminResponse riprogramma(Long id, Long idSlotNuovo, Long idAdmin) {
        Prenotazione prenotazione = trovaPrenotazione(id);

        Long idSlotVecchio = prenotazione.getSlotGiornaliero().getId();

        if (idSlotVecchio.equals(idSlotNuovo)) {
            return new PrenotazioneAdminResponse(prenotazione);
        }

        // Ordine di lock: prima il donatore, poi gli slot (stesso ordine del flusso di creazione),
        // così una riprogrammazione e una prenotazione concorrenti sullo stesso donatore non aggirano
        // il controllo dell'intervallo minimo.
        donatoreService.bloccaPerPrenotazione(prenotazione.getDonatore().getId());

        // Lock in ordine di id crescente: evita il deadlock tra due riprogrammazioni che scambiano gli stessi slot in direzioni opposte.
        Long idPrimo = Math.min(idSlotVecchio, idSlotNuovo);
        Long idSecondo = Math.max(idSlotVecchio, idSlotNuovo);

        SlotGiornaliero slotPrimo = bloccaSlot(idPrimo);
        SlotGiornaliero slotSecondo = bloccaSlot(idSecondo);

        SlotGiornaliero slotVecchio = idSlotVecchio.equals(idPrimo) ? slotPrimo : slotSecondo;
        SlotGiornaliero slotNuovo = idSlotNuovo.equals(idPrimo) ? slotPrimo : slotSecondo;

        // Una riprogrammazione è "stesso centro, altra data/ora": lo spostamento tra centri diversi
        // si fa annullando e ricreando la prenotazione.
        if (!slotNuovo.getTrasfusionale().getId().equals(slotVecchio.getTrasfusionale().getId())) {
            throw new RegolaPrenotazioneException("La riprogrammazione deve restare nello stesso centro trasfusionale. "
                    + "Per spostare la donazione in un altro centro annulla la prenotazione e creane una nuova.");
        }

        Donatore donatore = prenotazione.getDonatore();
        prenotazioneService.ricalcolaPostiOccupati(slotNuovo);
        prenotazioneService.validaSlotPrenotabile(slotNuovo, donatore, prenotazione.getId(), true);

        prenotazione.setSlotGiornaliero(slotNuovo);
        prenotazioneService.ricalcolaPostiOccupati(slotVecchio);
        prenotazioneService.ricalcolaPostiOccupati(slotNuovo);

        registraLog(idAdmin, AzioneAdmin.RIPROGRAMMAZIONE, prenotazione, idSlotVecchio, idSlotNuovo);

        return new PrenotazioneAdminResponse(prenotazione);
    }

    @Transactional
    public void elimina(Long id, Long idAdmin) {
        Prenotazione prenotazione = trovaPrenotazione(id);

        SlotGiornaliero slot = bloccaSlot(prenotazione.getSlotGiornaliero().getId());

        registraLog(idAdmin, AzioneAdmin.CANCELLAZIONE, prenotazione, slot.getId(), null);

        prenotazioneRepository.delete(prenotazione);
        prenotazioneService.ricalcolaPostiOccupati(slot);
    }

    @Transactional(readOnly = true)
    public byte[] esportaPdf(Long idTrasfusionale, LocalDate data) {
        String nomeTrasfusionale = trasfusionaleService.trova(idTrasfusionale).getNome();
        List<PrenotazioneAdminResponse> prenotazioni = elenco(idTrasfusionale, data);
        return prenotazionePdfService.generaPdf(nomeTrasfusionale, data, prenotazioni);
    }

    private Prenotazione trovaPrenotazione(Long id) {
        return prenotazioneRepository.findById(id)
                .orElseThrow(() -> new RisorsaNonTrovataException("Prenotazione non trovata"));
    }

    private SlotGiornaliero bloccaSlot(Long id) {
        return slotGiornalieroRepository.findWithLockById(id)
                .orElseThrow(() -> new RisorsaNonTrovataException("Slot non trovato"));
    }

    private void registraLog(Long idAdmin, AzioneAdmin azione, Prenotazione prenotazione,
                             Long idSlotVecchio, Long idSlotNuovo) {
        Donatore donatore = prenotazione.getDonatore();
        logModificaPrenotazioneRepository.save(new LogModificaPrenotazione(
                trovaAdmin(idAdmin), azione, prenotazione.getId(),
                idSlotVecchio, idSlotNuovo, donatore.getNome(), donatore.getCognome()));
    }

    private Admin trovaAdmin(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new RisorsaNonTrovataException("Admin non trovato"));
    }
}
