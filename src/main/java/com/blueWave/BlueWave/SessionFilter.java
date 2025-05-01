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
            "/vagas"
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
            "/scripts",         // JavaScript
            "/images",
            "/uploads",
            "/static",
            "/error",
            "/favicon.ico",
            "/webjars",
            "/quemSomoss"// Para bibliotecas JS/CSS via Maven/Gradle
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

        // Log para debug (remova em produção)
        System.out.println("Acessando: " + path);

        // Verifica se é uma URL pública
        if (isPublicUrl(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Verifica se há sessão ativa
        boolean isLoggedIn = session != null && session.getAttribute("userEmail") != null;
        String userType = session != null ? (String) session.getAttribute("userType") : null;

        // Se não está logado, redireciona para login apropriado
        if (!isLoggedIn) {
            if (isOngProtectedUrl(path)) {
                httpResponse.sendRedirect(contextPath + "/login/ong");
                return;
            } else if (isVoluntarioProtectedUrl(path)) {
                httpResponse.sendRedirect(contextPath + "/login/voluntario");
                return;
            } else {
                httpResponse.sendRedirect(contextPath + "/login");
                return;
            }
        }

        // Verifica permissões específicas
        if (isOngProtectedUrl(path) && !"ong".equals(userType)) {
            httpResponse.sendRedirect(contextPath + "/login/ong");
            return;
        }

        if (isVoluntarioProtectedUrl(path) && !"voluntario".equals(userType)) {
            httpResponse.sendRedirect(contextPath + "/login/voluntario");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicUrl(String path) {
        boolean isPublic = PUBLIC_URLS.stream().anyMatch(url ->
                path.equals(url) || path.startsWith(url + "/")
        );

        // Adiciona verificação especial para recursos estáticos
        if (!isPublic) {
            isPublic = path.matches(".*\\.(css|js|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$");
        }

        return isPublic;
    }

    private boolean isOngProtectedUrl(String path) {
        return ONG_PROTECTED_URLS.stream().anyMatch(url ->
                path.equals(url) || path.startsWith(url + "/")
        );
    }

    private boolean isVoluntarioProtectedUrl(String path) {
        return VOLUNTARIO_PROTECTED_URLS.stream().anyMatch(url ->
                path.equals(url) || path.startsWith(url + "/")
        );
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Inicialização se necessário
    }

    @Override
    public void destroy() {
        // Limpeza se necessário
    }
}