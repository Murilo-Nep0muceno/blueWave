package com.blueWave.BlueWave.controller;

import com.blueWave.BlueWave.model.Ong;
import com.blueWave.BlueWave.model.Voluntario;
import com.blueWave.BlueWave.repository.OngRepository;
import com.blueWave.BlueWave.repository.VoluntarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/login")
public class LoginController {

    @Autowired
    private OngRepository ongRepository;

    @Autowired
    private VoluntarioRepository voluntarioRepository;

    // Página de login geral - APENAS GET
    @GetMapping
    public ModelAndView loginPage() {
        ModelAndView mv = new ModelAndView("login");
        return mv;
    }

    // POST para login geral - redireciona baseado no tipo de usuário encontrado
    @PostMapping
    public ModelAndView processLogin(
            @RequestParam String email,
            @RequestParam String senha,
            HttpSession session) {

        BCryptPasswordEncoder bc = new BCryptPasswordEncoder();

        // Primeiro tenta encontrar como voluntário
        Voluntario voluntario = voluntarioRepository.findByEmail(email);
        if (voluntario != null && bc.matches(senha, voluntario.getSenha())) {
            session.setAttribute("userEmail", email);
            session.setAttribute("userType", "voluntario");
            session.setAttribute("userId", voluntario.getId());
            session.setAttribute("userName", voluntario.getNomeVoluntario());
            return new ModelAndView("redirect:/inscricao/vagasVoluntario");
        }

        // Se não for voluntário, tenta como ONG
        Ong ong = ongRepository.findByEmail(email);
        if (ong != null && bc.matches(senha, ong.getSenha())) {
            session.setAttribute("userEmail", email);
            session.setAttribute("userType", "ong");
            session.setAttribute("userId", ong.getId());
            session.setAttribute("userName", ong.getNome());
            return new ModelAndView("redirect:/vagas/homeOng");
        }

        // Se chegou até aqui, credenciais são inválidas
        ModelAndView mv = new ModelAndView("login");
        mv.addObject("error", "Credenciais inválidas");
        return mv;
    }

    // Login para Voluntários
    @GetMapping("/voluntario")
    public ModelAndView loginVoluntario() {
        ModelAndView mv = new ModelAndView("loginVoluntario");
        return mv;
    }

    @PostMapping("/voluntario")
    public ModelAndView logarVoluntario(
            @RequestParam String email,
            @RequestParam String senha,
            HttpSession session) {

        BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
        Voluntario voluntario = voluntarioRepository.findByEmail(email);

        if (voluntario != null && bc.matches(senha, voluntario.getSenha())) {
            session.setAttribute("userEmail", email);
            session.setAttribute("userType", "voluntario");
            session.setAttribute("userId", voluntario.getId());
            session.setAttribute("userName", voluntario.getNomeVoluntario());
            return new ModelAndView("redirect:/inscricao/vagasVoluntario");
        }

        ModelAndView mv = new ModelAndView("loginVoluntario");
        mv.addObject("error", "Credenciais inválidas");
        return mv;
    }

    // Login para ONGs
    @GetMapping("/ong")
    public ModelAndView loginOng() {
        ModelAndView mv = new ModelAndView("loginOng");
        return mv;
    }

    @PostMapping("/ong")
    public ModelAndView logarOng(
            @RequestParam String email,
            @RequestParam String senha,
            HttpSession session) {

        BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
        Ong ong = ongRepository.findByEmail(email);

        if (ong != null && bc.matches(senha, ong.getSenha())) {
            session.setAttribute("userEmail", email);
            session.setAttribute("userType", "ong");
            session.setAttribute("userId", ong.getId());
            session.setAttribute("userName", ong.getNome());
            return new ModelAndView("redirect:/vagas/homeOng");
        }

        ModelAndView mv = new ModelAndView("loginOng");
        mv.addObject("error", "Credenciais inválidas");
        return mv;
    }

    // Logout
    @GetMapping("/logout")
    public ModelAndView logout(HttpSession session) {
        session.invalidate();
        return new ModelAndView("redirect:/");
    }
}