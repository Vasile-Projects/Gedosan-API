package it.vszdev.gedosanapi.service;

import it.vszdev.gedosanapi.dto.admin.LogModificaPrenotazioneResponse;
import it.vszdev.gedosanapi.repositories.LogModificaPrenotazioneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LogModificaPrenotazioneService {

    private final LogModificaPrenotazioneRepository logModificaPrenotazioneRepository;

    public LogModificaPrenotazioneService(LogModificaPrenotazioneRepository logModificaPrenotazioneRepository) {
        this.logModificaPrenotazioneRepository = logModificaPrenotazioneRepository;
    }

    @Transactional(readOnly = true)
    public List<LogModificaPrenotazioneResponse> elenco() {
        return logModificaPrenotazioneRepository.findAllByOrderByTimestampDesc().stream()
                .map(LogModificaPrenotazioneResponse::new)
                .toList();
    }
}
