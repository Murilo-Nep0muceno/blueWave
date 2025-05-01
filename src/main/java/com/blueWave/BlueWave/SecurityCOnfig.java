package com.blueWave.BlueWave;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityCOnfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // libera todas as URLs sem autenticação
                .authorizeHttpRequests(authz -> authz
                        .anyRequest().permitAll()
                )
                // desabilita CSRF se não precisar
                .csrf(csrf -> csrf.disable())
                // desabilita o formulário de login padrão
                .formLogin(form -> form.disable())
                // desabilita o login HTTP Basic (opcional)
                .httpBasic(httpBasic -> httpBasic.disable())
                // desabilita logout
                .logout(logout -> logout.disable());

        return http.build();
    }
    }
