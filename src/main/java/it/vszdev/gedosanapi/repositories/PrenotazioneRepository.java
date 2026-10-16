package it.vszdev.gedosanapi.repositories;

import it.vszdev.gedosanapi.models.Prenotazione;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PrenotazioneRepository extends JpaRepository<Prenotazione, Long> {

    Optional<Prenotazione> findFirstByDonatore_IdOrderBySlotGiornaliero_DataDesc(Long idDonatore);

    Optional<Prenotazione> findFirstByDonatore_IdAndIdNotOrderBySlotGiornaliero_DataDesc(Long idDonatore, Long idEscluso);

    // Donazioni del donatore con data compresa nell'intervallo. Usato per il limite di donazioni
    // per finestra mobile delle donatrici. La variante AndIdNot esclude la prenotazione spostata
    // in una riprogrammazione, che altrimenti conterebbe se stessa.
    long countByDonatore_IdAndSlotGiornaliero_DataBetween(Long idDonatore, LocalDate da, LocalDate a);

    long countByDonatore_IdAndIdNotAndSlotGiornaliero_DataBetween(Long idDonatore, Long idEscluso, LocalDate da, LocalDate a);

    // Numero reale di prenotazioni su uno slot: fonte autoritativa per ricalcolare posti_occupati.
    long countBySlotGiornaliero_Id(Long idSlot);

    // Prenotazioni con tutte le associazioni necessarie a costruire il payload dell'email di conferma
    // fuori da una transazione (usato dal job di ritentativo).
    @EntityGraph(attributePaths = {"donatore", "slotGiornaliero.trasfusionale", "slotGiornaliero.orario"})
    List<Prenotazione> findByIdIn(Collection<Long> ids);

    // Storico delle prenotazioni di un donatore per il dettaglio admin, dalla più recente.
    @EntityGraph(attributePaths = {"slotGiornaliero.trasfusionale", "slotGiornaliero.orario"})
    List<Prenotazione> findAllByDonatore_IdOrderBySlotGiornaliero_DataDesc(Long idDonatore);

    List<Prenotazione> findAllBySlotGiornaliero_Trasfusionale_IdAndSlotGiornaliero_DataOrderBySlotGiornaliero_Orario_OrarioAsc(
            Long idTrasfusionale, LocalDate data);
}
