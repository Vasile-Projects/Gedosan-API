package it.vszdev.gedosanapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "prenotazioni")
public class PrenotazioniProperties {

    private int orizzonteGiorni;
    private int etaMinima;
    private int intervalloUominiGiorni;
    private int intervalloDonneGiorni;
    private int distanzaMinimaDonneGiorni;
    private int postiPerSlot;

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

    public int getIntervalloUominiGiorni() {
        return intervalloUominiGiorni;
    }

    public void setIntervalloUominiGiorni(int intervalloUominiGiorni) {
        this.intervalloUominiGiorni = intervalloUominiGiorni;
    }

    public int getIntervalloDonneGiorni() {
        return intervalloDonneGiorni;
    }

    public void setIntervalloDonneGiorni(int intervalloDonneGiorni) {
        this.intervalloDonneGiorni = intervalloDonneGiorni;
    }

    public int getDistanzaMinimaDonneGiorni() {
        return distanzaMinimaDonneGiorni;
    }

    public void setDistanzaMinimaDonneGiorni(int distanzaMinimaDonneGiorni) {
        this.distanzaMinimaDonneGiorni = distanzaMinimaDonneGiorni;
    }

    public int getPostiPerSlot() {
        return postiPerSlot;
    }

    public void setPostiPerSlot(int postiPerSlot) {
        this.postiPerSlot = postiPerSlot;
    }
}
