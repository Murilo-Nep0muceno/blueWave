package com.blueWave.BlueWave.controller;

import com.blueWave.BlueWave.model.Inscricao;
import com.blueWave.BlueWave.model.Report;
import com.blueWave.BlueWave.model.Vagas;
import com.blueWave.BlueWave.model.Voluntario;
import com.blueWave.BlueWave.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/inscricao")
public class InscricaoController {

    @Autowired
    private OngRepository ongRepository;

    @Autowired
    private ReportRepository reportRepository;

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
            // Buscar vagas ativas
            List<Vagas> todasVagas = vagasRepository.findAll().stream()
                    .filter(vaga ->
                            vaga.getData() != null &&
                                    !vaga.getData().isBefore(LocalDate.now()) &&
                                    vaga.getStatus() == Vagas.StatusVaga.ATIVA &&
                                    vaga.getQuantidade() > 0
                    )
                    .collect(Collectors.toList());

            // Filtrar vagas onde o voluntário já está inscrito
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
                                    // Apenas vagas ativas
                                    vaga.getStatus() == Vagas.StatusVaga.ATIVA &&
                                    // Vagas com quantidade disponível
                                    vaga.getQuantidade() > 0
                    )
                    .collect(Collectors.toList());

            ModelAndView mv = new ModelAndView("listaVagaVoluntario");
            mv.addObject("voluntario", voluntario);
            mv.addObject("vagas", vagasDisponiveis);
            mv.addObject("totalVagas", vagasDisponiveis.size());

            return mv;
        } catch (Exception e) {
            e.printStackTrace();
            ModelAndView mv = new ModelAndView("listaVagaVoluntario");
            mv.addObject("voluntario", voluntario);
            mv.addObject("vagas", Collections.emptyList());
            mv.addObject("totalVagas", 0);
            mv.addObject("erro", "Erro ao carregar vagas: " + e.getMessage());
            return mv;
        }
    }

    // API endpoint para listar vagas (para AJAX)
    @GetMapping("/api/vagas-disponiveis")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarVagasDisponiveisApi(HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return ResponseEntity.status(401).build();
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return ResponseEntity.status(404).build();
        }

        try {
            List<Vagas> todasVagas = vagasRepository.findAll().stream()
                    .filter(vaga ->
                            vaga.getData() != null &&
                                    !vaga.getData().isBefore(LocalDate.now()) &&
                                    vaga.getStatus() == Vagas.StatusVaga.ATIVA &&
                                    vaga.getQuantidade() > 0
                    )
                    .collect(Collectors.toList());

            List<Inscricao> inscricoesVoluntario = inscricaoRepository.findByVoluntario(voluntario);
            Set<Long> vagasInscritasIds = inscricoesVoluntario.stream()
                    .map(inscricao -> inscricao.getVaga().getId())
                    .collect(Collectors.toSet());

            List<Map<String, Object>> vagasDisponiveis = todasVagas.stream()
                    .filter(vaga ->
                            !vaga.getData().isBefore(LocalDate.now()) &&
                                    !vagasInscritasIds.contains(vaga.getId()) &&
                                    vaga.getStatus() == Vagas.StatusVaga.ATIVA &&
                                    vaga.getQuantidade() > 0
                    )
                    .map(vaga -> {
                        Map<String, Object> vagaMap = new HashMap<>();
                        vagaMap.put("id", vaga.getId());
                        vagaMap.put("nome", vaga.getNome());
                        vagaMap.put("descricao", vaga.getDescri());
                        vagaMap.put("data", vaga.getData().toString());
                        vagaMap.put("local", obterLocalVaga(vaga));
                        vagaMap.put("localReferencia", vaga.getLocalReferencia());
                        vagaMap.put("quantidade", vaga.getQuantidade());
                        vagaMap.put("ongNome", vaga.getOng().getNome());
                        vagaMap.put("ongId", vaga.getOng().getId());
                        return vagaMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("vagas", vagasDisponiveis);
            response.put("total", vagasDisponiveis.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/inscrever")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, String>> inscreverVaga(@RequestParam Long vagaId, HttpSession session) {
        Map<String, String> response = new HashMap<>();

        if (!isVoluntarioLoggedIn(session)) {
            response.put("status", "erro");
            response.put("message", "Não autenticado");
            return ResponseEntity.status(401).body(response);
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            response.put("status", "erro");
            response.put("message", "Usuário não encontrado");
            return ResponseEntity.status(404).body(response);
        }

        try {
            // Busca a vaga
            Vagas vaga = vagasRepository.findById(vagaId).orElse(null);
            if (vaga == null) {
                response.put("status", "erro");
                response.put("message", "Vaga não encontrada");
                return ResponseEntity.badRequest().body(response);
            }

            // Verificações de negócio
            if (vaga.getData().isBefore(LocalDate.now())) {
                response.put("status", "erro");
                response.put("message", "Esta vaga já expirou");
                return ResponseEntity.badRequest().body(response);
            }

            if (vaga.getQuantidade() <= 0) {
                response.put("status", "erro");
                response.put("message", "Não há vagas disponíveis");
                return ResponseEntity.badRequest().body(response);
            }

            if (vaga.getStatus() != Vagas.StatusVaga.ATIVA) {
                response.put("status", "erro");
                response.put("message", "Esta vaga não está mais ativa");
                return ResponseEntity.badRequest().body(response);
            }

            // Evita inscrição duplicada
            boolean jaInscrito = inscricaoRepository.existsByVoluntarioAndVaga(voluntario, vaga);
            if (jaInscrito) {
                response.put("status", "erro");
                response.put("message", "Você já está inscrito nesta vaga");
                return ResponseEntity.badRequest().body(response);
            }

            // Evita conflito de data
            List<Inscricao> inscricoesVoluntario = inscricaoRepository.findByVoluntario(voluntario);
            boolean conflitoData = inscricoesVoluntario.stream()
                    .anyMatch(inscricao -> inscricao.getVaga().getData().equals(vaga.getData()));

            if (conflitoData) {
                response.put("status", "erro");
                response.put("message", "Você já tem uma inscrição para esta data");
                return ResponseEntity.badRequest().body(response);
            }

            // Criar inscrição
            Inscricao inscricao = new Inscricao();
            inscricao.setVoluntario(voluntario);
            inscricao.setVaga(vaga);
            inscricao.setDataInscricao(LocalDate.now());

            // Reduzir quantidade disponível
            vaga.setQuantidade(vaga.getQuantidade() - 1);

            // Salvar
            inscricaoRepository.save(inscricao);
            vagasRepository.save(vaga);

            response.put("status", "sucesso");
            response.put("message", "Inscrição realizada com sucesso!");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "erro");
            response.put("message", "Erro interno do servidor");
            return ResponseEntity.status(500).body(response);
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

        ModelAndView mv = new ModelAndView("minhasInscricoes");

        try {
            List<Inscricao> inscricoes = inscricaoRepository.findByVoluntario(voluntario);

            // Separar por status
            LocalDate hoje = LocalDate.now();
            List<Inscricao> vagasAtivas = inscricoes.stream()
                    .filter(inscricao -> inscricao.getVaga().getData().isAfter(hoje) ||
                            inscricao.getVaga().getData().equals(hoje))
                    .collect(Collectors.toList());

            List<Inscricao> vagasEncerradas = inscricoes.stream()
                    .filter(inscricao -> inscricao.getVaga().getData().isBefore(hoje))
                    .collect(Collectors.toList());

            mv.addObject("voluntario", voluntario);
            mv.addObject("inscricoes", inscricoes);
            mv.addObject("vagasAtivas", vagasAtivas);
            mv.addObject("vagasEncerradas", vagasEncerradas);
            mv.addObject("totalInscricoes", inscricoes.size());

        } catch (Exception e) {
            e.printStackTrace();
            mv.addObject("voluntario", voluntario);
            mv.addObject("inscricoes", Collections.emptyList());
            mv.addObject("vagasAtivas", Collections.emptyList());
            mv.addObject("vagasEncerradas", Collections.emptyList());
            mv.addObject("totalInscricoes", 0);
            mv.addObject("erro", "Erro ao carregar inscrições: " + e.getMessage());
        }

        return mv;
    }

    // API endpoint para minhas inscrições
    @GetMapping("/api/minhas-inscricoes")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> minhasInscricoesApi(HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return ResponseEntity.status(401).build();
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return ResponseEntity.status(404).build();
        }

        try {
            List<Inscricao> inscricoes = inscricaoRepository.findByVoluntarioWithVagaAndOng(voluntario);

            List<Map<String, Object>> inscricoesMap = inscricoes.stream()
                    .map(inscricao -> {
                        Map<String, Object> inscricaoMap = new HashMap<>();
                        inscricaoMap.put("id", inscricao.getId());
                        inscricaoMap.put("dataInscricao", inscricao.getDataInscricao().toString());

                        Map<String, Object> vagaMap = new HashMap<>();
                        if (inscricao.getVaga() != null) {
                            vagaMap.put("id", inscricao.getVaga().getId());
                            vagaMap.put("nome", inscricao.getVaga().getNome());
                            vagaMap.put("descricao", inscricao.getVaga().getDescri());
                            vagaMap.put("data", inscricao.getVaga().getData() != null ?
                                    inscricao.getVaga().getData().toString() : "");
                            vagaMap.put("local", obterLocalVaga(inscricao.getVaga()));
                            vagaMap.put("localReferencia", inscricao.getVaga().getLocalReferencia());
                            vagaMap.put("status", inscricao.getVaga().getStatus() != null ?
                                    inscricao.getVaga().getStatus().toString() : "");
                        } else {
                            vagaMap.put("id", null);
                            vagaMap.put("nome", "Vaga não disponível");
                            vagaMap.put("descricao", "");
                            vagaMap.put("data", "");
                            vagaMap.put("local", "");
                            vagaMap.put("localReferencia", "");
                            vagaMap.put("status", "");
                        }

                        Map<String, Object> ongMap = new HashMap<>();
                        if (inscricao.getVaga() != null && inscricao.getVaga().getOng() != null) {
                            ongMap.put("id", inscricao.getVaga().getOng().getId());
                            ongMap.put("nome", inscricao.getVaga().getOng().getNome());
                        } else {
                            ongMap.put("id", null);
                            ongMap.put("nome", "ONG não informada");
                        }

                        vagaMap.put("ong", ongMap);
                        inscricaoMap.put("vaga", vagaMap);

                        // Determinar se pode cancelar (apenas vagas futuras)
                        boolean podeCancelar = inscricao.getVaga() != null &&
                                inscricao.getVaga().getData() != null &&
                                (inscricao.getVaga().getData().isAfter(LocalDate.now()) ||
                                        inscricao.getVaga().getData().equals(LocalDate.now()));
                        inscricaoMap.put("podeCancelar", podeCancelar);

                        return inscricaoMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("inscricoes", inscricoesMap);
            response.put("total", inscricoesMap.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/cancelar/{inscricaoId}")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, String>> cancelarInscricao(@PathVariable Long inscricaoId, HttpSession session) {
        Map<String, String> response = new HashMap<>();

        if (!isVoluntarioLoggedIn(session)) {
            response.put("status", "erro");
            response.put("message", "Não autenticado");
            return ResponseEntity.status(401).body(response);
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            response.put("status", "erro");
            response.put("message", "Usuário não encontrado");
            return ResponseEntity.status(404).body(response);
        }

        try {
            Inscricao inscricao = inscricaoRepository.findById(inscricaoId).orElse(null);
            if (inscricao == null) {
                response.put("status", "erro");
                response.put("message", "Inscrição não encontrada");
                return ResponseEntity.badRequest().body(response);
            }

            // Verificar se a inscrição pertence ao voluntário logado
            if (!inscricao.getVoluntario().getId().equals(voluntario.getId())) {
                response.put("status", "erro");
                response.put("message", "Não autorizado");
                return ResponseEntity.status(403).body(response);
            }

            // Verificar se ainda é possível cancelar
            Vagas vaga = inscricao.getVaga();
            if (vaga.getData().isBefore(LocalDate.now())) {
                response.put("status", "erro");
                response.put("message", "Não é possível cancelar inscrição de vaga que já aconteceu");
                return ResponseEntity.badRequest().body(response);
            }

            // Aumentar a quantidade de vagas disponíveis
            vaga.setQuantidade(vaga.getQuantidade() + 1);

            // Remover a inscrição e atualizar a vaga
            inscricaoRepository.delete(inscricao);
            vagasRepository.save(vaga);

            response.put("status", "sucesso");
            response.put("message", "Inscrição cancelada com sucesso!");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "erro");
            response.put("message", "Erro interno do servidor");
            return ResponseEntity.status(500).body(response);
        }
    }

    // Endpoint para verificar se o voluntário já está inscrito em uma vaga
    @GetMapping("/verificar/{vagaId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verificarInscricao(@PathVariable Long vagaId, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        if (!isVoluntarioLoggedIn(session)) {
            response.put("inscrito", false);
            response.put("erro", "não autenticado");
            return ResponseEntity.status(401).body(response);
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            response.put("inscrito", false);
            response.put("erro", "usuário não encontrado");
            return ResponseEntity.status(404).body(response);
        }

        try {
            Vagas vaga = vagasRepository.findById(vagaId).orElse(null);
            if (vaga == null) {
                response.put("inscrito", false);
                response.put("erro", "vaga não encontrada");
                return ResponseEntity.badRequest().body(response);
            }

            boolean inscrito = inscricaoRepository.existsByVoluntarioAndVaga(voluntario, vaga);
            response.put("inscrito", inscrito);

            if (inscrito) {
                // Buscar a inscrição específica
                List<Inscricao> inscricoes = inscricaoRepository.findByVoluntario(voluntario);
                Optional<Inscricao> inscricaoOpt = inscricoes.stream()
                        .filter(i -> i.getVaga().getId().equals(vagaId))
                        .findFirst();

                if (inscricaoOpt.isPresent()) {
                    Inscricao inscricao = inscricaoOpt.get();
                    response.put("inscricaoId", inscricao.getId());
                    response.put("dataInscricao", inscricao.getDataInscricao().toString());
                }
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("inscrito", false);
            response.put("erro", "falha ao verificar inscrição");
            return ResponseEntity.status(500).body(response);
        }
    }

    // Endpoint para detalhes de uma inscrição específica
    @GetMapping("/detalhes/{inscricaoId}")
    public ModelAndView detalhesInscricao(@PathVariable Long inscricaoId, HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return new ModelAndView("redirect:/login/voluntario");
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return new ModelAndView("redirect:/login/voluntario");
        }

        Inscricao inscricao = inscricaoRepository.findById(inscricaoId).orElse(null);

        if (inscricao == null || !inscricao.getVoluntario().getId().equals(voluntario.getId())) {
            return new ModelAndView("redirect:/inscricao/minhasVagas");
        }

        ModelAndView mv = new ModelAndView("detalhesInscricao");
        mv.addObject("inscricao", inscricao);
        mv.addObject("voluntario", voluntario);

        // Verificar se pode avaliar/reportar (após a data da vaga)
        boolean podeAvaliar = inscricao.getVaga().getData().isBefore(LocalDate.now());
        mv.addObject("podeAvaliar", podeAvaliar);

        return mv;
    }

    @PostMapping("/reportar")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, String>> reportarVaga(
            @RequestParam("vagaId") Long vagaId,
            @RequestParam("assunto") String assunto,
            @RequestParam("motivo") String motivo,
            @RequestParam(value = "avaliacao", required = false) Integer avaliacao,
            @RequestParam(value = "evidencia", required = false) MultipartFile evidencia,
            HttpSession session) {

        Map<String, String> response = new HashMap<>();

        if (!isVoluntarioLoggedIn(session)) {
            response.put("status", "erro");
            response.put("message", "Não autenticado");
            return ResponseEntity.status(401).body(response);
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            response.put("status", "erro");
            response.put("message", "Usuário não encontrado");
            return ResponseEntity.status(404).body(response);
        }

        try {
            // Validações
            if (motivo == null || motivo.trim().isEmpty()) {
                response.put("status", "erro");
                response.put("message", "Motivo é obrigatório");
                return ResponseEntity.badRequest().body(response);
            }

            if (motivo.trim().length() < 20) {
                response.put("status", "erro");
                response.put("message", "Descrição deve ter pelo menos 20 caracteres");
                return ResponseEntity.badRequest().body(response);
            }

            if (motivo.length() > 2000) {
                response.put("status", "erro");
                response.put("message", "Descrição muito longa (máximo 2000 caracteres)");
                return ResponseEntity.badRequest().body(response);
            }

            if (assunto == null || assunto.trim().isEmpty()) {
                response.put("status", "erro");
                response.put("message", "Assunto é obrigatório");
                return ResponseEntity.badRequest().body(response);
            }

            // Validar avaliação se fornecida
            if (avaliacao != null && (avaliacao < 1 || avaliacao > 5)) {
                response.put("status", "erro");
                response.put("message", "Avaliação deve ser entre 1 e 5 estrelas");
                return ResponseEntity.badRequest().body(response);
            }

            // Verificar se a vaga existe
            Vagas vaga = vagasRepository.findById(vagaId).orElse(null);
            if (vaga == null) {
                response.put("status", "erro");
                response.put("message", "Vaga não encontrada");
                return ResponseEntity.badRequest().body(response);
            }

            // Verificar se o voluntário está inscrito na vaga
            boolean estaInscrito = inscricaoRepository.existsByVoluntarioAndVaga(voluntario, vaga);
            if (!estaInscrito) {
                response.put("status", "erro");
                response.put("message", "Você não está inscrito nesta vaga");
                return ResponseEntity.badRequest().body(response);
            }

            // Verificar se o voluntário já reportou esta vaga
            boolean jaReportou = reportRepository.existsByVoluntarioAndVaga(voluntario, vaga);
            if (jaReportou) {
                response.put("status", "erro");
                response.put("message", "Você já reportou esta vaga");
                return ResponseEntity.badRequest().body(response);
            }

            // Converter assunto para enum
            Report.AssuntoReport assuntoEnum = Report.AssuntoReport.fromString(assunto);

            // Processar upload de evidência se fornecida
            String evidenciaUrl = null;
            String evidenciaNome = null;

            if (evidencia != null && !evidencia.isEmpty()) {
                try {
                    Map<String, String> evidenciaResult = salvarEvidencia(evidencia);
                    evidenciaUrl = evidenciaResult.get("url");
                    evidenciaNome = evidenciaResult.get("nome");
                } catch (Exception e) {
                    System.err.println("Erro ao salvar evidência: " + e.getMessage());
                    response.put("status", "erro");
                    response.put("message", "Falha ao processar evidência");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            // Criar e salvar o report
            Report report = new Report(vaga, voluntario, assuntoEnum, motivo.trim(),
                    avaliacao, evidenciaUrl, evidenciaNome);

            // Definir prioridade automaticamente
            report.definirPrioridadeAutomatica();

            reportRepository.save(report);

            // Log para acompanhamento
            System.out.println("NOVO REPORT CRIADO:");
            System.out.println("ID: " + report.getId());
            System.out.println("Assunto: " + report.getAssunto().getDescricao());
            System.out.println("Vaga: " + vaga.getNome());
            System.out.println("ONG: " + vaga.getOng().getNome());
            System.out.println("Voluntário: " + voluntario.getNomeVoluntario());
            System.out.println("Avaliação: " + (avaliacao != null ? avaliacao + " estrelas" : "Não avaliado"));
            System.out.println("Evidência: " + (evidenciaUrl != null ? "Sim" : "Não"));
            System.out.println("Prioridade: " + report.getPrioridade().getDescricao());
            System.out.println("===============================");

            response.put("status", "sucesso");
            response.put("message", "Report enviado com sucesso!");
            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            response.put("status", "erro");
            response.put("message", "Dados inválidos");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "erro");
            response.put("message", "Erro interno do servidor");
            return ResponseEntity.status(500).body(response);
        }
    }

    // Método para salvar evidência
    private Map<String, String> salvarEvidencia(MultipartFile arquivo) throws Exception {
        // Validar arquivo
        if (arquivo.getSize() > 5 * 1024 * 1024) { // 5MB
            throw new RuntimeException("Arquivo muito grande");
        }

        String[] tiposPermitidos = {"image/jpeg", "image/png", "image/gif", "application/pdf"};
        boolean tipoValido = false;
        for (String tipo : tiposPermitidos) {
            if (tipo.equals(arquivo.getContentType())) {
                tipoValido = true;
                break;
            }
        }

        if (!tipoValido) {
            throw new RuntimeException("Tipo de arquivo não permitido");
        }

        // Gerar nome único
        String nomeOriginal = arquivo.getOriginalFilename();
        String extensao = "";
        if (nomeOriginal != null && nomeOriginal.contains(".")) {
            extensao = nomeOriginal.substring(nomeOriginal.lastIndexOf("."));
        }

        String nomeArquivo = UUID.randomUUID().toString() + extensao;

        // Definir diretório de upload
        String diretorioUpload = "uploads/evidencias/";
        Path diretorio = Paths.get(diretorioUpload);

        // Criar diretório se não existir
        if (!Files.exists(diretorio)) {
            Files.createDirectories(diretorio);
        }

        // Salvar arquivo
        Path caminhoArquivo = diretorio.resolve(nomeArquivo);
        Files.copy(arquivo.getInputStream(), caminhoArquivo);

        // Retornar dados do arquivo
        Map<String, String> resultado = new HashMap<>();
        resultado.put("url", "/" + diretorioUpload + nomeArquivo);
        resultado.put("nome", nomeOriginal);

        return resultado;
    }

    // Endpoint para buscar reports do voluntário
    @GetMapping("/meusReports")
    @Transactional(readOnly = true)
    public ModelAndView verMeusReports(HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return new ModelAndView("redirect:/login/voluntario");
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return new ModelAndView("redirect:/login/voluntario");
        }

        ModelAndView mv = new ModelAndView("meusReports");

        try {
            List<Report> reports = reportRepository.findByVoluntario(voluntario);

            mv.addObject("voluntario", voluntario);
            mv.addObject("reports", reports);

            // Estatísticas
            long pendentes = reports.stream().filter(r -> r.getStatus() == Report.StatusReport.PENDENTE).count();
            long resolvidos = reports.stream().filter(r -> r.getStatus() == Report.StatusReport.RESOLVIDO).count();
            long rejeitados = reports.stream().filter(r -> r.getStatus() == Report.StatusReport.REJEITADO).count();
            long analisando = reports.stream().filter(r -> r.getStatus() == Report.StatusReport.ANALISANDO).count();

            mv.addObject("totalReports", reports.size());
            mv.addObject("reportsPendentes", pendentes);
            mv.addObject("reportsAnalisando", analisando);
            mv.addObject("reportsResolvidos", resolvidos);
            mv.addObject("reportsRejeitados", rejeitados);

        } catch (Exception e) {
            e.printStackTrace();
            mv.addObject("voluntario", voluntario);
            mv.addObject("reports", Collections.emptyList());
            mv.addObject("totalReports", 0);
            mv.addObject("reportsPendentes", 0);
            mv.addObject("reportsAnalisando", 0);
            mv.addObject("reportsResolvidos", 0);
            mv.addObject("reportsRejeitados", 0);
            mv.addObject("erro", "Erro ao carregar reports: " + e.getMessage());
        }

        return mv;
    }

    // API endpoint para reports
    @GetMapping("/api/meus-reports")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> meusReportsApi(HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return ResponseEntity.status(401).build();
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return ResponseEntity.status(404).build();
        }

        try {
            List<Report> reports = reportRepository.findByVoluntario(voluntario);

            List<Map<String, Object>> reportsMap = reports.stream()
                    .map(report -> {
                        Map<String, Object> reportMap = new HashMap<>();
                        reportMap.put("id", report.getId());
                        reportMap.put("assunto", report.getAssunto().getDescricao());
                        reportMap.put("motivo", report.getMotivo());
                        reportMap.put("status", report.getStatus().getDescricao());
                        reportMap.put("prioridade", report.getPrioridade().getDescricao());
                        reportMap.put("dataReport", report.getDataReport().toString());
                        reportMap.put("temEvidencia", report.temEvidencia());
                        reportMap.put("temAvaliacao", report.temAvaliacao());
                        if (report.temAvaliacao()) {
                            reportMap.put("avaliacao", report.getAvaliacao());
                        }

                        Map<String, Object> vagaMap = new HashMap<>();
                        if (report.getVaga() != null) {
                            vagaMap.put("id", report.getVaga().getId());
                            vagaMap.put("nome", report.getVaga().getNome());
                            vagaMap.put("ongNome", report.getVaga().getOng() != null ?
                                    report.getVaga().getOng().getNome() : "ONG não informada");
                        } else {
                            vagaMap.put("id", null);
                            vagaMap.put("nome", "Vaga não disponível");
                            vagaMap.put("ongNome", "ONG não informada");
                        }

                        reportMap.put("vaga", vagaMap);
                        return reportMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("reports", reportsMap);
            response.put("total", reportsMap.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    // Endpoint para verificar se pode reportar uma vaga
    @GetMapping("/podeReportar/{vagaId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verificarPodeReportar(@PathVariable Long vagaId, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        if (!isVoluntarioLoggedIn(session)) {
            response.put("podeReportar", false);
            response.put("motivo", "Não autenticado");
            return ResponseEntity.status(401).body(response);
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            response.put("podeReportar", false);
            response.put("motivo", "Usuário não encontrado");
            return ResponseEntity.status(404).body(response);
        }

        try {
            Vagas vaga = vagasRepository.findById(vagaId).orElse(null);
            if (vaga == null) {
                response.put("podeReportar", false);
                response.put("motivo", "Vaga não encontrada");
                return ResponseEntity.badRequest().body(response);
            }

            // Verificar se está inscrito na vaga
            boolean estaInscrito = inscricaoRepository.existsByVoluntarioAndVaga(voluntario, vaga);
            if (!estaInscrito) {
                response.put("podeReportar", false);
                response.put("motivo", "Não inscrito na vaga");
                return ResponseEntity.ok(response);
            }

            // Verificar se já reportou
            boolean jaReportou = reportRepository.existsByVoluntarioAndVaga(voluntario, vaga);
            response.put("podeReportar", !jaReportou);
            response.put("motivo", jaReportou ? "Já reportado" : "Pode reportar");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("podeReportar", false);
            response.put("motivo", "Erro interno");
            return ResponseEntity.status(500).body(response);
        }
    }

    // Utility methods
    private String formatarData(LocalDate data) {
        if (data == null) return "";
        return data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String obterLocalVaga(Vagas vaga) {
        if (vaga == null) return "Local não informado";

        // Priorizar localReferencia se existir
        if (vaga.getLocalReferencia() != null && !vaga.getLocalReferencia().trim().isEmpty()) {
            return vaga.getLocalReferencia();
        }

        // Senão, usar endereço completo
        String enderecoCompleto = vaga.getEnderecoCompleto();
        if (enderecoCompleto != null && !enderecoCompleto.trim().isEmpty()) {
            return enderecoCompleto;
        }

        // Fallback para cidade se disponível
        if (vaga.getCidade() != null && !vaga.getCidade().trim().isEmpty()) {
            return vaga.getCidade() + (vaga.getEstado() != null ? " - " + vaga.getEstado() : "");
        }

        return "Local não informado";
    }
}