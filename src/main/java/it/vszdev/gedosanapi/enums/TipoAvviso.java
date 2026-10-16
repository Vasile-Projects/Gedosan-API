package it.vszdev.gedosanapi.enums;

// Esiti di una richiesta di prenotazione che non generano un link di conferma e vengono comunicati
// solo via email, al proprietario dell'indirizzo registrato.
public enum TipoAvviso {
    DATI_DIVERSI,          // CF registrato con un'email diversa da quella inserita
    EMAIL_IN_USO,          // email inserita già associata a un altro CF
    REGOLA_NON_RISPETTATA  // intervallo minimo, limite annuo, prenotazione già esistente
}
