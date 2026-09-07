package it.vszdev.gedosanapi.repositories;

import it.vszdev.gedosanapi.models.LogEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LogEmailRepository extends JpaRepository<LogEmail, Long> {

    // Righe recenti, usate dal job di ritentativo per raggruppare gli esiti per prenotazione.
    List<LogEmail> findByTimestampAfter(LocalDateTime da);
}
