package com.blueWave.BlueWave.controller;

import com.blueWave.BlueWave.model.Inscricao;
import com.blueWave.BlueWave.model.Ong;
import com.blueWave.BlueWave.model.Vagas;
import com.blueWave.BlueWave.model.Voluntario;
import com.blueWave.BlueWave.repository.InscricaoRepository;
import com.blueWave.BlueWave.repository.OngRepository;
import com.blueWave.BlueWave.repository.VagasRepository;
import com.blueWave.BlueWave.repository.VoluntarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vagas")
public class ConfiguracoesController {

    @Autowired
    private OngRepository ongRepository;

    @Autowired
    private VagasRepository vagasRepository;

    @Autowired
    private InscricaoRepository inscricaoRepository;

    @Autowired
    private VoluntarioRepository voluntarioRepository;

    // Método para verificar se é ONG logada
    private boolean isOngLoggedIn(HttpSession session) {
        String userType = (String) session.getAttribute("userType");
        String userEmail = (String) session.getAttribute("userEmail");
        return "ong".equals(userType) && userEmail != null;
    }

    // Método para obter ONG logada
    private Ong getLoggedOng(HttpSession session) {
        if (!isOngLoggedIn(session)) {
            return null;
        }
        String userEmail = (String) session.getAttribute("userEmail");
        return ongRepository.findByEmail(userEmail);
    }

    @GetMapping("/configuracoes")
    public ModelAndView configuracoes(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String vagaFilter) {

        if (!isOngLoggedIn(session)) {
            return new ModelAndView("redirect:/login/ong");
        }

        Ong ong = getLoggedOng(session);
        if (ong == null) {
            return new ModelAndView("redirect:/login/ong");
        }

        // Buscar vagas da ONG
        List<Vagas> vagasOng = vagasRepository.findByOngId(ong.getId());

        // Criar Pageable para paginação
        Pageable pageable = PageRequest.of(page, size, Sort.by("dataInscricao").descending());

        // Buscar inscrições com filtros
        Page<Inscricao> inscricoesPage;

        if (!search.isEmpty() && !vagaFilter.isEmpty()) {
            // Filtro por nome do voluntário e vaga específica
            Long vagaId = Long.parseLong(vagaFilter);
            inscricoesPage = inscricaoRepository.findByVagaIdAndVoluntarioNomeVoluntarioContainingIgnoreCase(
                    vagaId, search, pageable);
        } else if (!search.isEmpty()) {
            // Filtro apenas por nome do voluntário
            List<Long> vagaIds = vagasOng.stream().map(Vagas::getId).collect(Collectors.toList());
            inscricoesPage = inscricaoRepository.findByVagaIdInAndVoluntarioNomeVoluntarioContainingIgnoreCase(
                    vagaIds, search, pageable);
        } else if (!vagaFilter.isEmpty()) {
            // Filtro apenas por vaga específica
            Long vagaId = Long.parseLong(vagaFilter);
            inscricoesPage = inscricaoRepository.findByVagaId(vagaId, pageable);
        } else {
            // Todas as inscrições das vagas da ONG
            List<Long> vagaIds = vagasOng.stream().map(Vagas::getId).collect(Collectors.toList());
            if (vagaIds.isEmpty()) {
                // Se não há vagas, criar página vazia
                inscricoesPage = Page.empty(pageable);
            } else {
                inscricoesPage = inscricaoRepository.findByVagaIdIn(vagaIds, pageable);
            }
        }

        // Estatísticas
        Map<String, Object> estatisticas = calcularEstatisticas(ong);

        ModelAndView mv = new ModelAndView("configuracoes");
        mv.addObject("ong", ong);
        mv.addObject("inscricoes", inscricoesPage.getContent());
        mv.addObject("currentPage", page);
        mv.addObject("totalPages", inscricoesPage.getTotalPages());
        mv.addObject("totalElements", inscricoesPage.getTotalElements());
        mv.addObject("size", size);
        mv.addObject("search", search);
        mv.addObject("vagaFilter", vagaFilter);
        mv.addObject("vagas", vagasOng);
        mv.addObject("estatisticas", estatisticas);
        mv.addObject("hasNext", inscricoesPage.hasNext());
        mv.addObject("hasPrevious", inscricoesPage.hasPrevious());

        return mv;
    }

    @PostMapping("/configuracoes/remover-voluntario/{inscricaoId}")
    @ResponseBody
    public String removerVoluntario(@PathVariable Long inscricaoId, HttpSession session) {
        if (!isOngLoggedIn(session)) {
            return "erro: não autenticado";
        }

        Ong ong = getLoggedOng(session);
        if (ong == null) {
            return "erro: usuário não encontrado";
        }

        try {
            Inscricao inscricao = inscricaoRepository.findById(inscricaoId)
                    .orElseThrow(() -> new IllegalArgumentException("Inscrição não encontrada"));

            // Verifica se a vaga pertence à ONG logada
            if (!inscricao.getVaga().getOng().getId().equals(ong.getId())) {
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
            return "erro: falha ao remover voluntário";
        }
    }

    @GetMapping("/configuracoes/detalhes-voluntario/{voluntarioId}")
    @ResponseBody
    public Map<String, Object> detalhesVoluntario(@PathVariable Long voluntarioId, HttpSession session) {
        if (!isOngLoggedIn(session)) {
            return Map.of("erro", "não autenticado");
        }

        try {
            Voluntario voluntario = voluntarioRepository.findById(voluntarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Voluntário não encontrado"));

            return Map.of(
                    "nome", voluntario.getNomeVoluntario(),
                    "email", voluntario.getEmail(),
                    "telefone", voluntario.getTelefone() != null ? formatTelefone(voluntario.getTelefone()) : "Não informado",
                    "cpf", voluntario.getCpf() != null ? formatCpf(voluntario.getCpf()) : "Não informado",
                    "dataNascimento", voluntario.getDataNascimento() != null ? voluntario.getDataNascimento() : "Não informada",
                    "sexo", voluntario.getSexo() != null ? voluntario.getSexo() : "Não informado"
            );
        } catch (Exception e) {
            return Map.of("erro", "Voluntário não encontrado");
        }
    }

    // Método auxiliar para formatar telefone
    private String formatTelefone(String telefone) {
        if (telefone == null || telefone.isEmpty()) {
            return "Não informado";
        }

        // Remove caracteres não numéricos
        String cleaned = telefone.replaceAll("\\D", "");

        // Formata de acordo com o tamanho
        if (cleaned.length() == 11) {
            return String.format("(%s) %s-%s",
                    cleaned.substring(0, 2),
                    cleaned.substring(2, 7),
                    cleaned.substring(7));
        } else if (cleaned.length() == 10) {
            return String.format("(%s) %s-%s",
                    cleaned.substring(0, 2),
                    cleaned.substring(2, 6),
                    cleaned.substring(6));
        }

        return telefone;
    }

    // Método auxiliar para formatar CPF
    private String formatCpf(String cpf) {
        if (cpf == null || cpf.isEmpty()) {
            return "Não informado";
        }

        // Remove caracteres não numéricos
        String cleaned = cpf.replaceAll("\\D", "");

        // Formata CPF
        if (cleaned.length() == 11) {
            return String.format("%s.%s.%s-%s",
                    cleaned.substring(0, 3),
                    cleaned.substring(3, 6),
                    cleaned.substring(6, 9),
                    cleaned.substring(9));
        }

        return cpf;
    }

    @PostMapping("/configuracoes/remover-multiplos")
    @ResponseBody
    public String removerMultiplosVoluntarios(@RequestBody List<Long> inscricaoIds, HttpSession session) {
        if (!isOngLoggedIn(session)) {
            return "erro: não autenticado";
        }

        Ong ong = getLoggedOng(session);
        if (ong == null) {
            return "erro: usuário não encontrado";
        }

        try {
            int removidos = 0;
            for (Long inscricaoId : inscricaoIds) {
                Inscricao inscricao = inscricaoRepository.findById(inscricaoId).orElse(null);

                if (inscricao != null && inscricao.getVaga().getOng().getId().equals(ong.getId())) {
                    // Aumenta a quantidade de vagas disponíveis
                    Vagas vaga = inscricao.getVaga();
                    vaga.setQuantidade(vaga.getQuantidade() + 1);

                    // Remove a inscrição e atualiza a vaga
                    inscricaoRepository.delete(inscricao);
                    vagasRepository.save(vaga);

                    removidos++;
                }
            }

            return "ok: " + removidos + " voluntários removidos";
        } catch (Exception e) {
            e.printStackTrace();
            return "erro: falha ao remover voluntários";
        }
    }

    private Map<String, Object> calcularEstatisticas(Ong ong) {
        List<Vagas> vagasOng = vagasRepository.findByOngId(ong.getId());
        List<Long> vagaIds = vagasOng.stream().map(Vagas::getId).collect(Collectors.toList());

        long totalInscricoes = inscricaoRepository.countByVagaIdIn(vagaIds);
        long totalVagas = vagasOng.size();
        long vagasComInscricoes = vagasOng.stream()
                .mapToLong(vaga -> inscricaoRepository.countByVagaId(vaga.getId()))
                .filter(count -> count > 0)
                .count();

        // Voluntários únicos
        List<Inscricao> todasInscricoes = inscricaoRepository.findByVagaIdIn(vagaIds);
        long voluntariosUnicos = todasInscricoes.stream()
                .map(inscricao -> inscricao.getVoluntario().getId())
                .distinct()
                .count();

        return Map.of(
                "totalInscricoes", totalInscricoes,
                "totalVagas", totalVagas,
                "vagasComInscricoes", vagasComInscricoes,
                "voluntariosUnicos", voluntariosUnicos
        );
    }
}