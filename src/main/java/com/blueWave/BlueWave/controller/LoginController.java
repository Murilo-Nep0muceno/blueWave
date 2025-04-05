package com.blueWave.BlueWave.controller;


import com.blueWave.BlueWave.model.Ong;
import com.blueWave.BlueWave.model.Voluntario;
import com.blueWave.BlueWave.repository.OngRepository;
import com.blueWave.BlueWave.repository.VoluntarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequestMapping("/login")
public class LoginController {

    @Autowired
    private  OngRepository or;

    @Autowired
    private VoluntarioRepository vr;

    @GetMapping()
    public ModelAndView login(){
        ModelAndView mv = new ModelAndView("login");
        return mv;
    }

    @PostMapping
    public ModelAndView logarSistema(
            @RequestParam String email,
            @RequestParam String senha,
            @RequestParam String tipo) {

        ModelAndView mv = new ModelAndView("login");
        BCryptPasswordEncoder bc = new BCryptPasswordEncoder();

        if (tipo.equalsIgnoreCase("voluntario")) {
            Voluntario voluntario = vr.findByEmail(email);
            if (voluntario != null && bc.matches(senha, voluntario.getSenha())) {
                mv.setViewName("homeVoluntario");
                return mv;
            }
        } else if (tipo.equalsIgnoreCase("ong")) {
            Ong ong = or.findByEmail(email);
            if (ong != null && bc.matches(senha, ong.getSenha())) {
                mv.setViewName("homeOng");
                return mv;
            }
        }

        return mv;
    }
}
