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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDate;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    @Transactional(readOnly = true)
    public ModelAndView listarVagasDisponiveis(HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return new ModelAndView("redirect:/login/voluntario");
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return new ModelAndView("redirect:/login/voluntario");
        }

        try {
            // Buscar vagas ativas com filtros aplicados
            List<Vagas> todasVagas = vagasRepository.findActiveVagasWithOng(LocalDate.now());

            // Filtrar vagas:
            // 1. Não mostrar vagas interrompidas (já filtrado no repositório)
            // 2. Não mostrar vagas que já passaram da data
            // 3. Não mostrar vagas onde o voluntário já está inscrito
            List<Inscricao> inscricoesVoluntario = inscricaoRepository.findByVoluntario(voluntario);
            Set<Long> vagasInscritasIds = inscricoesVoluntario.stream()
                    .map(inscricao -> inscricao.getVaga().getId())
                    .collect(Collectors.toSet());

            List<Vagas> vagasDisponiveis = todasVagas.stream()
                    .filter(vaga ->
                            // Não mostrar vagas expiradas
                            !vaga.getData().isBefore(LocalDate.now()) &&
                                    // Não mostrar vagas onde já está inscrito
                                    !vagasInscritasIds.contains(vaga.getId()) &&
                                    // Apenas vagas ativas (já filtrado no repositório)
                                    vaga.getStatus() == Vagas.StatusVaga.ATIVA
                    )
                    .collect(Collectors.toList());

            ModelAndView mv = new ModelAndView("listaVagaVoluntario");
            mv.addObject("nome", voluntario.getNomeVoluntario());
            mv.addObject("vagas", vagasDisponiveis);

            return mv;
        } catch (Exception e) {
            e.printStackTrace();
            ModelAndView mv = new ModelAndView("listaVagaVoluntario");
            mv.addObject("nome", voluntario.getNomeVoluntario());
            mv.addObject("vagas", List.of());
            mv.addObject("erro", "Erro ao carregar vagas: " + e.getMessage());
            return mv;
        }
    }
    @PostMapping("/inscrever")
    @ResponseBody
    @Transactional
    public String inscreverVaga(@RequestParam Long vagaId, HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return "erro: não autenticado";
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return "erro: usuário não encontrado";
        }

        try {
            // Busca a vaga
            Vagas vaga = vagasRepository.findById(vagaId)
                    .orElse(null);

            if (vaga == null) {
                return "erro: vaga não encontrada";
            }

            // Verifica se a vaga ainda está ativa (data não passou)
            if (vaga.getData().isBefore(LocalDate.now())) {
                return "erro: esta vaga já expirou";
            }

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
            List<Inscricao> inscricoesVoluntario = inscricaoRepository.findByVoluntario(voluntario);
            boolean conflitoData = inscricoesVoluntario.stream()
                    .anyMatch(inscricao -> inscricao.getVaga().getData().equals(vaga.getData()));

            if (conflitoData) {
                return "erro: já inscrito em vaga nesta data";
            }

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
            return "erro: falha ao processar inscrição - " + e.getMessage();
        }
    }

    @GetMapping("/minhasVagas")
    @Transactional(readOnly = true)
    public ModelAndView verMinhasInscricoes(HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return new ModelAndView("redirect:/login/voluntario");
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return new ModelAndView("redirect:/login/voluntario");
        }

        try {
            // Buscar todas as inscrições do voluntário com EAGER fetch
            List<Inscricao> inscricoes = inscricaoRepository.findByVoluntario(voluntario);

            // Log para debug
            System.out.println("Inscrições encontradas: " + inscricoes.size());
            for (Inscricao inscricao : inscricoes) {
                System.out.println("Inscrição: " + inscricao.getId() +
                        " - Vaga: " + (inscricao.getVaga() != null ? inscricao.getVaga().getNome() : "null") +
                        " - ONG: " + (inscricao.getVaga() != null && inscricao.getVaga().getOng() != null ?
                        inscricao.getVaga().getOng().getNome() : "null"));
            }

            ModelAndView mv = new ModelAndView("minhasInscricoes");
            mv.addObject("nome", voluntario.getNomeVoluntario());
            mv.addObject("inscricoes", inscricoes);

            // Adicionar estatísticas
            long vagasAtivas = inscricoes.stream()
                    .filter(inscricao -> inscricao.getVaga().getData().isAfter(LocalDate.now()) ||
                            inscricao.getVaga().getData().equals(LocalDate.now()))
                    .count();

            long vagasEncerradas = inscricoes.size() - vagasAtivas;

            mv.addObject("totalInscricoes", inscricoes.size());
            mv.addObject("vagasAtivas", vagasAtivas);
            mv.addObject("vagasEncerradas", vagasEncerradas);

            return mv;
        } catch (Exception e) {
            e.printStackTrace();
            ModelAndView mv = new ModelAndView("minhasInscricoes");
            mv.addObject("nome", voluntario.getNomeVoluntario());
            mv.addObject("inscricoes", List.of());
            mv.addObject("erro", "Erro ao carregar inscrições: " + e.getMessage());
            return mv;
        }
    }

    @PostMapping("/cancelar/{inscricaoId}")
    @ResponseBody
    @Transactional
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
                    .orElse(null);

            if (inscricao == null) {
                return "erro: inscrição não encontrada";
            }

            // Verifica se a inscrição pertence ao voluntário logado
            if (!inscricao.getVoluntario().getId().equals(voluntario.getId())) {
                return "erro: não autorizado";
            }

            // Verifica se ainda é possível cancelar (vaga não aconteceu ainda)
            Vagas vaga = inscricao.getVaga();
            if (vaga.getData().isBefore(LocalDate.now())) {
                return "erro: não é possível cancelar inscrição de vaga que já aconteceu";
            }

            // Aumenta a quantidade de vagas disponíveis
            vaga.setQuantidade(vaga.getQuantidade() + 1);

            // Remove a inscrição e atualiza a vaga
            inscricaoRepository.delete(inscricao);
            vagasRepository.save(vaga);

            return "ok";
        } catch (Exception e) {
            e.printStackTrace();
            return "erro: falha ao cancelar inscrição - " + e.getMessage();
        }
    }

    // Endpoint para verificar se o voluntário já está inscrito em uma vaga
    @GetMapping("/verificar/{vagaId}")
    @ResponseBody
    public Map<String, Object> verificarInscricao(@PathVariable Long vagaId, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        if (!isVoluntarioLoggedIn(session)) {
            response.put("inscrito", false);
            response.put("erro", "não autenticado");
            return response;
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            response.put("inscrito", false);
            response.put("erro", "usuário não encontrado");
            return response;
        }

        try {
            Vagas vaga = vagasRepository.findById(vagaId).orElse(null);
            if (vaga == null) {
                response.put("inscrito", false);
                response.put("erro", "vaga não encontrada");
                return response;
            }

            boolean inscrito = inscricaoRepository.existsByVoluntarioAndVaga(voluntario, vaga);
            response.put("inscrito", inscrito);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            response.put("inscrito", false);
            response.put("erro", "falha ao verificar inscrição");
            return response;
        }
    }

    // Endpoint para estatísticas do voluntário
    @GetMapping("/estatisticas")
    @ResponseBody
    public Map<String, Object> obterEstatisticas(HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        if (!isVoluntarioLoggedIn(session)) {
            response.put("erro", "não autenticado");
            return response;
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            response.put("erro", "usuário não encontrado");
            return response;
        }

        try {
            List<Inscricao> inscricoes = inscricaoRepository.findByVoluntario(voluntario);

            long total = inscricoes.size();
            long ativas = inscricoes.stream()
                    .filter(inscricao -> inscricao.getVaga().getData().isAfter(LocalDate.now()) ||
                            inscricao.getVaga().getData().equals(LocalDate.now()))
                    .count();
            long encerradas = total - ativas;
            long hoje = inscricoes.stream()
                    .filter(inscricao -> inscricao.getVaga().getData().equals(LocalDate.now()))
                    .count();

            response.put("total", total);
            response.put("ativas", ativas);
            response.put("encerradas", encerradas);
            response.put("hoje", hoje);

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            response.put("erro", "falha ao obter estatísticas");
            return response;
        }
    }

    // Endpoint para buscar detalhes de uma inscrição específica
    @GetMapping("/detalhes/{inscricaoId}")
    public ModelAndView detalhesInscricao(@PathVariable Long inscricaoId, HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return new ModelAndView("redirect:/login/voluntario");
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return new ModelAndView("redirect:/login/voluntario");
        }

        Inscricao inscricao = inscricaoRepository.findById(inscricaoId)
                .orElse(null);

        if (inscricao == null || !inscricao.getVoluntario().getId().equals(voluntario.getId())) {
            return new ModelAndView("redirect:/inscricao/minhasVagas");
        }

        ModelAndView mv = new ModelAndView("detalhesInscricao");
        mv.addObject("inscricao", inscricao);
        mv.addObject("nome", voluntario.getNomeVoluntario());

        return mv;
    }
}