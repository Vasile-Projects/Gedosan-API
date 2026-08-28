package it.vszdev.gedosanapi.repositories;

import it.vszdev.gedosanapi.models.Prenotazione;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PrenotazioneRepository extends JpaRepository<Prenotazione, Long> {

    Optional<Prenotazione> findFirstByDonatore_IdOrderBySlotGiornaliero_DataDesc(Long idDonatore);

    Optional<Prenotazione> findFirstByDonatore_IdAndIdNotOrderBySlotGiornaliero_DataDesc(Long idDonatore, Long idEscluso);

    List<Prenotazione> findAllBySlotGiornaliero_Trasfusionale_IdAndSlotGiornaliero_DataOrderBySlotGiornaliero_Orario_OrarioAsc(
            Long idTrasfusionale, LocalDate data);
}
