package it.vszdev.gedosanapi.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 60)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "nome", nullable = false, length = 60)
    private String nome;

    @Column(name = "cognome", nullable = false, length = 60)
    private String cognome;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Admin() {
    }

    public Admin(String username, String passwordHash, String nome, String cognome) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.nome = nome;
        this.cognome = cognome;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}