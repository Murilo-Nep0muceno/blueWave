package com.blueWave.BlueWave;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:Blue Wave}")
    private String appName;

    public void enviarEmailRecuperacaoSenha(String toEmail, String nomeUsuario, String resetUrl)
            throws MessagingException, UnsupportedEncodingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // Configurar remetente e destinatário
        helper.setFrom(fromEmail, appName);
        helper.setTo(toEmail);
        helper.setSubject("Recuperação de Senha - " + appName);

        // Usar método de construção HTML inline
        String htmlContent = construirEmailRecuperacaoHTML(nomeUsuario, resetUrl);
        helper.setText(htmlContent, true);

        // Enviar email
        mailSender.send(message);
    }

    public void enviarEmailConfirmacaoAlteracaoSenha(String toEmail, String nomeUsuario)
            throws MessagingException, UnsupportedEncodingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail, appName);
        helper.setTo(toEmail);
        helper.setSubject("Senha Alterada com Sucesso - " + appName);

        String htmlContent = construirEmailConfirmacaoHTML(nomeUsuario);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    private String construirEmailRecuperacaoHTML(String nomeUsuario, String resetUrl) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Recuperação de Senha</title>
                <style>
                    body {
                        font-family: 'Arial', sans-serif;
                        line-height: 1.6;
                        color: #333;
                        background-color: #f4f4f4;
                        margin: 0;
                        padding: 20px;
                    }
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        background-color: white;
                        border-radius: 10px;
                        overflow: hidden;
                        box-shadow: 0 0 20px rgba(0,0,0,0.1);
                    }
                    .header {
                        background: linear-gradient(135deg, #0ea5e9 0%%, #0284c7 100%%);
                        color: white;
                        text-align: center;
                        padding: 30px 20px;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 2rem;
                        font-weight: 700;
                        letter-spacing: 1px;
                    }
                    .content {
                        padding: 40px 30px;
                    }
                    .content h2 {
                        color: #1e293b;
                        margin-top: 0;
                        margin-bottom: 20px;
                    }
                    .content p {
                        margin-bottom: 20px;
                        font-size: 16px;
                    }
                    .button {
                        display: inline-block;
                        background: linear-gradient(135deg, #0ea5e9, #0284c7);
                        color: white;
                        text-decoration: none;
                        padding: 15px 30px;
                        border-radius: 8px;
                        font-weight: 600;
                        text-align: center;
                        margin: 20px 0;
                        transition: transform 0.2s ease;
                    }
                    .button:hover {
                        transform: translateY(-2px);
                    }
                    .footer {
                        background-color: #f8f9fa;
                        padding: 20px 30px;
                        text-align: center;
                        color: #6c757d;
                        font-size: 14px;
                    }
                    .warning {
                        background-color: #fff3cd;
                        border: 1px solid #ffeaa7;
                        border-radius: 5px;
                        padding: 15px;
                        margin: 20px 0;
                        color: #856404;
                    }
                    .link-alternative {
                        background-color: #f8f9fa;
                        padding: 15px;
                        border-radius: 5px;
                        margin: 20px 0;
                        word-break: break-all;
                        font-family: monospace;
                        font-size: 14px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>%s</h1>
                    </div>
                    
                    <div class="content">
                        <h2>Olá, %s!</h2>
                        
                        <p>Recebemos uma solicitação para redefinir a senha da sua conta no %s.</p>
                        
                        <p>Para criar uma nova senha, clique no botão abaixo:</p>
                        
                        <div style="text-align: center;">
                            <a href="%s" class="button">Redefinir Senha</a>
                        </div>
                        
                        <div class="warning">
                            <strong>⚠️ Importante:</strong>
                            <ul>
                                <li>Este link é válido por apenas 1 hora</li>
                                <li>Só pode ser usado uma vez</li>
                                <li>Se você não solicitou esta alteração, ignore este email</li>
                            </ul>
                        </div>
                        
                        <p>Se o botão não funcionar, copie e cole o link abaixo no seu navegador:</p>
                        
                        <div class="link-alternative">
                            %s
                        </div>
                        
                        <p>Se você não solicitou a recuperação de senha, pode ignorar este email com segurança. Sua senha atual permanecerá inalterada.</p>
                    </div>
                    
                    <div class="footer">
                        <p>Este é um email automático, não responda.</p>
                        <p>&copy; 2024 %s - Plataforma de Voluntariado</p>
                    </div>
                </div>
            </body>
            </html>
            """, appName, nomeUsuario, appName, resetUrl, resetUrl, appName);
    }

    private String construirEmailConfirmacaoHTML(String nomeUsuario) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Senha Alterada com Sucesso</title>
                <style>
                    body {
                        font-family: 'Arial', sans-serif;
                        line-height: 1.6;
                        color: #333;
                        background-color: #f4f4f4;
                        margin: 0;
                        padding: 20px;
                    }
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        background-color: white;
                        border-radius: 10px;
                        overflow: hidden;
                        box-shadow: 0 0 20px rgba(0,0,0,0.1);
                    }
                    .header {
                        background: linear-gradient(135deg, #10b981 0%%, #059669 100%%);
                        color: white;
                        text-align: center;
                        padding: 30px 20px;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 2rem;
                        font-weight: 700;
                        letter-spacing: 1px;
                    }
                    .content {
                        padding: 40px 30px;
                    }
                    .content h2 {
                        color: #1e293b;
                        margin-top: 0;
                        margin-bottom: 20px;
                    }
                    .content p {
                        margin-bottom: 20px;
                        font-size: 16px;
                    }
                    .success-icon {
                        text-align: center;
                        font-size: 48px;
                        color: #10b981;
                        margin: 20px 0;
                    }
                    .footer {
                        background-color: #f8f9fa;
                        padding: 20px 30px;
                        text-align: center;
                        color: #6c757d;
                        font-size: 14px;
                    }
                    .info-box {
                        background-color: #e0f2fe;
                        border: 1px solid #0284c7;
                        border-radius: 5px;
                        padding: 15px;
                        margin: 20px 0;
                        color: #0c4a6e;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>%s</h1>
                    </div>
                    
                    <div class="content">
                        <div class="success-icon">✅</div>
                        
                        <h2>Olá, %s!</h2>
                        
                        <p>Sua senha foi alterada com sucesso no %s.</p>
                        
                        <div class="info-box">
                            <strong>🔒 Informações de Segurança:</strong>
                            <ul>
                                <li>Data da alteração: Agora</li>
                                <li>Se você não fez esta alteração, entre em contato conosco imediatamente</li>
                                <li>Recomendamos usar senhas fortes e únicas</li>
                            </ul>
                        </div>
                        
                        <p>Agora você pode fazer login com sua nova senha.</p>
                        
                        <p>Se você não fez esta alteração, entre em contato conosco imediatamente.</p>
                    </div>
                    
                    <div class="footer">
                        <p>Este é um email automático, não responda.</p>
                        <p>&copy; 2024 %s - Plataforma de Voluntariado</p>
                    </div>
                </div>
            </body>
            </html>
            """, appName, nomeUsuario, appName, appName);
    }
}