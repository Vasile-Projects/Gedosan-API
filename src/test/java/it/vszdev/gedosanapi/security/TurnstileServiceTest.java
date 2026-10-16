package it.vszdev.gedosanapi.security;

import it.vszdev.gedosanapi.exception.RichiestaNonValidaException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TurnstileServiceTest {

    @Test
    void disattivato_nonVerificaNulla() {
        assertThatCode(() -> new TurnstileService(false, "").verifica(null)).doesNotThrowAnyException();
    }

    @Test
    void attivoSenzaSecret_bloccaLAvvio() {
        assertThatThrownBy(() -> new TurnstileService(true, " ")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void attivoSenzaToken_rifiutaSenzaChiamareCloudflare() {
        TurnstileService service = new TurnstileService(true, "segreto");
        assertThatThrownBy(() -> service.verifica(null)).isInstanceOf(RichiestaNonValidaException.class);
        assertThatThrownBy(() -> service.verifica("  ")).isInstanceOf(RichiestaNonValidaException.class);
    }
}
