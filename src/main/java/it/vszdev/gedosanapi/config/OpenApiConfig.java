package it.vszdev.gedosanapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Documentazione su /swagger-ui.html. Gli schemi di sicurezza permettono di provare dalla UI sia l'area
// admin (JWT da /api/auth/login) sia gli endpoint del bot (X-Bot-Key).
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gedosanOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gedosan API")
                        .version("1.0")
                        .description("Prenotazione delle donazioni di sangue. Area pubblica (modulo con conferma "
                                + "via email), area admin (JWT) e endpoint del bot Telegram usati dal workflow n8n."))
                .components(new Components()
                        .addSecuritySchemes("jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))
                        .addSecuritySchemes("botKey", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("X-Bot-Key")));
    }
}
