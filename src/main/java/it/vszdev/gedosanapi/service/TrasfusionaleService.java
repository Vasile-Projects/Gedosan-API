package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.dto.trasfusionale.TrasfusionaleResponse;
import it.vszdev.gedosanapi.exception.RisorsaNonTrovataException;
import it.vszdev.gedosanapi.models.Trasfusionale;
import it.vszdev.gedosanapi.repositories.TrasfusionaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TrasfusionaleService {

    private final TrasfusionaleRepository trasfusionaleRepository;

    public TrasfusionaleService(TrasfusionaleRepository trasfusionaleRepository) {
        this.trasfusionaleRepository = trasfusionaleRepository;
    }

    @Transactional(readOnly = true)
    public List<TrasfusionaleResponse> elenco() {
        return trasfusionaleRepository.findAll().stream()
                .map(TrasfusionaleResponse::new)
                .toList();
    }

    public Trasfusionale trova(Long id) {
        return trasfusionaleRepository.findById(id)
                .orElseThrow(() -> new RisorsaNonTrovataException("Centro trasfusionale non trovato"));
    }
}
