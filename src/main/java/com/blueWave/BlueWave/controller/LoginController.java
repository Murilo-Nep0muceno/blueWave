package com.blueWave.BlueWave.controller;

import com.blueWave.BlueWave.model.Admin;
import com.blueWave.BlueWave.model.Ong;
import com.blueWave.BlueWave.model.Voluntario;
import com.blueWave.BlueWave.repository.AdminRepository;
import com.blueWave.BlueWave.repository.OngRepository;
import com.blueWave.BlueWave.repository.VoluntarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.Optional;

@Controller
@RequestMapping("/login")
public class LoginController {

    @Autowired
    private OngRepository ongRepository;

    @Autowired
    private VoluntarioRepository voluntarioRepository;

    @Autowired
    private AdminRepository adminRepository;

    // Página de login geral - APENAS GET
    @GetMapping
    public ModelAndView loginPage(@RequestParam(value = "error", required = false) String error,
                                  @RequestParam(value = "type", required = false) String type) {
        ModelAndView mv = new ModelAndView("login");

        // Se houver erro na URL, adicionar ao modelo
        if ("true".equals(error) && type != null) {
            mv.addObject("error", true);
            mv.addObject("errorType", type);
            mv.addObject("errorMessage", "Email ou senha incorretos");
        }

        return mv;
    }

    // POST para login geral - redireciona baseado no tipo de usuário encontrado
    @PostMapping
    public ModelAndView processLogin(
            @RequestParam String email,
            @RequestParam String senha,
            HttpSession session) {

        try {
            // Validação básica
            if (email == null || email.trim().isEmpty() ||
                    senha == null || senha.trim().isEmpty()) {
                ModelAndView mv = new ModelAndView("login");
                mv.addObject("error", true);
                mv.addObject("errorMessage", "Email e senha são obrigatórios");
                return mv;
            }

            BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
            String emailLimpo = email.trim().toLowerCase();

            // Primeiro tenta encontrar como voluntário
            Voluntario voluntario = voluntarioRepository.findByEmail(emailLimpo);
            if (voluntario != null && bc.matches(senha, voluntario.getSenha())) {
                session.setAttribute("userEmail", emailLimpo);
                session.setAttribute("userType", "voluntario");
                session.setAttribute("userId", voluntario.getId());
                session.setAttribute("userName", voluntario.getNomeVoluntario());
                return new ModelAndView("redirect:/inscricao/vagasVoluntario");
            }

            // Se não for voluntário, tenta como ONG
            Ong ong = ongRepository.findByEmail(emailLimpo);
            if (ong != null && bc.matches(senha, ong.getSenha())) {
                session.setAttribute("userEmail", emailLimpo);
                session.setAttribute("userType", "ong");
                session.setAttribute("userId", ong.getId());
                session.setAttribute("userName", ong.getNome());
                return new ModelAndView("redirect:/vagas/homeOng");
            }

            // Se chegou até aqui, credenciais são inválidas
            ModelAndView mv = new ModelAndView("login");
            mv.addObject("error", true);
            mv.addObject("errorMessage", "Email ou senha incorretos");
            return mv;

        } catch (Exception e) {
            e.printStackTrace();
            ModelAndView mv = new ModelAndView("login");
            mv.addObject("error", true);
            mv.addObject("errorMessage", "Erro interno do servidor. Tente novamente.");
            return mv;
        }
    }

    // Login específico para Voluntários
    @GetMapping("/voluntario")
    public ModelAndView loginVoluntario(@RequestParam(value = "error", required = false) String error) {
        ModelAndView mv = new ModelAndView("login");

        // Forçar mostrar o formulário de voluntário se houver erro
        if ("true".equals(error)) {
            mv.addObject("showVoluntarioForm", true);
            mv.addObject("error", true);
            mv.addObject("errorType", "voluntario");
            mv.addObject("errorMessage", "Email ou senha incorretos");
        }

        return mv;
    }

    @PostMapping("/voluntario")
    public ModelAndView logarVoluntario(
            @RequestParam String email,
            @RequestParam String senha,
            HttpSession session) {

        try {
            // Validação básica
            if (email == null || email.trim().isEmpty() ||
                    senha == null || senha.trim().isEmpty()) {
                return new ModelAndView("redirect:/login?error=true&type=voluntario&message=campos_obrigatorios");
            }

            BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
            String emailLimpo = email.trim().toLowerCase();
            Voluntario voluntario = voluntarioRepository.findByEmail(emailLimpo);

            if (voluntario != null && bc.matches(senha, voluntario.getSenha())) {
                session.setAttribute("userEmail", emailLimpo);
                session.setAttribute("userType", "voluntario");
                session.setAttribute("userId", voluntario.getId());
                session.setAttribute("userName", voluntario.getNomeVoluntario());
                return new ModelAndView("redirect:/inscricao/vagasVoluntario");
            }

            // Credenciais inválidas - redirecionar com erro
            return new ModelAndView("redirect:/login?error=true&type=voluntario");

        } catch (Exception e) {
            e.printStackTrace();
            return new ModelAndView("redirect:/login?error=true&type=voluntario&message=erro_interno");
        }
    }

    // Login específico para ONGs - MODIFICADO PARA DETECTAR ADMIN
    @GetMapping("/ong")
    public ModelAndView loginOng(@RequestParam(value = "error", required = false) String error) {
        ModelAndView mv = new ModelAndView("login");

        // Forçar mostrar o formulário de ONG se houver erro
        if ("true".equals(error)) {
            mv.addObject("showOngForm", true);
            mv.addObject("error", true);
            mv.addObject("errorType", "ong");
            mv.addObject("errorMessage", "Email ou senha incorretos");
        }

        return mv;
    }

    @PostMapping("/ong")
    public ModelAndView logarOng(
            @RequestParam String email,
            @RequestParam String senha,
            HttpSession session) {

        try {
            // Validação básica
            if (email == null || email.trim().isEmpty() ||
                    senha == null || senha.trim().isEmpty()) {
                return new ModelAndView("redirect:/login?error=true&type=ong&message=campos_obrigatorios");
            }

            BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
            String emailLimpo = email.trim().toLowerCase();

            // ===== VERIFICAR SE É ADMIN PRIMEIRO =====
            if ("admin@gmail.com".equals(emailLimpo)) {
                // Limpar bloqueios expirados
                adminRepository.limparBloqueiosExpirados(LocalDateTime.now());

                Optional<Admin> adminOpt = adminRepository.findByEmailForAuth(emailLimpo, LocalDateTime.now());

                if (adminOpt.isPresent()) {
                    Admin admin = adminOpt.get();

                    if (bc.matches(senha, admin.getSenha())) {
                        // Login bem-sucedido do admin
                        admin.registrarLoginSucesso();
                        adminRepository.save(admin);

                        // Configurar sessão como ADMIN
                        session.setAttribute("userEmail", emailLimpo);
                        session.setAttribute("userType", "admin");
                        session.setAttribute("userId", admin.getId());
                        session.setAttribute("userName", admin.getNome());
                        session.setAttribute("adminLevel", admin.getNivelAcesso().name());

                        return new ModelAndView("redirect:/admin/dashboard");
                    } else {
                        // Senha incorreta do admin
                        admin.registrarTentativaFalha();
                        adminRepository.save(admin);
                    }
                }

                // Se chegou aqui, credenciais do admin são inválidas
                return new ModelAndView("redirect:/login?error=true&type=ong");
            }

            // ===== VERIFICAR COMO ONG NORMAL =====
            Ong ong = ongRepository.findByEmail(emailLimpo);
            if (ong != null && bc.matches(senha, ong.getSenha())) {
                session.setAttribute("userEmail", emailLimpo);
                session.setAttribute("userType", "ong");
                session.setAttribute("userId", ong.getId());
                session.setAttribute("userName", ong.getNome());
                return new ModelAndView("redirect:/vagas/homeOng");
            }

            // Credenciais inválidas - redirecionar com erro
            return new ModelAndView("redirect:/login?error=true&type=ong");

        } catch (Exception e) {
            e.printStackTrace();
            return new ModelAndView("redirect:/login?error=true&type=ong&message=erro_interno");
        }
    }

    // Logout
    @GetMapping("/logout")
    public ModelAndView logout(HttpSession session) {
        try {
            if (session != null) {
                session.invalidate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ModelAndView("redirect:/");
    }
}