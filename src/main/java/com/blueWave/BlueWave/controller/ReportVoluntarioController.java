package com.blueWave.BlueWave.controller;

import com.blueWave.BlueWave.model.*;
import com.blueWave.BlueWave.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/vagas")
public class ReportVoluntarioController {

    @Autowired
    private ReportVoluntarioRepository reportVoluntarioRepository;

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Autowired
    private OngRepository ongRepository;

    @Autowired
    private VoluntarioRepository voluntarioRepository;

    @Autowired
    private InscricaoRepository inscricaoRepository;

    @Autowired
    private VagasRepository vagasRepository;

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + File.separator + "uploads";

    // Verificar se é ONG logada
    private boolean isOngLoggedIn(HttpSession session) {
        String userType = (String) session.getAttribute("userType");
        String userEmail = (String) session.getAttribute("userEmail");
        return "ong".equals(userType) && userEmail != null;
    }

    // Obter ONG logada
    private Ong getLoggedOng(HttpSession session) {
        if (!isOngLoggedIn(session)) {
            return null;
        }
        String userEmail = (String) session.getAttribute("userEmail");
        return ongRepository.findOngByEmail(userEmail);
    }

    // Página de gerenciamento de reports de voluntários
    @GetMapping("/reports-voluntarios")
    public String listarReportsVoluntarios(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", defaultValue = "") String search,
            @RequestParam(value = "status", defaultValue = "") String status,
            Model model,
            HttpSession session) {

        if (!isOngLoggedIn(session)) {
            return "redirect:/login/ong";
        }

        Ong ong = getLoggedOng(session);
        if (ong == null) {
            return "redirect:/login/ong";
        }

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ReportVoluntario> reportsPage;

            // Filtrar por nome do voluntário se fornecido
            if (!search.trim().isEmpty()) {
                reportsPage = reportVoluntarioRepository.findByOngAndVoluntarioNome(ong, search.trim(), pageable);
            } else {
                reportsPage = reportVoluntarioRepository.findByOngOrderByDataReportDesc(ong, pageable);
            }

            // Filtrar por status se fornecido
            if (!status.trim().isEmpty()) {
                try {
                    ReportVoluntario.StatusReportVoluntario statusEnum =
                            ReportVoluntario.StatusReportVoluntario.valueOf(status.toUpperCase());

                    List<ReportVoluntario> reportsFiltrados = reportsPage.getContent().stream()
                            .filter(report -> report.getStatus() == statusEnum)
                            .toList();

                    model.addAttribute("reports", reportsFiltrados);
                } catch (IllegalArgumentException e) {
                    model.addAttribute("reports", reportsPage.getContent());
                }
            } else {
                model.addAttribute("reports", reportsPage.getContent());
            }

            // Estatísticas
            long totalReports = reportVoluntarioRepository.countByOng(ong);
            long reportsPendentes = reportVoluntarioRepository.findByOngAndStatusOrderByDataReportDesc(
                    ong, ReportVoluntario.StatusReportVoluntario.PENDENTE).size();

            Map<String, Object> estatisticas = new HashMap<>();
            estatisticas.put("totalReports", totalReports);
            estatisticas.put("reportsPendentes", reportsPendentes);

            model.addAttribute("estatisticas", estatisticas);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", reportsPage.getTotalPages());
            model.addAttribute("totalElements", reportsPage.getTotalElements());
            model.addAttribute("size", size);
            model.addAttribute("search", search);
            model.addAttribute("statusFilter", status);
            model.addAttribute("hasNext", reportsPage.hasNext());
            model.addAttribute("hasPrevious", reportsPage.hasPrevious());

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("erro", "Erro ao carregar reports: " + e.getMessage());
        }

        return "reportsVoluntarios";
    }

    // Endpoint para reportar voluntário
    @PostMapping("/reportar-voluntario")
    @ResponseBody
    public ResponseEntity<String> reportarVoluntario(
            @RequestParam("voluntarioId") Long voluntarioId,
            @RequestParam("assunto") String assunto,
            @RequestParam("motivo") String motivo,
            @RequestParam(value = "vagaId", required = false) Long vagaId,
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
            // Validações
            if (motivo == null || motivo.trim().length() < 20) {
                return ResponseEntity.badRequest().body("A descrição deve ter pelo menos 20 caracteres");
            }

            // Buscar voluntário
            Voluntario voluntario = voluntarioRepository.findById(voluntarioId).orElse(null);
            if (voluntario == null) {
                return ResponseEntity.badRequest().body("Voluntário não encontrado");
            }

            // Verificar se já reportou este voluntário
            if (reportVoluntarioRepository.existsByOngAndVoluntario(ong, voluntario)) {
                return ResponseEntity.badRequest().body("Você já reportou este voluntário anteriormente");
            }

            // Converter assunto
            ReportVoluntario.AssuntoReportVoluntario assuntoEnum;
            try {
                assuntoEnum = ReportVoluntario.AssuntoReportVoluntario.valueOf(assunto.toUpperCase());
            } catch (IllegalArgumentException e) {
                assuntoEnum = ReportVoluntario.AssuntoReportVoluntario.OUTROS;
            }

            // Buscar vaga se fornecida
            Vagas vaga = null;
            if (vagaId != null) {
                vaga = vagasRepository.findById(vagaId).orElse(null);
            }

            // Processar evidência se fornecida
            String evidenciaUrl = null;
            String evidenciaNome = null;

            if (evidenciaFile != null && !evidenciaFile.isEmpty()) {
                // Validações da evidência
                if (evidenciaFile.getSize() > 5 * 1024 * 1024) { // 5MB
                    return ResponseEntity.badRequest().body("Arquivo de evidência muito grande (máx. 5MB)");
                }

                String contentType = evidenciaFile.getContentType();
                if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
                    return ResponseEntity.badRequest().body("Formato de arquivo não suportado");
                }

                // Salvar arquivo
                File uploadsDir = new File(UPLOAD_DIR);
                if (!uploadsDir.exists()) {
                    uploadsDir.mkdirs();
                }

                String originalFilename = evidenciaFile.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }

                String filename = "report_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + extension;
                File destinationFile = new File(uploadsDir, filename);
                evidenciaFile.transferTo(destinationFile);

                evidenciaUrl = "/uploads/" + filename;
                evidenciaNome = originalFilename;
            }

            // Criar report
            ReportVoluntario report = new ReportVoluntario(voluntario, ong, vaga, assuntoEnum,
                    motivo.trim(), evidenciaUrl, evidenciaNome);
            report.definirPrioridadeAutomatica();
            reportVoluntarioRepository.save(report);

            // Criar notificação para o voluntário
            criarNotificacaoReport(voluntario, ong, report);

            return ResponseEntity.ok("Report enviado com sucesso!");

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao salvar evidência: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno: " + e.getMessage());
        }
    }

    // Endpoint para verificar se pode reportar voluntário
    @GetMapping("/pode-reportar-voluntario/{voluntarioId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> podeReportarVoluntario(
            @PathVariable Long voluntarioId,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        if (!isOngLoggedIn(session)) {
            response.put("podeReportar", false);
            response.put("motivo", "Não autorizado");
            return ResponseEntity.ok(response);
        }

        Ong ong = getLoggedOng(session);
        if (ong == null) {
            response.put("podeReportar", false);
            response.put("motivo", "ONG não encontrada");
            return ResponseEntity.ok(response);
        }

        try {
            Voluntario voluntario = voluntarioRepository.findById(voluntarioId).orElse(null);
            if (voluntario == null) {
                response.put("podeReportar", false);
                response.put("motivo", "Voluntário não encontrado");
                return ResponseEntity.ok(response);
            }

            // Verificar se já reportou
            boolean jaReportou = reportVoluntarioRepository.existsByOngAndVoluntario(ong, voluntario);

            if (jaReportou) {
                response.put("podeReportar", false);
                response.put("motivo", "Já reportado");
            } else {
                response.put("podeReportar", true);
                response.put("motivo", "Pode reportar");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("podeReportar", false);
            response.put("motivo", "Erro interno");
            return ResponseEntity.ok(response);
        }
    }

    // Endpoint para obter detalhes de um report
    @GetMapping("/report-voluntario/{reportId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obterDetalhesReport(
            @PathVariable Long reportId,
            HttpSession session) {

        if (!isOngLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Ong ong = getLoggedOng(session);
        if (ong == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            ReportVoluntario report = reportVoluntarioRepository.findById(reportId).orElse(null);

            if (report == null || !report.getOng().getId().equals(ong.getId())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Map<String, Object> detalhes = new HashMap<>();
            detalhes.put("id", report.getId());
            detalhes.put("assunto", report.getAssunto().getDescricao());
            detalhes.put("motivo", report.getMotivo());
            detalhes.put("status", report.getStatus().getDescricao());
            detalhes.put("prioridade", report.getPrioridade().getDescricao());
            detalhes.put("dataReport", report.getDataReport());
            detalhes.put("temEvidencia", report.temEvidencia());
            detalhes.put("evidenciaUrl", report.getEvidenciaUrl());
            detalhes.put("evidenciaNome", report.getEvidenciaNome());

            // Dados do voluntário
            Map<String, Object> voluntarioData = new HashMap<>();
            voluntarioData.put("id", report.getVoluntario().getId());
            voluntarioData.put("nome", report.getVoluntario().getNomeVoluntario());
            voluntarioData.put("email", report.getVoluntario().getEmail());
            detalhes.put("voluntario", voluntarioData);

            // Dados da vaga se houver
            if (report.getVaga() != null) {
                Map<String, Object> vagaData = new HashMap<>();
                vagaData.put("id", report.getVaga().getId());
                vagaData.put("nome", report.getVaga().getNome());
                detalhes.put("vaga", vagaData);
            }

            return ResponseEntity.ok(detalhes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoint para atualizar status do report
    @PostMapping("/report-voluntario/{reportId}/status")
    @ResponseBody
    public ResponseEntity<String> atualizarStatusReport(
            @PathVariable Long reportId,
            @RequestParam("status") String novoStatus,
            @RequestParam(value = "resposta", required = false) String resposta,
            HttpSession session) {

        if (!isOngLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Acesso não autorizado");
        }

        Ong ong = getLoggedOng(session);
        if (ong == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("ONG não encontrada");
        }

        try {
            ReportVoluntario report = reportVoluntarioRepository.findById(reportId).orElse(null);

            if (report == null || !report.getOng().getId().equals(ong.getId())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Report não encontrado");
            }

            ReportVoluntario.StatusReportVoluntario statusEnum;
            try {
                statusEnum = ReportVoluntario.StatusReportVoluntario.valueOf(novoStatus.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Status inválido");
            }

            report.setStatus(statusEnum);
            if (resposta != null && !resposta.trim().isEmpty()) {
                report.setRespostaAdmin(resposta.trim());
                report.setDataResposta(LocalDateTime.now());
            }

            reportVoluntarioRepository.save(report);

            // Criar notificação para o voluntário sobre a atualização
            criarNotificacaoStatusReport(report.getVoluntario(), ong, report, statusEnum);

            return ResponseEntity.ok("Status atualizado com sucesso!");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno: " + e.getMessage());
        }
    }

    // Método auxiliar para criar notificação de report
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

            // Criar notificação usando setters ao invés de construtor
            Notificacao notificacao = new Notificacao();
            notificacao.setVoluntario(voluntario);
            notificacao.setOng(ong);
            notificacao.setTipo(Notificacao.TipoNotificacao.INFORMACAO_GERAL); // Usar tipo disponível
            notificacao.setTitulo(titulo);
            notificacao.setMensagem(mensagem);
            notificacao.setPrioridade(Notificacao.PrioridadeNotificacao.ALTA);
            notificacao.setDataEnvio(LocalDateTime.now());
            notificacao.setLida(false);
            notificacao.setReportVoluntario(report);

            notificacaoRepository.save(notificacao);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Método auxiliar para criar notificação de atualização de status
    private void criarNotificacaoStatusReport(Voluntario voluntario, Ong ong, ReportVoluntario report,
                                              ReportVoluntario.StatusReportVoluntario novoStatus) {
        try {
            String titulo;
            String mensagem;
            Notificacao.TipoNotificacao tipo;
            Notificacao.PrioridadeNotificacao prioridade;

            switch (novoStatus) {
                case ADVERTENCIA_ENVIADA:
                    titulo = "Advertência Recebida";
                    mensagem = String.format(
                            "Você recebeu uma advertência da ONG %s.\n\n" +
                                    "Motivo: %s\n\n" +
                                    "Por favor, leia atentamente e evite comportamentos similares no futuro.",
                            ong.getNome(),
                            report.getAssunto().getDescricao()
                    );
                    tipo = Notificacao.TipoNotificacao.INFORMACAO_GERAL; // Usar tipo disponível
                    prioridade = Notificacao.PrioridadeNotificacao.ALTA;
                    break;

                case SUSPENSO:
                    titulo = "Conta Suspensa";
                    mensagem = String.format(
                            "Sua conta foi suspensa temporariamente devido ao report da ONG %s.\n\n" +
                                    "Entre em contato com o suporte para mais informações.",
                            ong.getNome()
                    );
                    tipo = Notificacao.TipoNotificacao.INFORMACAO_GERAL; // Usar tipo disponível
                    prioridade = Notificacao.PrioridadeNotificacao.URGENTE;
                    break;

                case RESOLVIDO:
                    titulo = "Report Resolvido";
                    mensagem = String.format(
                            "O report enviado pela ONG %s foi analisado e resolvido.\n\n" +
                                    "Nenhuma ação adicional é necessária.",
                            ong.getNome()
                    );
                    tipo = Notificacao.TipoNotificacao.INFORMACAO_GERAL;
                    prioridade = Notificacao.PrioridadeNotificacao.NORMAL;
                    break;

                case REJEITADO:
                    titulo = "Report Rejeitado";
                    mensagem = String.format(
                            "O report enviado pela ONG %s foi analisado e rejeitado.\n\n" +
                                    "Nenhuma ação será tomada contra sua conta.",
                            ong.getNome()
                    );
                    tipo = Notificacao.TipoNotificacao.INFORMACAO_GERAL;
                    prioridade = Notificacao.PrioridadeNotificacao.NORMAL;
                    break;

                default:
                    return; // Não criar notificação para outros status
            }

            // Criar notificação usando setters ao invés de construtor
            Notificacao notificacao = new Notificacao();
            notificacao.setVoluntario(voluntario);
            notificacao.setOng(ong);
            notificacao.setTipo(tipo);
            notificacao.setTitulo(titulo);
            notificacao.setMensagem(mensagem);
            notificacao.setPrioridade(prioridade);
            notificacao.setDataEnvio(LocalDateTime.now());
            notificacao.setLida(false);
            notificacao.setReportVoluntario(report);

            notificacaoRepository.save(notificacao);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}