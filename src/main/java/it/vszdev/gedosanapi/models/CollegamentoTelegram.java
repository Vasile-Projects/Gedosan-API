package it.vszdev.gedosanapi.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Chat Telegram collegata a un donatore dopo la verifica del codice. Fino alla scadenza i messaggi da
// questa chat valgono come provenienti dal donatore: il chatId lo fornisce Telegram, non l'utente.
@Entity
@Table(name = "collegamenti_telegram")
public class CollegamentoTelegram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false, unique = true)
    private Long chatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_donatore", nullable = false)
    private Donatore donatore;

    @Column(nullable = false)
    private LocalDateTime scadenza;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public CollegamentoTelegram() {
    }

    public CollegamentoTelegram(Long chatId, Donatore donatore, LocalDateTime scadenza) {
        this.chatId = chatId;
        this.donatore = donatore;
        this.scadenza = scadenza;
    }

    public Long getId() {
        return id;
    }

    public Long getChatId() {
        return chatId;
    }

    public Donatore getDonatore() {
        return donatore;
    }

    public LocalDateTime getScadenza() {
        return scadenza;
    }
}
