package it.vszdev.gedosanapi.repositories;

import it.vszdev.gedosanapi.models.SlotGiornaliero;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

public interface SlotGiornalieroRepository extends JpaRepository<SlotGiornaliero, Long> {

    Optional<SlotGiornaliero> findByTrasfusionale_IdAndOrario_OrarioAndData(Long idTrasfusionale, LocalTime orario, LocalDate data);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SlotGiornaliero> findWithLockById(Long id);

    long deleteByDataBeforeAndPostiOccupati(LocalDate data, int postiOccupati);
}
