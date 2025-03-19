package org.microservice.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity // Sử dụng cho WebFlux
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable()) // Tắt CSRF
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll()
                )
                .httpBasic(httpBasic -> httpBasic.disable()) // Tắt Basic Auth
                .build();
    }
}

