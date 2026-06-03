package com.finanzas.finanzas;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configuración de seguridad básica para desarrollo.
     * Permite todas las peticiones al API sin autenticación de Spring Security
     * (el login lo maneja nuestra propia lógica en UsuarioService).
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())        // Habilitar CORS para el frontend
            .csrf(csrf -> csrf.disable())           // Deshabilitar CSRF para API REST
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()           // Permitir todo (manejamos auth manual)
            );
        return http.build();
    }
}
