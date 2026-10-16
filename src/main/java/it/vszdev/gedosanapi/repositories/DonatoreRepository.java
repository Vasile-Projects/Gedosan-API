package it.vszdev.gedosanapi.repositories;

import it.vszdev.gedosanapi.models.Donatore;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface DonatoreRepository extends JpaRepository<Donatore, Long> {

    Optional<Donatore> findByEmail(String email);

    Optional<Donatore> findByCodiceFiscale(String codiceFiscale);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Donatore> findWithLockById(Long id);

    // Ricerca dell'area admin per cognome (prefisso). Limitata: è uno strumento di ricerca puntuale,
    // non un modo per scaricare l'anagrafica completa.
    List<Donatore> findTop50ByCognomeStartingWithIgnoreCaseOrderByCognomeAscNomeAsc(String prefissoCognome);
}
