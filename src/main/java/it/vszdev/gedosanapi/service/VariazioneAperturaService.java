package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.dto.admin.VariazioneAperturaRequest;
import it.vszdev.gedosanapi.dto.admin.VariazioneAperturaResponse;
import it.vszdev.gedosanapi.exception.RisorsaNonTrovataException;
import it.vszdev.gedosanapi.models.Trasfusionale;
import it.vszdev.gedosanapi.models.VariazioneApertura;
import it.vszdev.gedosanapi.repositories.VariazioneAperturaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VariazioneAperturaService {

    private final VariazioneAperturaRepository variazioneAperturaRepository;
    private final TrasfusionaleService trasfusionaleService;

    public VariazioneAperturaService(VariazioneAperturaRepository variazioneAperturaRepository,
                                      TrasfusionaleService trasfusionaleService) {
        this.variazioneAperturaRepository = variazioneAperturaRepository;
        this.trasfusionaleService = trasfusionaleService;
    }

    @Transactional(readOnly = true)
    public List<VariazioneAperturaResponse> elenco(Long idTrasfusionale) {
        List<VariazioneApertura> variazioni = idTrasfusionale == null
                ? variazioneAperturaRepository.findAllByOrderByDataVariazioneDesc()
                : variazioneAperturaRepository.findAllByTrasfusionale_IdOrderByDataVariazioneDesc(idTrasfusionale);

        return variazioni.stream()
                .map(VariazioneAperturaResponse::new)
                .toList();
    }

    // Scelta: nessun controllo di conflitto con le prenotazioni del giorno chiuso, l'admin le gestisce a mano.
    @Transactional
    public List<VariazioneAperturaResponse> crea(VariazioneAperturaRequest request) {
        return request.idTrasfusionali().stream()
                .map(idTrasfusionale -> {
                    Trasfusionale trasfusionale = trasfusionaleService.trova(idTrasfusionale);
                    VariazioneApertura variazione = new VariazioneApertura(
                            trasfusionale, request.dataVariazione(), request.apertura(), request.motivo());
                    return new VariazioneAperturaResponse(variazioneAperturaRepository.save(variazione));
                })
                .toList();
    }

    @Transactional
    public void elimina(Long id) {
        if (!variazioneAperturaRepository.existsById(id)) {
            throw new RisorsaNonTrovataException("Variazione di apertura non trovata");
        }
        variazioneAperturaRepository.deleteById(id);
    }
}
