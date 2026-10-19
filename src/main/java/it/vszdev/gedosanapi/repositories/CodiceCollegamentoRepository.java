package it.vszdev.gedosanapi.repositories;

import it.vszdev.gedosanapi.models.CodiceCollegamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CodiceCollegamentoRepository extends JpaRepository<CodiceCollegamento, Long> {

    // Lock: due verifiche concorrenti dello stesso codice non contano i tentativi una volta sola.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CodiceCollegamento> findWithLockByChatId(Long chatId);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM CodiceCollegamento c WHERE c.chatId = :chatId")
    void eliminaPerChat(Long chatId);

    @Modifying
    @Query("DELETE FROM CodiceCollegamento c WHERE c.scadenza <= :adesso")
    int eliminaScaduti(LocalDateTime adesso);
}
