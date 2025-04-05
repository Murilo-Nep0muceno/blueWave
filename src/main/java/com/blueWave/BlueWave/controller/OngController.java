package com.blueWave.BlueWave.controller;

import com.blueWave.BlueWave.model.Ong;
import com.blueWave.BlueWave.repository.OngRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequestMapping("/ongForm")
public class OngController {

    @Autowired
    private OngRepository or;

    @GetMapping
    public ModelAndView form(){
        ModelAndView mv = new ModelAndView("ongForm");
        return mv;
    }

    @PostMapping
    public Ong ongForm(@RequestBody Ong ong){
        BCryptPasswordEncoder bc = new BCryptPasswordEncoder();
        String senha = bc.encode(ong.getSenha());
        ong.setSenha(senha);

        return or.save(ong);

    }
}
