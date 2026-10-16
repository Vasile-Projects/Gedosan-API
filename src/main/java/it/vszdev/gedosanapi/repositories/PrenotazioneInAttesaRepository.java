package it.vszdev.gedosanapi.repositories;

import it.vszdev.gedosanapi.models.PrenotazioneInAttesa;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PrenotazioneInAttesaRepository extends JpaRepository<PrenotazioneInAttesa, Long> {

    // Lock sulla riga dell'attesa: due click concorrenti sullo stesso link non confermano due volte.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PrenotazioneInAttesa> findWithLockByTokenHash(String tokenHash);

    // Attesa già aperta per lo stesso CF (al massimo una, vincolo uq_attesa_codice_fiscale), bloccata
    // perché una nuova richiesta la sostituisce. Ordine dei lock ovunque: attesa -> donatore -> slot.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PrenotazioneInAttesa> findWithLockByCodiceFiscale(String codiceFiscale);

    // Attese che occupano ancora un posto sullo slot: concorrono a posti_occupati insieme alle confermate.
    long countBySlotGiornaliero_IdAndScadenzaAfter(Long idSlot, LocalDateTime adesso);

    // Attese scadute, da cancellare fisicamente dal job di scadenza. Bloccate (in ordine di id) prima
    // degli slot, rispettando lo stesso ordine dei lock dei flussi di richiesta e conferma.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<PrenotazioneInAttesa> findByScadenzaLessThanEqualOrderByIdAsc(LocalDateTime adesso);
}
