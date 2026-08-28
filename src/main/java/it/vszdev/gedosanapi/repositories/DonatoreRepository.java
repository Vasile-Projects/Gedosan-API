package it.vszdev.gedosanapi.repositories;

import it.vszdev.gedosanapi.models.Donatore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DonatoreRepository extends JpaRepository<Donatore, Long> {

    Optional<Donatore> findByEmail(String email);

    Optional<Donatore> findByCodiceFiscale(String codiceFiscale);
}
