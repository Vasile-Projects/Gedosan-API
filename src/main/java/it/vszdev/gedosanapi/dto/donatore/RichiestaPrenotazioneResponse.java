package it.vszdev.gedosanapi.dto.donatore;

// Risposta alla richiesta pubblica di prenotazione: sempre la stessa, qualunque sia l'esito legato
// alla persona (nuova attesa, dati diversi da quelli registrati, regola non rispettata). I dettagli
// arrivano solo per email, cioè solo a chi possiede l'indirizzo.
public record RichiestaPrenotazioneResponse(String messaggio) {

    public static final RichiestaPrenotazioneResponse GENERICA = new RichiestaPrenotazioneResponse(
            "Se i dati sono corretti riceverai a breve un'email con il link per confermare la prenotazione. "
                    + "Se non arriva entro qualche minuto controlla lo spam o contatta il centro trasfusionale.");
}
