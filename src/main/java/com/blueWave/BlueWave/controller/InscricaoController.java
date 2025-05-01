package com.blueWave.BlueWave.controller;

import com.blueWave.BlueWave.model.Inscricao;
import com.blueWave.BlueWave.model.Vagas;
import com.blueWave.BlueWave.model.Voluntario;
import com.blueWave.BlueWave.repository.InscricaoRepository;
import com.blueWave.BlueWave.repository.VagasRepository;
import com.blueWave.BlueWave.repository.VoluntarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/inscricao")
public class InscricaoController {

    @Autowired
    private VagasRepository vagasRepository;

    @Autowired
    private VoluntarioRepository voluntarioRepository;

    @Autowired
    private InscricaoRepository inscricaoRepository;

    // Método para verificar se é voluntário logado
    private boolean isVoluntarioLoggedIn(HttpSession session) {
        String userType = (String) session.getAttribute("userType");
        String userEmail = (String) session.getAttribute("userEmail");
        return "voluntario".equals(userType) && userEmail != null;
    }

    // Método para obter voluntário logado
    private Voluntario getLoggedVoluntario(HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return null;
        }
        String userEmail = (String) session.getAttribute("userEmail");
        return voluntarioRepository.findByEmail(userEmail);
    }

    @GetMapping("/vagasVoluntario")
    public ModelAndView voluntarioCad(HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return new ModelAndView("redirect:/login/voluntario");
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return new ModelAndView("redirect:/login/voluntario");
        }

        List<Vagas> vagas = vagasRepository.findAll();
        ModelAndView mv = new ModelAndView("listaVagaVoluntario");
        mv.addObject("nome", voluntario.getNomeVoluntario());
        mv.addObject("vagas", vagas);
        return mv;
    }

    @PostMapping("/inscrever")
    @ResponseBody
    public String inscreverVaga(@RequestParam Long vagaId, HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return "erro: não autenticado";
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return "erro: usuário não encontrado";
        }

        // Busca a vaga
        Vagas vaga = vagasRepository.findById(vagaId)
                .orElseThrow(() -> new IllegalArgumentException("Vaga não encontrada"));

        // Verifica se há vagas disponíveis
        if (vaga.getQuantidade() <= 0) {
            return "erro: não há vagas disponíveis";
        }

        // Evita inscrição duplicada na mesma vaga
        boolean jaInscrito = inscricaoRepository.existsByVoluntarioAndVaga(voluntario, vaga);
        if (jaInscrito) {
            return "já inscrito";
        }

        // Evita inscrição em duas vagas com a mesma data
        boolean conflitoData = inscricaoRepository.findByVoluntario(voluntario).stream()
                .map(Inscricao::getVaga)
                .map(Vagas::getData)
                .anyMatch(data -> data.equals(vaga.getData()));
        if (conflitoData) {
            return "erro: já inscrito em vaga nesta data";
        }

        try {
            // Cria inscrição
            Inscricao inscricao = new Inscricao();
            inscricao.setVoluntario(voluntario);
            inscricao.setVaga(vaga);
            inscricao.setDataInscricao(LocalDate.now());

            // Reduz a quantidade de vagas disponíveis
            vaga.setQuantidade(vaga.getQuantidade() - 1);

            // Salva a inscrição e atualiza a vaga
            inscricaoRepository.save(inscricao);
            vagasRepository.save(vaga);

            return "ok";
        } catch (Exception e) {
            e.printStackTrace();
            return "erro: falha ao processar inscrição";
        }
    }

    @GetMapping("/minhasVagas")
    public ModelAndView verMinhasInscricoes(HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return new ModelAndView("redirect:/login/voluntario");
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return new ModelAndView("redirect:/login/voluntario");
        }

        List<Inscricao> inscricoes = inscricaoRepository.findByVoluntario(voluntario);

        ModelAndView mv = new ModelAndView("minhasInscricoes");
        mv.addObject("nome", voluntario.getNomeVoluntario());
        mv.addObject("inscricoes", inscricoes);
        return mv;
    }

    @PostMapping("/cancelar/{inscricaoId}")
    @ResponseBody
    public String cancelarInscricao(@PathVariable Long inscricaoId, HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return "erro: não autenticado";
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return "erro: usuário não encontrado";
        }

        try {
            Inscricao inscricao = inscricaoRepository.findById(inscricaoId)
                    .orElseThrow(() -> new IllegalArgumentException("Inscrição não encontrada"));

            // Verifica se a inscrição pertence ao voluntário logado
            if (!inscricao.getVoluntario().getId().equals(voluntario.getId())) {
                return "erro: não autorizado";
            }

            // Aumenta a quantidade de vagas disponíveis
            Vagas vaga = inscricao.getVaga();
            vaga.setQuantidade(vaga.getQuantidade() + 1);

            // Remove a inscrição e atualiza a vaga
            inscricaoRepository.delete(inscricao);
            vagasRepository.save(vaga);

            return "ok";
        } catch (Exception e) {
            e.printStackTrace();
            return "erro: falha ao cancelar inscrição";
        }
    }
}