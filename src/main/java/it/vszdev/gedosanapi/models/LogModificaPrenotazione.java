package it.vszdev.gedosanapi.models;

import it.vszdev.gedosanapi.enums.AzioneAdmin;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_modifiche_prenotazioni")
public class LogModificaPrenotazione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "azione", nullable = false, length = 20)
    private AzioneAdmin azione;

    // Non è una FK: riferimento storico, deve sopravvivere alla cancellazione della prenotazione.
    @Column(name = "id_prenotazione", nullable = false)
    private Long idPrenotazione;

    @Column(name = "id_slot_vecchio", nullable = false)
    private Long idSlotVecchio;

    @Column(name = "id_slot_nuovo")
    private Long idSlotNuovo;

    @Column(name = "donatore_nome", length = 60)
    private String donatoreNome;

    @Column(name = "donatore_cognome", length = 120)
    private String donatoreCognome;

    @Column(name = "timestamp", nullable = false, insertable = false, updatable = false)
    private LocalDateTime timestamp;

    public LogModificaPrenotazione() {
    }

    public LogModificaPrenotazione(Admin admin, AzioneAdmin azione, Long idPrenotazione,
                                   Long idSlotVecchio, Long idSlotNuovo,
                                   String donatoreNome, String donatoreCognome) {
        this.admin = admin;
        this.azione = azione;
        this.idPrenotazione = idPrenotazione;
        this.idSlotVecchio = idSlotVecchio;
        this.idSlotNuovo = idSlotNuovo;
        this.donatoreNome = donatoreNome;
        this.donatoreCognome = donatoreCognome;
    }

    public Long getId() {
        return id;
    }

    public Admin getAdmin() {
        return admin;
    }

    public AzioneAdmin getAzione() {
        return azione;
    }

    public Long getIdPrenotazione() {
        return idPrenotazione;
    }

    public Long getIdSlotVecchio() {
        return idSlotVecchio;
    }

    public Long getIdSlotNuovo() {
        return idSlotNuovo;
    }

    public String getDonatoreNome() {
        return donatoreNome;
    }

    public String getDonatoreCognome() {
        return donatoreCognome;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}