package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.dto.admin.DonatoreAdminResponse;
import it.vszdev.gedosanapi.dto.admin.DonatoreDettaglioResponse;
import it.vszdev.gedosanapi.dto.admin.ModificaDonatoreRequest;
import it.vszdev.gedosanapi.exception.RegolaPrenotazioneException;
import it.vszdev.gedosanapi.exception.RichiestaNonValidaException;
import it.vszdev.gedosanapi.exception.RisorsaNonTrovataException;
import it.vszdev.gedosanapi.models.Admin;
import it.vszdev.gedosanapi.models.Donatore;
import it.vszdev.gedosanapi.models.LogModificaDonatore;
import it.vszdev.gedosanapi.repositories.AdminRepository;
import it.vszdev.gedosanapi.repositories.DonatoreRepository;
import it.vszdev.gedosanapi.repositories.LogModificaDonatoreRepository;
import it.vszdev.gedosanapi.repositories.PrenotazioneRepository;
import it.vszdev.gedosanapi.validation.CodiceFiscaleUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Gestione dell'anagrafica dei donatori da parte del centro trasfusionale. È l'unico modo per
// cambiare i dati di un donatore esistente: il flusso pubblico non li aggiorna mai.
@Service
public class AdminDonatoreService {

    private static final int LUNGHEZZA_MINIMA_RICERCA = 2;

    private final DonatoreRepository donatoreRepository;
    private final PrenotazioneRepository prenotazioneRepository;
    private final LogModificaDonatoreRepository logModificaDonatoreRepository;
    private final AdminRepository adminRepository;
    private final DonatoreService donatoreService;

    public AdminDonatoreService(DonatoreRepository donatoreRepository,
                                PrenotazioneRepository prenotazioneRepository,
                                LogModificaDonatoreRepository logModificaDonatoreRepository,
                                AdminRepository adminRepository,
                                DonatoreService donatoreService) {
        this.donatoreRepository = donatoreRepository;
        this.prenotazioneRepository = prenotazioneRepository;
        this.logModificaDonatoreRepository = logModificaDonatoreRepository;
        this.adminRepository = adminRepository;
        this.donatoreService = donatoreService;
    }

    // Un testo con la @ cerca per email esatta, 16 caratteri alfanumerici per codice fiscale esatto,
    // altrimenti per prefisso del cognome (massimo 50 risultati).
    @Transactional(readOnly = true)
    public List<DonatoreAdminResponse> cerca(String testo) {
        String q = testo == null ? "" : testo.trim();
        if (q.length() < LUNGHEZZA_MINIMA_RICERCA) {
            throw new RichiestaNonValidaException("Inserisci almeno " + LUNGHEZZA_MINIMA_RICERCA + " caratteri per la ricerca.");
        }

        if (q.contains("@")) {
            return donatoreRepository.findByEmail(Donatore.normalizzaEmail(q)).map(DonatoreAdminResponse::new).stream().toList();
        }
        String cf = CodiceFiscaleUtil.normalizza(q);
        if (CodiceFiscaleUtil.formatoValido(cf)) {
            return donatoreRepository.findByCodiceFiscale(cf).map(DonatoreAdminResponse::new).stream().toList();
        }
        return donatoreRepository.findTop50ByCognomeStartingWithIgnoreCaseOrderByCognomeAscNomeAsc(q).stream()
                .map(DonatoreAdminResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public DonatoreDettaglioResponse dettaglio(Long id) {
        Donatore donatore = trovaDonatore(id);
        return new DonatoreDettaglioResponse(
                new DonatoreAdminResponse(donatore),
                prenotazioneRepository.findAllByDonatore_IdOrderBySlotGiornaliero_DataDesc(id).stream()
                        .map(DonatoreDettaglioResponse.PrenotazioneDonatore::new)
                        .toList(),
                logModificaDonatoreRepository.findAllByDonatore_IdOrderByTimestampDescIdDesc(id).stream()
                        .map(DonatoreDettaglioResponse.ModificaAnagrafica::new)
                        .toList());
    }

    // Sostituisce l'anagrafica con quella inviata (coerenza col CF già verificata sul DTO). Lock sul
    // donatore, come nei flussi di prenotazione, così una modifica non si intreccia con una conferma
    // o una prenotazione admin in corso. Unicità di CF ed email verificata qui per un messaggio chiaro;
    // i vincoli UNIQUE del DB restano l'arbitro finale.
    @Transactional
    public DonatoreAdminResponse modifica(Long id, ModificaDonatoreRequest request, Long idAdmin) {
        Donatore donatore = donatoreService.bloccaPerPrenotazione(id);

        String codiceFiscale = Donatore.normalizzaCodiceFiscale(request.codiceFiscale());
        String email = Donatore.normalizzaEmail(request.email());

        if (!codiceFiscale.equals(donatore.getCodiceFiscale()) && appartieneAdAltro(donatoreRepository.findByCodiceFiscale(codiceFiscale).orElse(null), id)) {
            throw new RegolaPrenotazioneException("Il codice fiscale inserito è già associato a un altro donatore.");
        }
        if (!email.equals(donatore.getEmail()) && appartieneAdAltro(donatoreRepository.findByEmail(email).orElse(null), id)) {
            throw new RegolaPrenotazioneException("L'email inserita è già associata a un altro donatore.");
        }

        List<String> campiModificati = new ArrayList<>();
        if (!Objects.equals(donatore.getNome(), request.nome())) {
            campiModificati.add("nome");
            donatore.setNome(request.nome());
        }
        if (!Objects.equals(donatore.getCognome(), request.cognome())) {
            campiModificati.add("cognome");
            donatore.setCognome(request.cognome());
        }
        if (!Objects.equals(donatore.getDataNascita(), request.dataNascita())) {
            campiModificati.add("dataNascita");
            donatore.setDataNascita(request.dataNascita());
        }
        if (donatore.getSesso() != request.sesso()) {
            campiModificati.add("sesso");
            donatore.setSesso(request.sesso());
        }
        if (!codiceFiscale.equals(donatore.getCodiceFiscale())) {
            campiModificati.add("codiceFiscale");
            donatore.setCodiceFiscale(codiceFiscale);
        }
        if (!email.equals(donatore.getEmail())) {
            campiModificati.add("email");
            donatore.setEmail(email);
        }
        if (!Objects.equals(donatore.getCellulare(), request.cellulare())) {
            campiModificati.add("cellulare");
            donatore.setCellulare(request.cellulare());
        }

        if (!campiModificati.isEmpty()) {
            donatoreRepository.save(donatore);
            logModificaDonatoreRepository.save(new LogModificaDonatore(trovaAdmin(idAdmin), donatore, String.join(",", campiModificati)));
        }

        return new DonatoreAdminResponse(donatore);
    }

    private boolean appartieneAdAltro(Donatore trovato, Long idDonatore) {
        return trovato != null && !trovato.getId().equals(idDonatore);
    }

    private Donatore trovaDonatore(Long id) {
        return donatoreRepository.findById(id)
                .orElseThrow(() -> new RisorsaNonTrovataException("Donatore non trovato"));
    }

    private Admin trovaAdmin(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new RisorsaNonTrovataException("Admin non trovato"));
    }
}
