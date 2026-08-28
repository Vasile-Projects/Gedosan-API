package it.vszdev.gedosanapi.models;

import it.vszdev.gedosanapi.enums.Sesso;
import it.vszdev.gedosanapi.validation.CodiceFiscale;
import it.vszdev.gedosanapi.validation.CodiceFiscaleUtil;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Locale;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "donatori")
public class Donatore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @CodiceFiscale
    @Column(name = "codice_fiscale", nullable = false, unique = true, length = 16)
    private String codiceFiscale;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 15)
    private String cellulare;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Donatore() {
    }

    public Donatore(String nome, String cognome, LocalDate dataNascita, Sesso sesso, String codiceFiscale, String cellulare, String email, LocalDateTime createdAt) {
        this.nome = nome;
        this.cognome = cognome;
        this.dataNascita = dataNascita;
        this.sesso = sesso;
        this.codiceFiscale = codiceFiscale;
        this.cellulare = cellulare;
        this.email = email;
        this.createdAt = createdAt;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDataNascita() {
        return dataNascita;
    }

    public void setDataNascita(LocalDate dataNascita) {
        this.dataNascita = dataNascita;
    }

    public Sesso getSesso() {
        return sesso;
    }

    public void setSesso(Sesso sesso) {
        this.sesso = sesso;
    }

    public String getCodiceFiscale() {
        return codiceFiscale;
    }

    public void setCodiceFiscale(String codiceFiscale) {
        this.codiceFiscale = codiceFiscale;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCellulare() {
        return cellulare;
    }

    public void setCellulare(String cellulare) {
        this.cellulare = cellulare;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    @PreUpdate
    private void normalizza() {
        email = normalizzaEmail(email);
        codiceFiscale = normalizzaCodiceFiscale(codiceFiscale);
    }

    public static String normalizzaEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizzaCodiceFiscale(String codiceFiscale) {
        return CodiceFiscaleUtil.normalizza(codiceFiscale);
    }
}
