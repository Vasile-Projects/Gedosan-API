package it.vszdev.gedosanapi.models;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "variazioni_apertura")
public class VariazioneApertura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_centro", nullable = false)
    private Trasfusionale trasfusionale;

    @Column(name = "data", nullable = false)
    private LocalDate dataVariazione;

    @Column(name = "aperto", nullable = false)
    private Boolean apertura;

    @Column(name = "motivo", length = 255)
    private String motivo;

    public VariazioneApertura() {
    }

    public VariazioneApertura(Trasfusionale trasfusionale, LocalDate dataVariazione, Boolean apertura, String motivo) {
        this.trasfusionale = trasfusionale;
        this.dataVariazione = dataVariazione;
        this.apertura = apertura;
        this.motivo = motivo;
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

    public LocalDate getDataVariazione() {
        return dataVariazione;
    }

    public void setDataVariazione(LocalDate dataVariazione) {
        this.dataVariazione = dataVariazione;
    }

    public Boolean getApertura() {
        return apertura;
    }

    public void setApertura(Boolean apertura) {
        this.apertura = apertura;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}