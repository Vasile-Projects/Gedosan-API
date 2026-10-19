package it.vszdev.gedosanapi.repositories;

import it.vszdev.gedosanapi.models.CollegamentoTelegram;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CollegamentoTelegramRepository extends JpaRepository<CollegamentoTelegram, Long> {

    @EntityGraph(attributePaths = "donatore")
    Optional<CollegamentoTelegram> findByChatIdAndScadenzaAfter(Long chatId, LocalDateTime adesso);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM CollegamentoTelegram c WHERE c.chatId = :chatId")
    int eliminaPerChat(Long chatId);

    @Modifying
    @Query("DELETE FROM CollegamentoTelegram c WHERE c.scadenza <= :adesso")
    int eliminaScaduti(LocalDateTime adesso);
}
