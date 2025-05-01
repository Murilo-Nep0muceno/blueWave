package com.blueWave.BlueWave.controller;


import com.blueWave.BlueWave.model.Ong;
import com.blueWave.BlueWave.model.Voluntario;
import com.blueWave.BlueWave.repository.OngRepository;
import com.blueWave.BlueWave.repository.VoluntarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequestMapping("/login")
@SessionAttributes("userEmail")
public class LoginController {

    @Autowired private OngRepository or;
    @Autowired private VoluntarioRepository vr;

    @ModelAttribute("userEmail")
    public String userEmail() {
        return null;
    }

    @GetMapping()
    public ModelAndView login(){
        return new ModelAndView("login");
    }

    @PostMapping()
    public ModelAndView logarSistema(
            @RequestParam String email,
            @RequestParam String senha,
            @RequestParam String tipo,
            Model model) {

        BCryptPasswordEncoder bc = new BCryptPasswordEncoder();

        if ("voluntario".equalsIgnoreCase(tipo)) {
            Voluntario voluntario = vr.findByEmail(email);
            if (voluntario != null && bc.matches(senha, voluntario.getSenha())) {
                model.addAttribute("userEmail", email);
                return new ModelAndView("redirect:/inscricao/vagasVoluntario");
            }
        }
        else if ("ong".equalsIgnoreCase(tipo)) {
            Ong ong = or.findByEmail(email);
            if (ong != null && bc.matches(senha, ong.getSenha())) {
                model.addAttribute("userEmail", email);
                return new ModelAndView("redirect:/vagas/homeOng");
            }
        }

        ModelAndView mv = new ModelAndView("login");
        mv.addObject("error","Credenciais inválidas");
        return mv;
    }
}
