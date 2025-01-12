package com.videocompression.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI videoCompressionOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Video Compression Platform (VCP) API")
                .description("RESTful API for video compression and format conversion")
                .version("1.0.0")
                .contact(new Contact()
                    .name("VCP Team")
                    .email("support@nohost.co")
                    .url("https://github.com/syswe/vcp"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Local development server")
            ));
    }
} 