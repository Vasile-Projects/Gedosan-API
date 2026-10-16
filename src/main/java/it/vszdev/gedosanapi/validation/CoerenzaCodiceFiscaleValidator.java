package it.vszdev.gedosanapi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class CoerenzaCodiceFiscaleValidator implements ConstraintValidator<CoerenzaCodiceFiscale, DatiAnagrafici> {

    @Override
    public boolean isValid(DatiAnagrafici request, ConstraintValidatorContext context) {
        if (request == null || request.dataNascita() == null || request.sesso() == null || request.codiceFiscale() == null) {
            return true;
        }

        String cf = CodiceFiscaleUtil.normalizza(request.codiceFiscale());
        if (!CodiceFiscaleUtil.formatoValido(cf)) {
            return true; // formato già segnalato da @CodiceFiscale sul campo
        }

        boolean valido = true;
        context.disableDefaultConstraintViolation();

        // Data di nascita e sesso: verificabili solo se il CF è nel formato standard (no omocodia spinta).
        CfDecodificato decodificato = CfDecodificato.da(cf);
        if (decodificato != null) {
            LocalDate dataNascita = request.dataNascita();
            boolean dataSessoCoerenti = decodificato.sesso() == request.sesso()
                    && decodificato.mese() == dataNascita.getMonthValue()
                    && decodificato.giorno() == dataNascita.getDayOfMonth()
                    && decodificato.anno() == dataNascita.getYear() % 100;
            if (!dataSessoCoerenti) {
                valido = false;
                violazione(context,
                        "Il codice fiscale non è coerente con la data di nascita e/o il sesso dichiarati",
                        "codiceFiscale");
            }
        }

        // Cognome e nome: le prime 6 posizioni del CF sono sempre lettere, quindi confrontabili
        // anche quando la parte data/sesso non è decodificabile.
        if (cf.substring(0, 6).chars().allMatch(Character::isLetter)) {
            String cognomeAtteso = CodiceCognomeNome.cognome(request.cognome());
            if (cognomeAtteso != null && !cognomeAtteso.equals(cf.substring(0, 3))) {
                valido = false;
                violazione(context,
                        "Il cognome non corrisponde al codice fiscale: inseriscilo come sulla tessera sanitaria",
                        "cognome");
            }

            String nomeAtteso = CodiceCognomeNome.nome(request.nome());
            if (nomeAtteso != null && !nomeAtteso.equals(cf.substring(3, 6))) {
                valido = false;
                violazione(context,
                        "Il nome non corrisponde al codice fiscale: inseriscilo per intero come sulla tessera sanitaria",
                        "nome");
            }
        }

        return valido;
    }

    private void violazione(ConstraintValidatorContext context, String messaggio, String campo) {
        context.buildConstraintViolationWithTemplate(messaggio)
                .addPropertyNode(campo)
                .addConstraintViolation();
    }
}
