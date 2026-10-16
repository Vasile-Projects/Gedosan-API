package it.vszdev.gedosanapi.dto.admin;

import it.vszdev.gedosanapi.enums.TipoDonazione;
import it.vszdev.gedosanapi.models.LogModificaDonatore;
import it.vszdev.gedosanapi.models.Prenotazione;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

// Dettaglio per l'area admin: anagrafica, storico delle prenotazioni (dalla più recente) e modifiche
// all'anagrafica fatte dagli operatori.
public record DonatoreDettaglioResponse(
        DonatoreAdminResponse donatore,
        List<PrenotazioneDonatore> prenotazioni,
        List<ModificaAnagrafica> modifiche
) {

    public record PrenotazioneDonatore(
            Long id,
            String nomeTrasfusionale,
            LocalDate data,
            LocalTime orario,
            TipoDonazione tipoDonazione
    ) {
        public PrenotazioneDonatore(Prenotazione p) {
            this(
                    p.getId(),
                    p.getSlotGiornaliero().getTrasfusionale().getNome(),
                    p.getSlotGiornaliero().getData(),
                    p.getSlotGiornaliero().getOrario().getOrario(),
                    p.getTipoDonazione()
            );
        }
    }

    public record ModificaAnagrafica(
            String campiModificati,
            String usernameAdmin,
            LocalDateTime timestamp
    ) {
        public ModificaAnagrafica(LogModificaDonatore l) {
            this(l.getCampiModificati(), l.getAdmin().getUsername(), l.getTimestamp());
        }
    }
}
