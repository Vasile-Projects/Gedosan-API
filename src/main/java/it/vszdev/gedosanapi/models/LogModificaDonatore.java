package it.vszdev.gedosanapi.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Audit delle modifiche all'anagrafica fatte dall'area admin: quali campi, non i valori, per non
// accumulare copie di dati personali.
@Entity
@Table(name = "log_modifiche_donatori")
public class LogModificaDonatore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_donatore", nullable = false)
    private Donatore donatore;

    @Column(name = "campi_modificati", nullable = false, length = 255)
    private String campiModificati;

    @Column(name = "timestamp", nullable = false, insertable = false, updatable = false)
    private LocalDateTime timestamp;

    public LogModificaDonatore() {
    }

    public LogModificaDonatore(Admin admin, Donatore donatore, String campiModificati) {
        this.admin = admin;
        this.donatore = donatore;
        this.campiModificati = campiModificati;
    }

    public Long getId() {
        return id;
    }

    public Admin getAdmin() {
        return admin;
    }

    public Donatore getDonatore() {
        return donatore;
    }

    public String getCampiModificati() {
        return campiModificati;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
