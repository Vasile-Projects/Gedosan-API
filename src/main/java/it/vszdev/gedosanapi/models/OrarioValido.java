package it.vszdev.gedosanapi.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalTime;

@Entity
@Table(name = "orari_validi")
public class OrarioValido {

    @Id
    @Column(name = "ora", nullable = false, updatable = false)
    private LocalTime orario;

    public OrarioValido() {
    }

    public OrarioValido(LocalTime orario) {
        this.orario = orario;
    }

    public LocalTime getOrario() {
        return orario;
    }
}
