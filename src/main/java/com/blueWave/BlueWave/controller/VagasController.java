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
//        String nomeOng = ongRepository.findByEmail();
        mv.addObject("vagas", vagas);
        return mv;
    }


    @GetMapping("/homeOng")
    public ModelAndView cadastraVaga(){
        return new ModelAndView("homeOng");  // templates/homeOng.html
    }


    @PostMapping("/homeOng")
    public ResponseEntity<String> criarVaga(@RequestBody Vagas vaga) {
        vagaRepository.save(vaga);
        return ResponseEntity.ok("Vaga criada com sucesso!");
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vagas> updateVaga(
            @PathVariable Long id,
            @RequestBody Vagas novaVaga) {

        return vagaRepository.findById(id)
                .map(vaga -> {
                    vaga.setNome(novaVaga.getNome());
                    vaga.setDescri(novaVaga.getDescri());
                    vaga.setQuantidade(novaVaga.getQuantidade());
                    Vagas salva = vagaRepository.save(vaga);
                    return ResponseEntity.ok(salva);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVaga(@PathVariable Long id) {
        if (vagaRepository.existsById(id)) {
            vagaRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}