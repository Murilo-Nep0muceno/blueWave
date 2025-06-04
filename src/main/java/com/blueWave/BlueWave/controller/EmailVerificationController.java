package com.blueWave.BlueWave.controller;

import com.blueWave.BlueWave.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/test")
public class EmailVerificationController {

    @Autowired
    private EmailService emailService;

    // Teste básico de envio de email
    @PostMapping("/email")
    public ResponseEntity<?> testarEmail(@RequestParam String destinatario) {
        try {
            emailService.enviarEmailRecuperacaoSenha(
                    destinatario,
                    "Usuário Teste",
                    "http://localhost:8080/recuperar-senha/redefinir?token=teste-funcionamento"
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "✅ Email de teste enviado com sucesso para: " + destinatario,
                    "instrucoes", "Verifique sua caixa de entrada e spam"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "❌ Erro ao enviar email: " + e.getMessage(),
                    "detalhes", e.getClass().getSimpleName()
            ));
        }
    }

    // Teste de confirmação de alteração
    @PostMapping("/email-confirmacao")
    public ResponseEntity<?> testarEmailConfirmacao(@RequestParam String destinatario) {
        try {
            emailService.enviarEmailConfirmacaoAlteracaoSenha(destinatario, "Usuário Teste");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "✅ Email de confirmação enviado com sucesso para: " + destinatario
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "❌ Erro ao enviar email de confirmação: " + e.getMessage()
            ));
        }
    }

    // Verificar configuração de email
    @GetMapping("/config")
    public ResponseEntity<?> verificarConfiguracao() {
        return ResponseEntity.ok(Map.of(
                "status", "Configuração de email carregada",
                "host", "smtp.gmail.com",
                "port", "587",
                "usuario", "bluwavecurso@gmail.com",
                "observacao", "Senha configurada via properties"
        ));
    }
}