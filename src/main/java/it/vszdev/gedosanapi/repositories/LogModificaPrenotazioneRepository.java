package it.vszdev.gedosanapi.repositories;

import it.vszdev.gedosanapi.models.LogModificaPrenotazione;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogModificaPrenotazioneRepository extends JpaRepository<LogModificaPrenotazione, Long> {

    List<LogModificaPrenotazione> findAllByOrderByTimestampDesc();
}
