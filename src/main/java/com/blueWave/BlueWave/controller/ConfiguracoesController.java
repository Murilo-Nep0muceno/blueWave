package com.blueWave.BlueWave.controller;

import com.blueWave.BlueWave.model.*;
import com.blueWave.BlueWave.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.IOException;
import java.util.*;
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

    @Autowired
    private ReportVoluntarioRepository reportVoluntarioRepository;

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "evidencias";

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

        try {
            List<Vagas> vagasOng = vagasRepository.findByOng(ong);
            Pageable pageable = PageRequest.of(page, size, Sort.by("dataInscricao").descending());
            Page<Inscricao> inscricoesPage;

            if (!search.isEmpty() && !vagaFilter.isEmpty()) {
                Long vagaId = Long.parseLong(vagaFilter);
                Vagas vaga = vagasRepository.findById(vagaId).orElse(null);
                if (vaga != null && vaga.getOng().getId().equals(ong.getId())) {
                    inscricoesPage = inscricaoRepository.findByVagaAndVoluntarioNomeVoluntarioContainingIgnoreCase(
                            vaga, search, pageable);
                } else {
                    inscricoesPage = Page.empty(pageable);
                }
            } else if (!search.isEmpty()) {
                inscricoesPage = inscricaoRepository.findByOngAndVoluntarioNome(ong, search, pageable);
            } else if (!vagaFilter.isEmpty()) {
                Long vagaId = Long.parseLong(vagaFilter);
                Vagas vaga = vagasRepository.findById(vagaId).orElse(null);
                if (vaga != null && vaga.getOng().getId().equals(ong.getId())) {
                    inscricoesPage = inscricaoRepository.findByVaga(vaga, pageable);
                } else {
                    inscricoesPage = Page.empty(pageable);
                }
            } else {
                inscricoesPage = inscricaoRepository.findByOngOrderByDataInscricaoDesc(ong, pageable);
            }

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

        } catch (Exception e) {
            e.printStackTrace();
            ModelAndView mv = new ModelAndView("configuracoes");
            mv.addObject("erro", "Erro ao carregar configurações: " + e.getMessage());
            mv.addObject("inscricoes", Collections.emptyList());
            mv.addObject("vagas", Collections.emptyList());
            mv.addObject("estatisticas", Collections.emptyMap());
            return mv;
        }
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

            if (!inscricao.getVaga().getOng().getId().equals(ong.getId())) {
                return "erro: não autorizado";
            }

            Vagas vaga = inscricao.getVaga();
            vaga.setQuantidade(vaga.getQuantidade() + 1);

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
                    Vagas vaga = inscricao.getVaga();
                    vaga.setQuantidade(vaga.getQuantidade() + 1);

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

    // ===== FUNCIONALIDADE: REPORTAR VOLUNTÁRIO =====

    @PostMapping("/configuracoes/reportar-voluntario/{inscricaoId}")
    @ResponseBody
    public ResponseEntity<String> reportarVoluntario(
            @PathVariable Long inscricaoId,
            @RequestParam("assunto") String assunto,
            @RequestParam("motivo") String motivo,
            @RequestParam(value = "evidencia", required = false) MultipartFile evidenciaFile,
            HttpSession session) {

        if (!isOngLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Acesso não autorizado");
        }

        Ong ong = getLoggedOng(session);
        if (ong == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("ONG não encontrada");
        }

        try {
            Inscricao inscricao = inscricaoRepository.findById(inscricaoId).orElse(null);
            if (inscricao == null || !inscricao.getVaga().getOng().getId().equals(ong.getId())) {
                return ResponseEntity.badRequest().body("Inscrição não encontrada ou não autorizada");
            }

            Voluntario voluntario = inscricao.getVoluntario();
            Vagas vaga = inscricao.getVaga();

            if (motivo == null || motivo.trim().length() < 20) {
                return ResponseEntity.badRequest().body("A descrição deve ter pelo menos 20 caracteres");
            }

            if (motivo.trim().length() > 2000) {
                return ResponseEntity.badRequest().body("A descrição é muito longa (máximo 2000 caracteres)");
            }

            if (reportVoluntarioRepository.existsByOngAndVoluntarioAndVaga(ong, voluntario, vaga)) {
                return ResponseEntity.badRequest().body("Você já reportou este voluntário para esta vaga");
            }

            ReportVoluntario.AssuntoReportVoluntario assuntoEnum;
            try {
                assuntoEnum = ReportVoluntario.AssuntoReportVoluntario.valueOf(assunto.toUpperCase());
            } catch (IllegalArgumentException e) {
                assuntoEnum = ReportVoluntario.AssuntoReportVoluntario.OUTROS;
            }

            String evidenciaUrl = null;
            String evidenciaNome = null;

            if (evidenciaFile != null && !evidenciaFile.isEmpty()) {
                try {
                    if (evidenciaFile.getSize() > 5 * 1024 * 1024) {
                        return ResponseEntity.badRequest().body("Arquivo de evidência muito grande (máx. 5MB)");
                    }

                    String contentType = evidenciaFile.getContentType();
                    if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
                        return ResponseEntity.badRequest().body("Formato de arquivo não suportado");
                    }

                    evidenciaUrl = salvarEvidencia(evidenciaFile);
                    evidenciaNome = evidenciaFile.getOriginalFilename();

                } catch (Exception e) {
                    e.printStackTrace();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Erro ao salvar evidência: " + e.getMessage());
                }
            }

            ReportVoluntario report = new ReportVoluntario(voluntario, ong, vaga, assuntoEnum,
                    motivo.trim(), evidenciaUrl, evidenciaNome);
            report.definirPrioridadeAutomatica();
            reportVoluntarioRepository.save(report);

            criarNotificacaoReport(voluntario, ong, report);

            System.out.println("NOVO REPORT DE VOLUNTÁRIO CRIADO:");
            System.out.println("ID: " + report.getId());
            System.out.println("Assunto: " + report.getAssunto().getDescricao());
            System.out.println("Vaga: " + vaga.getNome());
            System.out.println("ONG: " + ong.getNome());
            System.out.println("Voluntário: " + voluntario.getNomeVoluntario());
            System.out.println("Evidência: " + (evidenciaUrl != null ? "Sim" : "Não"));
            System.out.println("Prioridade: " + report.getPrioridade().getDescricao());
            System.out.println("Data: " + report.getDataReport());
            System.out.println("===============================");

            return ResponseEntity.ok("Report enviado com sucesso!");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno: " + e.getMessage());
        }
    }

    // ===== FUNCIONALIDADE: ENVIAR NOTIFICAÇÃO =====

    @PostMapping("/configuracoes/enviar-notificacao/{inscricaoId}")
    @ResponseBody
    public ResponseEntity<String> enviarNotificacao(
            @PathVariable Long inscricaoId,
            @RequestParam("tipo") String tipo,
            @RequestParam("titulo") String titulo,
            @RequestParam("mensagem") String mensagem,
            @RequestParam(value = "prioridade", defaultValue = "NORMAL") String prioridade,
            HttpSession session) {

        if (!isOngLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Acesso não autorizado");
        }

        Ong ong = getLoggedOng(session);
        if (ong == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("ONG não encontrada");
        }

        try {
            Inscricao inscricao = inscricaoRepository.findById(inscricaoId).orElse(null);
            if (inscricao == null || !inscricao.getVaga().getOng().getId().equals(ong.getId())) {
                return ResponseEntity.badRequest().body("Inscrição não encontrada ou não autorizada");
            }

            Voluntario voluntario = inscricao.getVoluntario();

            if (titulo == null || titulo.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Título é obrigatório");
            }

            if (titulo.trim().length() > 255) {
                return ResponseEntity.badRequest().body("Título muito longo (máximo 255 caracteres)");
            }

            if (mensagem == null || mensagem.trim().length() < 10) {
                return ResponseEntity.badRequest().body("Mensagem deve ter pelo menos 10 caracteres");
            }

            if (mensagem.trim().length() > 2000) {
                return ResponseEntity.badRequest().body("Mensagem muito longa (máximo 2000 caracteres)");
            }

            Notificacao.TipoNotificacao tipoEnum;
            try {
                tipoEnum = Notificacao.TipoNotificacao.valueOf(tipo.toUpperCase());
            } catch (IllegalArgumentException e) {
                tipoEnum = Notificacao.TipoNotificacao.INFORMACAO_GERAL;
            }

            Notificacao.PrioridadeNotificacao prioridadeEnum;
            try {
                prioridadeEnum = Notificacao.PrioridadeNotificacao.valueOf(prioridade.toUpperCase());
            } catch (IllegalArgumentException e) {
                prioridadeEnum = Notificacao.PrioridadeNotificacao.NORMAL;
            }

            Notificacao notificacao = new Notificacao(
                    voluntario,
                    ong,
                    tipoEnum,
                    titulo.trim(),
                    mensagem.trim(),
                    prioridadeEnum
            );
            notificacao.setVaga(inscricao.getVaga());
            notificacao.gerarUrlAcao();

            notificacaoRepository.save(notificacao);

            System.out.println("NOVA NOTIFICAÇÃO ENVIADA:");
            System.out.println("ID: " + notificacao.getId());
            System.out.println("Tipo: " + notificacao.getTipo().getDescricao());
            System.out.println("Título: " + notificacao.getTitulo());
            System.out.println("ONG: " + ong.getNome());
            System.out.println("Voluntário: " + voluntario.getNomeVoluntario());
            System.out.println("Prioridade: " + notificacao.getPrioridade().getDescricao());
            System.out.println("Data: " + notificacao.getDataEnvio());
            System.out.println("===============================");

            return ResponseEntity.ok("Notificação enviada com sucesso!");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno: " + e.getMessage());
        }
    }

    // ===== MÉTODOS AUXILIARES =====

    private void criarNotificacaoReport(Voluntario voluntario, Ong ong, ReportVoluntario report) {
        try {
            String titulo = "Você recebeu um report";
            String mensagem = String.format(
                    "A ONG %s enviou um report sobre seu comportamento.\n\n" +
                            "Assunto: %s\n\n" +
                            "Nossa equipe irá analisar este report e entrará em contato se necessário.\n\n" +
                            "Se você acredita que este report é injusto, pode entrar em contato conosco através do suporte.",
                    ong.getNome(),
                    report.getAssunto().getDescricao()
            );

            Notificacao notificacao = new Notificacao(
                    voluntario,
                    ong,
                    Notificacao.TipoNotificacao.REPORT_RECEBIDO,
                    titulo,
                    mensagem,
                    report,
                    Notificacao.PrioridadeNotificacao.ALTA
            );

            notificacaoRepository.save(notificacao);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String salvarEvidencia(MultipartFile arquivo) throws Exception {
        if (arquivo.getSize() > 5 * 1024 * 1024) {
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

        String nomeOriginal = arquivo.getOriginalFilename();
        String extensao = "";
        if (nomeOriginal != null && nomeOriginal.contains(".")) {
            extensao = nomeOriginal.substring(nomeOriginal.lastIndexOf("."));
        }

        String nomeArquivo = "report_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + extensao;

        File diretorioUpload = new File(UPLOAD_DIR);
        if (!diretorioUpload.exists()) {
            diretorioUpload.mkdirs();
        }

        File arquivoDestino = new File(diretorioUpload, nomeArquivo);
        arquivo.transferTo(arquivoDestino);

        return "/uploads/evidencias/" + nomeArquivo;
    }

    private String formatTelefone(String telefone) {
        if (telefone == null || telefone.isEmpty()) {
            return "Não informado";
        }

        String cleaned = telefone.replaceAll("\\D", "");

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

    private String formatCpf(String cpf) {
        if (cpf == null || cpf.isEmpty()) {
            return "Não informado";
        }

        String cleaned = cpf.replaceAll("\\D", "");

        if (cleaned.length() == 11) {
            return String.format("%s.%s.%s-%s",
                    cleaned.substring(0, 3),
                    cleaned.substring(3, 6),
                    cleaned.substring(6, 9),
                    cleaned.substring(9));
        }

        return cpf;
    }

    private Map<String, Object> calcularEstatisticas(Ong ong) {
        try {
            long totalInscricoes = inscricaoRepository.countByOng(ong);
            long totalVagas = vagasRepository.countByOng(ong);
            long voluntariosUnicos = inscricaoRepository.countDistinctVoluntariosByOng(ong);
            long vagasComInscricoes = inscricaoRepository.countDistinctVagasWithInscricoesByOng(ong);

            return Map.of(
                    "totalInscricoes", totalInscricoes,
                    "totalVagas", totalVagas,
                    "vagasComInscricoes", vagasComInscricoes,
                    "voluntariosUnicos", voluntariosUnicos
            );
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                    "totalInscricoes", 0L,
                    "totalVagas", 0L,
                    "vagasComInscricoes", 0L,
                    "voluntariosUnicos", 0L
            );
        }
    }
}