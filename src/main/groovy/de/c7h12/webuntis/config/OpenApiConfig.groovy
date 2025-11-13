package de.c7h12.webuntis.config

import groovy.transform.CompileStatic
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI/Swagger configuration for API documentation
 */
@CompileStatic
@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("WebUntis Groovy API")
                .version("1.0.0")
                .description("""
                    REST API für WebUntis Integration mit erweiterten Features:

                    ## Features
                    - Standard WebUntis API Integration
                    - Enhanced 2017 API mit Hausaufgaben, Abwesenheiten und Ferien
                    - QR-Code Extraktion für einfache Konfiguration
                    - Automatische OTP-Generierung
                    - Master-Daten Caching
                    - Rate Limiting (100 Anfragen/Minute)

                    ## Authentication
                    - **Standard API**: Benutzername + Passwort
                    - **2017 API**: Benutzername + App Secret (automatische OTP-Generation)

                    ## Endpoints
                    - `/api/webuntis/*` - Standard WebUntis API
                    - `/api/webuntis/v2017/*` - Enhanced 2017 API
                    - `/api/qrcode/*` - QR-Code Verarbeitung
                """.stripIndent())
                .contact(new Contact()
                    .name("C7H12")
                    .url("https://github.com/ptC7H12/WebUntisGroovyAPI"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .addServersItem(new Server()
                .url("http://localhost:8080")
                .description("Local Development Server"))
    }
}
