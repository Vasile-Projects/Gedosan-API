package it.vszdev.gedosanapi.repositories;

import it.vszdev.gedosanapi.models.LogEmail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogEmailRepository extends JpaRepository<LogEmail, Long> {
}
