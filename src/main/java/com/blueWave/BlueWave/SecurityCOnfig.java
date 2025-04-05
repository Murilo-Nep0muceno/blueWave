package com.blueWave.BlueWave;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityCOnfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        .anyRequest().permitAll()  // permite todas as requisições
                )
                .csrf(csrf -> csrf.disable())  // desabilita CSRF se não for necessário
                .formLogin(form -> form.disable());  // desabilita o formulário de login

        return http.build();
    }


}
