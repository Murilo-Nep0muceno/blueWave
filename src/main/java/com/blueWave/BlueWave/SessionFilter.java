package com.blueWave.BlueWave.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class SessionFilter implements Filter {

    // URLs que requerem login de ONG
    private static final List<String> ONG_PROTECTED_URLS = Arrays.asList(
            "/vagas/homeOng",
            "/vagas/listaVaga",
            "/vagas/editar"
    );

    // URLs que requerem login de Voluntário
    private static final List<String> VOLUNTARIO_PROTECTED_URLS = Arrays.asList(
            "/inscricao/vagasVoluntario",
            "/inscricao/minhasVagas"
    );

    // URLs públicas (não requerem autenticação)
    private static final List<String> PUBLIC_URLS = Arrays.asList(
            "/",
            "/login",           // Inclui todas as rotas de login
            "/login/ong",       // Login específico para ONG
            "/login/voluntario", // Login específico para Voluntário
            "/login/logout",    // Logout
            "/ongForm",
            "/voluntarioForm",
            "/css",
            "/js",
            "/scripts",         // Adicionado para incluir JavaScript
            "/images",
            "/uploads",
            "/static",
            "/error",
            "/favicon.ico"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);

        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String path = requestURI.substring(contextPath.length());

        // Verifica se é uma URL pública
        if (isPublicUrl(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Verifica se há sessão ativa
        boolean isLoggedIn = session != null && session.getAttribute("userEmail") != null;
        String userType = session != null ? (String) session.getAttribute("userType") : null;

        // Se não está logado e não é URL pública, redireciona para login geral
        if (!isLoggedIn) {
            // Se está tentando acessar área específica, redireciona para login específico
            if (isOngProtectedUrl(path)) {
                httpResponse.sendRedirect(contextPath + "/login/ong");
                return;
            } else if (isVoluntarioProtectedUrl(path)) {
                httpResponse.sendRedirect(contextPath + "/login/voluntario");
                return;
            } else {
                // Para outras URLs protegidas, redireciona para login geral
                httpResponse.sendRedirect(contextPath + "/login");
                return;
            }
        }

        // Protege URLs de ONG - verifica se o usuário logado é ONG
        if (isOngProtectedUrl(path)) {
            if (!"ong".equals(userType)) {
                httpResponse.sendRedirect(contextPath + "/login/ong");
                return;
            }
        }

        // Protege URLs de Voluntário - verifica se o usuário logado é Voluntário
        if (isVoluntarioProtectedUrl(path)) {
            if (!"voluntario".equals(userType)) {
                httpResponse.sendRedirect(contextPath + "/login/voluntario");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicUrl(String path) {
        return PUBLIC_URLS.stream().anyMatch(url -> path.equals(url) || path.startsWith(url + "/"));
    }

    private boolean isOngProtectedUrl(String path) {
        return ONG_PROTECTED_URLS.stream().anyMatch(path::startsWith);
    }

    private boolean isVoluntarioProtectedUrl(String path) {
        return VOLUNTARIO_PROTECTED_URLS.stream().anyMatch(path::startsWith);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Inicialização do filtro, se necessário
    }

    @Override
    public void destroy() {
        // Limpeza de recursos, se necessário
    }
}