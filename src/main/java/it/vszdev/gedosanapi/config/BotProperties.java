package it.vszdev.gedosanapi.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

// Parametri dell'integrazione con il bot Telegram (workflow n8n).
@Component
@ConfigurationProperties(prefix = "bot")
@Validated
public class BotProperties {

    // Chiave condivisa con n8n, inviata nell'header X-Bot-Key. Vuota = endpoint del bot disattivati.
    private String apiKey = "";

    @Min(1)
    private int giorniValiditaCollegamento;

    @Min(1)
    private int minutiValiditaCodice;

    @Min(1)
    private int maxTentativiCodice;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getGiorniValiditaCollegamento() {
        return giorniValiditaCollegamento;
    }

    public void setGiorniValiditaCollegamento(int giorniValiditaCollegamento) {
        this.giorniValiditaCollegamento = giorniValiditaCollegamento;
    }

    public int getMinutiValiditaCodice() {
        return minutiValiditaCodice;
    }

    public void setMinutiValiditaCodice(int minutiValiditaCodice) {
        this.minutiValiditaCodice = minutiValiditaCodice;
    }

    public int getMaxTentativiCodice() {
        return maxTentativiCodice;
    }

    public void setMaxTentativiCodice(int maxTentativiCodice) {
        this.maxTentativiCodice = maxTentativiCodice;
    }
}
