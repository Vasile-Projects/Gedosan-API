package it.vszdev.gedosanapi.models;


import jakarta.persistence.*;

@Entity
@Table(name = "trasfusionali")
public class Trasfusionale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column( nullable = false, length = 4)
    private String citta;

    @Column(nullable = false, length = 255)
    private String indirizzo;

    @Column(nullable = false)
    private Integer civico;

    @Column(nullable = false, unique = true, length = 10)
    private String telefono;

    public Trasfusionale() {
    }

    public Trasfusionale(String nome, String citta, String indirizzo, Integer civico, String telefono) {
        this.nome = nome;
        this.citta = citta;
        this.indirizzo = indirizzo;
        this.civico = civico;
        this.telefono = telefono;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCitta() {
        return citta;
    }

    public void setCitta(String citta) {
        this.citta = citta;
    }

    public String getIndirizzo() {
        return indirizzo;
    }

    public void setIndirizzo(String indirizzo) {
        this.indirizzo = indirizzo;
    }

    public Integer getCivico() {
        return civico;
    }

    public void setCivico(Integer civico) {
        this.civico = civico;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
}
