package it.vszdev.gedosanapi.models;

import it.vszdev.gedosanapi.enums.EsitoInvioEmail;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_email")
public class LogEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "id_prenotazione", nullable = false)
    private Long idPrenotazione;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "esito", nullable = false, length = 3)
    private EsitoInvioEmail esito;

    @Column(name = "messaggio_errore", length = 500)
    private String messaggioErrore;

    @Column(name = "timestamp", nullable = false, insertable = false, updatable = false)
    private LocalDateTime timestamp;

    public LogEmail() {
    }

    public LogEmail(Long idPrenotazione, EsitoInvioEmail esito, String messaggioErrore) {
        this.idPrenotazione = idPrenotazione;
        this.esito = esito;
        this.messaggioErrore = messaggioErrore;
    }

    public Long getId() {
        return id;
    }

    public Long getIdPrenotazione() {
        return idPrenotazione;
    }

    public EsitoInvioEmail getEsito() {
        return esito;
    }

    public String getMessaggioErrore() {
        return messaggioErrore;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}