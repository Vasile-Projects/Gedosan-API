package it.vszdev.gedosanapi.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private record StatoErrore(HttpStatus stato, String titolo) {
    }

    private static final Map<Class<? extends RuntimeException>, StatoErrore> ERRORI_APPLICATIVI = Map.ofEntries(
            Map.entry(CredenzialiNonValideException.class, new StatoErrore(HttpStatus.UNAUTHORIZED, "Non autorizzato")),
            Map.entry(RichiestaNonValidaException.class, new StatoErrore(HttpStatus.BAD_REQUEST, "Richiesta non valida")),
            Map.entry(RisorsaNonTrovataException.class, new StatoErrore(HttpStatus.NOT_FOUND, "Risorsa non trovata")),
            Map.entry(RegolaPrenotazioneException.class, new StatoErrore(HttpStatus.CONFLICT, "Prenotazione non consentita"))
    );

    @ExceptionHandler({
            CredenzialiNonValideException.class,
            RichiestaNonValidaException.class,
            RisorsaNonTrovataException.class,
            RegolaPrenotazioneException.class
    })
    public ResponseEntity<Errore> gestisciErroreApplicativo(RuntimeException ex, HttpServletRequest request) {
        StatoErrore statoErrore = ERRORI_APPLICATIVI.get(ex.getClass());
        return costruisciErrore(statoErrore.stato(), statoErrore.titolo(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Errore> gestisciValidazione(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErroreValidazione> erroriCampo = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErroreValidazione(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new Errore(HttpStatus.BAD_REQUEST.value(), "Richiesta non valida",
                        "Uno o più campi non sono validi", request.getRequestURI(), erroriCampo));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Errore> gestisciVincoloParametro(ConstraintViolationException ex, HttpServletRequest request) {
        List<ErroreValidazione> erroriCampo = ex.getConstraintViolations().stream()
                .map(v -> new ErroreValidazione(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new Errore(HttpStatus.BAD_REQUEST.value(), "Richiesta non valida",
                        "Uno o più parametri non sono validi", request.getRequestURI(), erroriCampo));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Errore> gestisciParametroNonValido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String messaggio = "Valore non valido per il parametro '" + ex.getName() + "'";
        return costruisciErrore(HttpStatus.BAD_REQUEST, "Richiesta non valida", messaggio, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Errore> gestisciJsonNonLeggibile(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String messaggio = "Corpo della richiesta malformato";
        List<ErroreValidazione> erroriCampo = new ArrayList<>();

        if (ex.getCause() instanceof InvalidFormatException ife) {
            String campo = "sconosciuto";
            var path = ife.getPath();
            if (!path.isEmpty()) {
                var riferimento = path.get(path.size() - 1);
                if (riferimento != null) {
                    campo = riferimento.getPropertyName();
                }
            }
            erroriCampo.add(new ErroreValidazione(campo, "Valore non valido: " + ife.getValue()));
            messaggio = "Uno o più campi hanno un formato non valido";
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new Errore(HttpStatus.BAD_REQUEST.value(), "Richiesta non valida", messaggio,
                        request.getRequestURI(), erroriCampo));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Errore> gestisciVincoloDb(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violazione di un vincolo del database su {}", request.getRequestURI(), ex);
        String causa = ex.getMostSpecificCause().getMessage();
        boolean vincoloDiValore = causa != null && causa.toLowerCase().contains("check constraint");
        String messaggio = vincoloDiValore
                ? "Uno o più dati inviati non rispettano i vincoli previsti"
                : "Operazione non consentita: dato duplicato";
        return costruisciErrore(HttpStatus.CONFLICT, "Conflitto dati", messaggio, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Errore> gestisciRisorsaWebNonTrovata(NoResourceFoundException ex, HttpServletRequest request) {
        return costruisciErrore(HttpStatus.NOT_FOUND, "Risorsa non trovata", "Endpoint inesistente", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Errore> gestisciGenerico(Exception ex, HttpServletRequest request) {
        log.error("Errore imprevisto su {}", request.getRequestURI(), ex);
        return costruisciErrore(HttpStatus.INTERNAL_SERVER_ERROR, "Errore interno",
                "Si è verificato un errore imprevisto", request);
    }

    private ResponseEntity<Errore> costruisciErrore(HttpStatus stato, String titolo, String messaggio,
                                                    HttpServletRequest request) {
        return ResponseEntity.status(stato)
                .body(new Errore(stato.value(), titolo, messaggio, request.getRequestURI()));
    }
}
