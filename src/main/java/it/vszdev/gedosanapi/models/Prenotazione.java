package it.vszdev.gedosanapi.models;

import it.vszdev.gedosanapi.enums.TipoDonazione;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;


@Entity
@Table(name = "prenotazioni")
public class Prenotazione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_donatore", nullable = false)
    private Donatore donatore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_slot", nullable = false)
    private SlotGiornaliero slotGiornaliero;

    @Column(name= "tipo_donazione", nullable = false, length = 2)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.CHAR)
    private TipoDonazione tipoDonazione;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    public Prenotazione() {
    }

    public Prenotazione(Donatore donatore, SlotGiornaliero slotGiornaliero, TipoDonazione tipoDonazione) {
        this.donatore = donatore;
        this.slotGiornaliero = slotGiornaliero;
        this.tipoDonazione = tipoDonazione;
    }

    public Long getId() {
        return id;
    }

    public Donatore getDonatore() {
        return donatore;
    }

    public void setDonatore(Donatore donatore) {
        this.donatore = donatore;
    }

    public TipoDonazione getTipoDonazione() {
        return tipoDonazione;
    }

    public void setTipoDonazione(TipoDonazione tipoDonazione) {
        this.tipoDonazione = tipoDonazione;
    }

    public SlotGiornaliero getSlotGiornaliero() {
        return slotGiornaliero;
    }

    public void setSlotGiornaliero(SlotGiornaliero slotGiornaliero) {
        this.slotGiornaliero = slotGiornaliero;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
