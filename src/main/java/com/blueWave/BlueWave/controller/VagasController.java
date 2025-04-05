package com.blueWave.BlueWave.controller;

import com.blueWave.BlueWave.model.Ong;
import com.blueWave.BlueWave.model.Vagas;
import com.blueWave.BlueWave.repository.OngRepository;
import com.blueWave.BlueWave.repository.VagasRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/vagas")
public class VagasController {

    @Autowired
    private OngRepository ongRepository;

    @Autowired
    private VagasRepository vagaRepository;

    @GetMapping("/listaVaga")
    public ModelAndView listarVagas(Model model) {
        ModelAndView mv = new ModelAndView("listaVaga");
        List<Vagas> vagas = vagaRepository.findAll();
        mv.addObject("vagas", vagas);
        return mv;
    }

    @GetMapping("/homeOng")
    public ModelAndView cadastraVaga(){
        ModelAndView mv = new ModelAndView("homeOng");

        return mv;
    }

    @PostMapping("/homeOng")
    public ResponseEntity<String> criarVaga(@RequestBody Vagas vaga) {
        vagaRepository.save(vaga);
        return ResponseEntity.ok("Vaga criada com sucesso!");
    }
}