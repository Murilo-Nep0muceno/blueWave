package com.blueWave.BlueWave.controller;
import com.blueWave.BlueWave.model.Inscricao;
import com.blueWave.BlueWave.model.Vagas;
import com.blueWave.BlueWave.model.Voluntario;
import com.blueWave.BlueWave.repository.InscricaoRepository;
import com.blueWave.BlueWave.repository.VagasRepository;
import com.blueWave.BlueWave.repository.VoluntarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/inscricao")
@SessionAttributes("userEmail")
public class inscricaoController {


    @Autowired private VagasRepository vagasRepo;
    @Autowired private VoluntarioRepository voluntarioRepo;
    @Autowired private InscricaoRepository inscricaoRepo;

    @ModelAttribute("userEmail")
    public String userEmail() { return null; }

    @GetMapping("/vagasVoluntario")
    public ModelAndView voluntarioCad(@ModelAttribute("userEmail") String email) {
        // se email for null, redireciona para login
        if (email == null) {
            return new ModelAndView("redirect:/inscricao/login");
        }
        List<Vagas> vagas = vagasRepo.findAll();
        ModelAndView mv = new ModelAndView("listaVagaVoluntario");
        Voluntario vl = voluntarioRepo.findByEmail(email);
        mv.addObject("nome" ,vl.getNomeVoluntario());
        mv.addObject("vagas", vagas);
        return mv;
    }

    @PostMapping("/inscrever")
    @ResponseBody
    public String inscreverVaga(
            @RequestParam Long vagaId,
            @ModelAttribute("userEmail") String email) {

        if (email == null) {
            return "erro: não autenticado";
        }
        Voluntario vol = voluntarioRepo.findByEmail(email);
        if (vol == null) {
            return "erro: usuário não encontrado";
        }

        // busca a vaga
        Vagas vaga = vagasRepo.findById(vagaId)
                .orElseThrow(() -> new IllegalArgumentException("Vaga não encontrada"));

        // evita inscrição duplicada na mesma vaga
        boolean ja = inscricaoRepo.existsByVoluntarioAndVaga(vol, vaga);
        if (ja) {
            return "já inscrito";
        }

        // evita inscrição em duas vagas com a mesma data
        boolean conflitoData = inscricaoRepo.findByVoluntario(vol).stream()
                .map(Inscricao::getVaga)
                .map(Vagas::getData)
                .anyMatch(d -> d.equals(vaga.getData()));
        if (conflitoData) {
            return "erro: já inscrito em vaga nesta data";
        }

        // cria inscrição
        Inscricao insc = new Inscricao();
        insc.setVoluntario(vol);
        vaga.setQuantidade(vaga.getQuantidade() - 1);
        insc.setVaga(vaga);
        insc.setDataInscricao(LocalDate.now());
        inscricaoRepo.save(insc);
        return "ok";
    }


    @GetMapping("/minhasVagas")
    public ModelAndView verMinhasInscricoes(@ModelAttribute("userEmail") String email) {
        Voluntario vol = voluntarioRepo.findByEmail(email);
        List<Inscricao> inscricoes = inscricaoRepo.findByVoluntario(vol);

        ModelAndView mv = new ModelAndView("minhasInscricoes");
        mv.addObject("nome", vol.getNomeVoluntario());
        mv.addObject("inscricoes", inscricoes);
        return mv;
    }
}
