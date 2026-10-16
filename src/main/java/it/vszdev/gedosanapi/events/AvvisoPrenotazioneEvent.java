package it.vszdev.gedosanapi.events;

import it.vszdev.gedosanapi.enums.TipoAvviso;

// email e nome sono sempre quelli REGISTRATI del destinatario, mai quelli digitati nel modulo:
// chi conosce i dati di un altro non può far arrivare testo scelto da lui nella sua casella.
// dettaglio è il messaggio della regola violata (solo per REGOLA_NON_RISPETTATA).
public record AvvisoPrenotazioneEvent(
        String email,
        String nome,
        TipoAvviso tipo,
        String dettaglio
) {
}
