package it.vszdev.gedosanapi.models;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "slot_giornalieri")
public class SlotGiornaliero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_centro", nullable = false)
    private Trasfusionale trasfusionale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ora", nullable = false)
    private OrarioValido orario;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private Integer postiOccupati;

    public SlotGiornaliero() {
    }

    public SlotGiornaliero(Trasfusionale trasfusionale, OrarioValido orario, LocalDate data, Integer postiOccupati) {
        this.trasfusionale = trasfusionale;
        this.orario = orario;
        this.data = data;
        this.postiOccupati = postiOccupati;
    }

    public Long getId() {
        return id;
    }

    public Trasfusionale getTrasfusionale() {
        return trasfusionale;
    }

    public void setTrasfusionale(Trasfusionale trasfusionale) {
        this.trasfusionale = trasfusionale;
    }

    public OrarioValido getOrario() {
        return orario;
    }

    public void setOrario(OrarioValido orario) {
        this.orario = orario;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public Integer getPostiOccupati() {
        return postiOccupati;
    }

    public void setPostiOccupati(Integer postiOccupati) {
        this.postiOccupati = postiOccupati;
    }
}