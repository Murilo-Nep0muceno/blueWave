package com.blueWave.BlueWave.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Configuração para servir arquivos estáticos da pasta uploads
        String uploadPath = "file:" + System.getProperty("user.dir") + File.separator + "uploads" + File.separator;

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath)
                .setCachePeriod(3600) // Cache por 1 hora
                .resourceChain(true);

        // Configuração padrão para recursos estáticos (CSS, JS, imagens)
        registry.addResourceHandler("/css/**", "/js/**", "/images/**")
                .addResourceLocations("classpath:/static/css/", "classpath:/static/js/", "classpath:/static/images/")
                .setCachePeriod(3600);

        // Configuração para servir recursos da pasta static
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);
    }
}