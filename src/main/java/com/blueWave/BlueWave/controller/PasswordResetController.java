package com.blueWave.BlueWave.controller;

import com.blueWave.BlueWave.EmailService;
import com.blueWave.BlueWave.model.Ong;
import com.blueWave.BlueWave.model.PasswordResetToken;
import com.blueWave.BlueWave.model.Voluntario;
import com.blueWave.BlueWave.repository.OngRepository;
import com.blueWave.BlueWave.repository.PasswordResetTokenRepository;
import com.blueWave.BlueWave.repository.VoluntarioRepository;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/recuperar-senha")
public class PasswordResetController {

    @Autowired
    private VoluntarioRepository voluntarioRepository;

    @Autowired
    private OngRepository ongRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Página inicial de recuperação de senha
    @GetMapping
    public ModelAndView recuperarSenhaPage() {
        return new ModelAndView("recuperarSenha");
    }

    // Processar solicitação de recuperação de senha
    @PostMapping("/solicitar")
    @ResponseBody
    public ResponseEntity<?> solicitarRecuperacao(@RequestParam String email, HttpServletRequest request) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email é obrigatório"));
            }

            email = email.trim().toLowerCase();

            // Verificar se é voluntário ou ONG
            Voluntario voluntario = voluntarioRepository.findByEmail(email);
            Ong ong = ongRepository.findByEmail(email);

            if (voluntario == null && ong == null) {
                // Por segurança, não revelamos se o email existe ou não
                return ResponseEntity.ok(Map.of("success", true, "message", "Se o email estiver cadastrado, você receberá instruções para recuperar sua senha."));
            }

            // Gerar token único
            String token = UUID.randomUUID().toString();
            LocalDateTime expirationTime = LocalDateTime.now().plusHours(1); // Token válido por 1 hora

            // Remover tokens antigos para este email
            tokenRepository.deleteByEmail(email);

            // Criar novo token
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(token);
            resetToken.setEmail(email);
            resetToken.setUserType(voluntario != null ? "voluntario" : "ong");
            resetToken.setExpirationTime(expirationTime);
            resetToken.setUsed(false);

            tokenRepository.save(resetToken);

            // Construir URL de reset
            String baseUrl = getBaseUrl(request);
            String resetUrl = baseUrl + "/recuperar-senha/redefinir?token=" + token;

            // Determinar nome do usuário
            String nomeUsuario = voluntario != null ? voluntario.getNomeVoluntario() : ong.getNome();

            // Enviar email
            emailService.enviarEmailRecuperacaoSenha(email, nomeUsuario, resetUrl);

            return ResponseEntity.ok(Map.of("success", true, "message", "Se o email estiver cadastrado, você receberá instruções para recuperar sua senha."));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Erro interno do servidor. Tente novamente mais tarde."));
        }
    }

    // Página de redefinição de senha
    @GetMapping("/redefinir")
    public ModelAndView redefinirSenhaPage(@RequestParam String token, Model model) {
        // Verificar se token é válido
        PasswordResetToken resetToken = tokenRepository.findByTokenAndUsedFalse(token);

        if (resetToken == null || resetToken.getExpirationTime().isBefore(LocalDateTime.now())) {
            ModelAndView mv = new ModelAndView("recuperarSenha");
            mv.addObject("error", "Token inválido ou expirado. Solicite uma nova recuperação de senha.");
            return mv;
        }

        ModelAndView mv = new ModelAndView("redefinirSenha");
        mv.addObject("token", token);
        mv.addObject("email", resetToken.getEmail());
        mv.addObject("userType", resetToken.getUserType());
        return mv;
    }

    // Processar redefinição de senha
    @PostMapping("/redefinir")
    @ResponseBody
    public ResponseEntity<?> redefinirSenha(
            @RequestParam String token,
            @RequestParam String novaSenha,
            @RequestParam String confirmarSenha) {

        try {
            // Validações básicas
            if (token == null || token.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Token é obrigatório"));
            }

            if (novaSenha == null || novaSenha.length() < 8) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Nova senha deve ter pelo menos 8 caracteres"));
            }

            if (!novaSenha.equals(confirmarSenha)) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Senhas não coincidem"));
            }

            // Verificar token
            PasswordResetToken resetToken = tokenRepository.findByTokenAndUsedFalse(token);

            if (resetToken == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Token inválido"));
            }

            if (resetToken.getExpirationTime().isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Token expirado. Solicite uma nova recuperação de senha."));
            }

            // Criptografar nova senha
            String senhaCriptografada = passwordEncoder.encode(novaSenha);

            // Atualizar senha baseado no tipo de usuário
            boolean senhaAtualizada = false;
            String nomeUsuario = "";

            if ("voluntario".equals(resetToken.getUserType())) {
                Voluntario voluntario = voluntarioRepository.findByEmail(resetToken.getEmail());
                if (voluntario != null) {
                    voluntario.setSenha(senhaCriptografada);
                    voluntarioRepository.save(voluntario);
                    nomeUsuario = voluntario.getNomeVoluntario();
                    senhaAtualizada = true;
                }
            } else if ("ong".equals(resetToken.getUserType())) {
                Ong ong = ongRepository.findByEmail(resetToken.getEmail());
                if (ong != null) {
                    ong.setSenha(senhaCriptografada);
                    ongRepository.save(ong);
                    nomeUsuario = ong.getNome();
                    senhaAtualizada = true;
                }
            }

            if (!senhaAtualizada) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Usuário não encontrado"));
            }

            // Marcar token como usado
            resetToken.setUsed(true);
            resetToken.setUsedAt(LocalDateTime.now());
            tokenRepository.save(resetToken);

            // Enviar email de confirmação
            try {
                emailService.enviarEmailConfirmacaoAlteracaoSenha(resetToken.getEmail(), nomeUsuario);
            } catch (Exception e) {
                // Log do erro, mas não falha a operação
                System.err.println("Erro ao enviar email de confirmação: " + e.getMessage());
            }

            return ResponseEntity.ok(Map.of("success", true, "message", "Senha alterada com sucesso! Você pode fazer login com sua nova senha."));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Erro interno do servidor. Tente novamente mais tarde."));
        }
    }

    // Método auxiliar para obter URL base
    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);

        if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
            url.append(":").append(serverPort);
        }

        url.append(contextPath);
        return url.toString();
    }

    // Endpoint para verificar status do token (opcional, para AJAX)
    @GetMapping("/verificar-token")
    @ResponseBody
    public ResponseEntity<?> verificarToken(@RequestParam String token) {
        PasswordResetToken resetToken = tokenRepository.findByTokenAndUsedFalse(token);

        if (resetToken == null) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "message", "Token não encontrado"));
        }

        if (resetToken.getExpirationTime().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "message", "Token expirado"));
        }

        return ResponseEntity.ok(Map.of("valid", true, "email", resetToken.getEmail(), "userType", resetToken.getUserType()));
    }
}