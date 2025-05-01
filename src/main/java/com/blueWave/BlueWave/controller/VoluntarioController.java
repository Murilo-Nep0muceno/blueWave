package com.blueWave.BlueWave.controller;

import com.blueWave.BlueWave.model.Voluntario;
import com.blueWave.BlueWave.repository.VoluntarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@RequestMapping("/voluntarioForm")
@RestController
public class VoluntarioController {

    @Autowired
    private VoluntarioRepository vr;

    @GetMapping
    public ModelAndView form(){
        ModelAndView mv = new ModelAndView("voluntarioForm");
        return mv;

    }


    @PostMapping()
    public  Voluntario submitForm(@RequestBody Voluntario voluntario){

        BCryptPasswordEncoder bcript = new BCryptPasswordEncoder();

        String senha = bcript.encode(voluntario.getSenha());
        voluntario.setSenha(senha);
        return vr.save(voluntario);

    }





}
