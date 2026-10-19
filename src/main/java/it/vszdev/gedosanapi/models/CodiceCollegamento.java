package it.vszdev.gedosanapi.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Codice monouso inviato all'email registrata del donatore per collegare una chat Telegram. Esiste solo
// se CF ed email corrispondevano a un donatore: chi chiede il codice riceve comunque la stessa risposta.
@Entity
@Table(name = "codici_collegamento")
public class CodiceCollegamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false, unique = true)
    private Long chatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_donatore", nullable = false)
    private Donatore donatore;

    // SHA-256 esadecimale del codice: in chiaro esiste solo nell'email.
    @Column(name = "codice_hash", nullable = false, length = 64)
    private String codiceHash;

    @Column(nullable = false)
    private int tentativi;

    @Column(nullable = false)
    private LocalDateTime scadenza;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public CodiceCollegamento() {
    }

    public CodiceCollegamento(Long chatId, Donatore donatore, String codiceHash, LocalDateTime scadenza) {
        this.chatId = chatId;
        this.donatore = donatore;
        this.codiceHash = codiceHash;
        this.scadenza = scadenza;
    }

    public boolean scadutoAl(LocalDateTime adesso) {
        return !scadenza.isAfter(adesso);
    }

    public void registraTentativoErrato() {
        tentativi++;
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

    public String getCodiceHash() {
        return codiceHash;
    }

    public int getTentativi() {
        return tentativi;
    }

    public LocalDateTime getScadenza() {
        return scadenza;
    }
}
