package it.vszdev.gedosanapi.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "prenotazioni")
@Validated
public class PrenotazioniProperties {

    private int orizzonteGiorni;
    private int etaMinima;
    private int etaMassima;
    private int intervalloUominiGiorni;
    private int distanzaMinimaDonneGiorni;
    private int finestraLimiteDonneGiorni;
    private int maxDonazioniDonnePerFinestra;

    // Capienza massima di uno slot (non l'occupazione corrente, che può essere 0 quando lo slot è
    // vuoto). @Min(1): una capienza 0 renderebbe il portale incapace di accettare prenotazioni.
    // @Max(2): l'occupazione risultante non può superare il CHECK DB chk_slot_posti (0..2), altrimenti
    // il ricalcolo di posti_occupati fa fallire l'UPDATE a runtime. Un valore fuori range blocca l'avvio.
    @Min(1)
    @Max(2)
    private int postiPerSlot;
    private int anticipoMinimoMinuti;

    // Validità del link di conferma inviato per email: per tutto questo tempo la richiesta occupa il
    // posto sullo slot. @Min(1): con 0 nessuna richiesta sarebbe mai confermabile.
    @Min(1)
    private int minutiAttesaConferma;

    public int getOrizzonteGiorni() {
        return orizzonteGiorni;
    }

    public void setOrizzonteGiorni(int orizzonteGiorni) {
        this.orizzonteGiorni = orizzonteGiorni;
    }

    public int getEtaMinima() {
        return etaMinima;
    }

    public void setEtaMinima(int etaMinima) {
        this.etaMinima = etaMinima;
    }

    // Età massima (compiuta) alla data della donazione. Oltre questo valore la prenotazione va gestita
    // dal trasfusionale, che valuta caso per caso l'idoneità del donatore periodico.
    public int getEtaMassima() {
        return etaMassima;
    }

    public void setEtaMassima(int etaMassima) {
        this.etaMassima = etaMassima;
    }

    public int getIntervalloUominiGiorni() {
        return intervalloUominiGiorni;
    }

    public void setIntervalloUominiGiorni(int intervalloUominiGiorni) {
        this.intervalloUominiGiorni = intervalloUominiGiorni;
    }

    public int getDistanzaMinimaDonneGiorni() {
        return distanzaMinimaDonneGiorni;
    }

    public void setDistanzaMinimaDonneGiorni(int distanzaMinimaDonneGiorni) {
        this.distanzaMinimaDonneGiorni = distanzaMinimaDonneGiorni;
    }

    public int getFinestraLimiteDonneGiorni() {
        return finestraLimiteDonneGiorni;
    }

    public void setFinestraLimiteDonneGiorni(int finestraLimiteDonneGiorni) {
        this.finestraLimiteDonneGiorni = finestraLimiteDonneGiorni;
    }

    public int getMaxDonazioniDonnePerFinestra() {
        return maxDonazioniDonnePerFinestra;
    }

    public void setMaxDonazioniDonnePerFinestra(int maxDonazioniDonnePerFinestra) {
        this.maxDonazioniDonnePerFinestra = maxDonazioniDonnePerFinestra;
    }

    // Capienza massima di uno slot. Per portarla oltre 2 serve una migrazione Flyway che aggiorni
    // anche il vincolo DB chk_slot_posti e i @Max qui sopra; finché il CHECK resta [0, 2] il valore
    // è vincolato a [1, 2] e un valore fuori range blocca l'avvio.
    public int getPostiPerSlot() {
        return postiPerSlot;
    }

    public void setPostiPerSlot(int postiPerSlot) {
        this.postiPerSlot = postiPerSlot;
    }

    public int getAnticipoMinimoMinuti() {
        return anticipoMinimoMinuti;
    }

    public void setAnticipoMinimoMinuti(int anticipoMinimoMinuti) {
        this.anticipoMinimoMinuti = anticipoMinimoMinuti;
    }

    public int getMinutiAttesaConferma() {
        return minutiAttesaConferma;
    }

    public void setMinutiAttesaConferma(int minutiAttesaConferma) {
        this.minutiAttesaConferma = minutiAttesaConferma;
    }
}
