package it.vszdev.gedosanapi.models;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "festivi_ricorrenti")
@Immutable
public class FestivoRicorrente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "mese", nullable = false)
    private Integer mese;

    @Column(name = "giorno", nullable = false)
    private Integer giorno;

    @Column(name = "descrizione")
    private String descrizione;

    public FestivoRicorrente() {
    }

    public FestivoRicorrente(Integer mese, Integer giorno, String descrizione) {
        this.mese = mese;
        this.giorno = giorno;
        this.descrizione = descrizione;
    }

    public Integer getId() {
        return id;
    }

    public Integer getMese() {
        return mese;
    }

    public Integer getGiorno() {
        return giorno;
    }

    public String getDescrizione() {
        return descrizione;
    }
}