package it.vszdev.gedosanapi.validation;

import it.vszdev.gedosanapi.dto.PrenotazioneRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class CoerenzaCodiceFiscaleValidator implements ConstraintValidator<CoerenzaCodiceFiscale, PrenotazioneRequest> {

    @Override
    public boolean isValid(PrenotazioneRequest request, ConstraintValidatorContext context) {
        if (request == null || request.dataNascita() == null || request.sesso() == null || request.codiceFiscale() == null) {
            return true;
        }

        String cf = CodiceFiscaleUtil.normalizza(request.codiceFiscale());
        if (!CodiceFiscaleUtil.formatoValido(cf)) {
            return true; // formato già segnalato da @CodiceFiscale sul campo
        }

        CfDecodificato decodificato = CfDecodificato.da(cf);
        if (decodificato == null) {
            return true; // codice non standard, lasciato al controllo del checksum
        }

        LocalDate dataNascita = request.dataNascita();
        boolean coerente = decodificato.sesso() == request.sesso()
                && decodificato.mese() == dataNascita.getMonthValue()
                && decodificato.giorno() == dataNascita.getDayOfMonth()
                && decodificato.anno() == dataNascita.getYear() % 100;

        if (!coerente) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("codiceFiscale")
                    .addConstraintViolation();
        }

        return coerente;
    }
}
