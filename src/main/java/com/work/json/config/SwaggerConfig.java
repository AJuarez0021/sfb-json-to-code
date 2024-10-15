package com.work.json.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class SwaggerConfig.
 *
 * @author ajuarez
 */
@Configuration
@Slf4j
public class SwaggerConfig {

    /**
     * Open API.
     *
     * @return the open API
     */
    @Bean
    OpenAPI openAPI() {
        return new OpenAPI().info(new Info().title("Json-To-Code").version("1.0.0")
                .description("Aplicacion para convertir json a codigo java").contact(getContact()));
    }

    /**
     * Gets the contact.
     *
     * @return the contact
     */
    private Contact getContact() {
        return new Contact().name("json")
                .url("https://www.mypage.com/")
                .email("json@mail.com");
    }
}
