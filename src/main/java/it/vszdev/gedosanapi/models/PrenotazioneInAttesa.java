package it.vszdev.gedosanapi.models;

import it.vszdev.gedosanapi.enums.Sesso;
import it.vszdev.gedosanapi.enums.TipoDonazione;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Prenotazione richiesta dal modulo pubblico ma non ancora confermata dal link email. Tiene una copia
// dei dati inviati e occupa il posto sullo slot fino a `scadenza`: donatore e prenotazione vengono
// creati solo alla conferma, da chi dimostra di possedere l'email.
@Entity
@Table(name = "prenotazioni_in_attesa")
public class PrenotazioneInAttesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_slot", nullable = false)
    private SlotGiornaliero slotGiornaliero;

    @Column(nullable = false, length = 60)
    private String nome;

    @Column(nullable = false, length = 120)
    private String cognome;

    @Column(name = "data_nascita", nullable = false)
    private LocalDate dataNascita;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 1)
    private Sesso sesso;

    @Column(name = "codice_fiscale", nullable = false, unique = true, length = 16)
    private String codiceFiscale;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 15)
    private String cellulare;

    @Column(name = "tipo_donazione", nullable = false, length = 2)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.CHAR)
    private TipoDonazione tipoDonazione;

    // SHA-256 esadecimale del token inviato per email: il token in chiaro non viene mai salvato.
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime scadenza;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public PrenotazioneInAttesa() {
    }

    public PrenotazioneInAttesa(SlotGiornaliero slotGiornaliero, String nome, String cognome, LocalDate dataNascita,
                                Sesso sesso, String codiceFiscale, String email, String cellulare,
                                TipoDonazione tipoDonazione, String tokenHash, LocalDateTime scadenza) {
        this.slotGiornaliero = slotGiornaliero;
        this.nome = nome;
        this.cognome = cognome;
        this.dataNascita = dataNascita;
        this.sesso = sesso;
        this.codiceFiscale = codiceFiscale;
        this.email = email;
        this.cellulare = cellulare;
        this.tipoDonazione = tipoDonazione;
        this.tokenHash = tokenHash;
        this.scadenza = scadenza;
    }

    // Confine escluso: allo scoccare della scadenza l'attesa non vale più (coerente con il conteggio dei
    // posti, che considera solo le attese con scadenza successiva ad adesso).
    public boolean scadutaAl(LocalDateTime adesso) {
        return !scadenza.isAfter(adesso);
    }

    public Long getId() {
        return id;
    }

    public SlotGiornaliero getSlotGiornaliero() {
        return slotGiornaliero;
    }

    public String getNome() {
        return nome;
    }

    public String getCognome() {
        return cognome;
    }

    public LocalDate getDataNascita() {
        return dataNascita;
    }

    public Sesso getSesso() {
        return sesso;
    }

    public String getCodiceFiscale() {
        return codiceFiscale;
    }

    public String getEmail() {
        return email;
    }

    public String getCellulare() {
        return cellulare;
    }

    public TipoDonazione getTipoDonazione() {
        return tipoDonazione;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getScadenza() {
        return scadenza;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Stessa normalizzazione di Donatore: il confronto CF/email alla conferma avviene su valori omogenei.
    @PrePersist
    @PreUpdate
    private void normalizza() {
        email = Donatore.normalizzaEmail(email);
        codiceFiscale = Donatore.normalizzaCodiceFiscale(codiceFiscale);
    }
}
