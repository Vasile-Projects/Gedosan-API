package it.vszdev.gedosanapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CoerenzaCodiceFiscaleValidator.class)
public @interface CoerenzaCodiceFiscale {

    String message() default "Il codice fiscale non è coerente con la data di nascita e/o il sesso dichiarati";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
