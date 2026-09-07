package it.vszdev.gedosanapi.repositories;

import it.vszdev.gedosanapi.models.Donatore;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface DonatoreRepository extends JpaRepository<Donatore, Long> {

    Optional<Donatore> findByEmail(String email);

    Optional<Donatore> findByCodiceFiscale(String codiceFiscale);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Donatore> findWithLockById(Long id);
}
