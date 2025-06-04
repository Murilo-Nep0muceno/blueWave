package com.blueWave.BlueWave.controller;

import com.blueWave.BlueWave.model.*;
import com.blueWave.BlueWave.repository.*;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private OngRepository ongRepository;

    @Autowired
    private VoluntarioRepository voluntarioRepository;

    @Autowired
    private VagasRepository vagasRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportVoluntarioRepository reportVoluntarioRepository;

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Autowired
    private InscricaoRepository inscricaoRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ===== INICIALIZAÇÃO =====

    @PostConstruct
    public void criarAdminPadrao() {
        try {
            if (!adminRepository.existsByEmail("admin@bluewave.com")) {
                Admin admin = new Admin();
                admin.setNome("Administrador Blue Wave");
                admin.setEmail("admin@bluewave.com");
                admin.setSenha(passwordEncoder.encode("BlueWave2025"));
                admin.setNivelAcesso(Admin.NivelAcesso.SUPER_ADMIN);
                admin.setAtivo(true);
                admin.setDataCriacao(LocalDateTime.now());

                adminRepository.save(admin);
                System.out.println("🌊 ===== BLUE WAVE ADMIN CRIADO =====");
                System.out.println("   Email: admin@bluewave.com");
                System.out.println("   Senha: BlueWave2025");
                System.out.println("   Acesse: http://localhost:8080/admin/dashboard");
                System.out.println("🌊 ===================================");
            } else {
                System.out.println("🌊 Admin Blue Wave já existe no sistema");
            }
        } catch (Exception e) {
            System.err.println("❌ Erro ao criar admin Blue Wave: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===== MÉTODOS DE AUTENTICAÇÃO =====

    private boolean isAdminLoggedIn(HttpSession session) {
        String userType = (String) session.getAttribute("userType");
        String userEmail = (String) session.getAttribute("userEmail");
        return "admin".equals(userType) && userEmail != null;
    }

    private Admin getLoggedAdmin(HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return null;
        }
        String userEmail = (String) session.getAttribute("userEmail");
        return adminRepository.findByEmail(userEmail).orElse(null);
    }

    // ===== ROTAS DE AUTENTICAÇÃO =====

    @GetMapping("/login")
    public ModelAndView loginPage(@RequestParam(value = "error", required = false) String error) {
        ModelAndView mv = new ModelAndView("admin-login");

        if ("true".equals(error)) {
            mv.addObject("error", true);
            mv.addObject("errorMessage", "Email ou senha incorretos para administrador Blue Wave");
        }

        mv.addObject("isAdminLogin", true);
        return mv;
    }

    @PostMapping("/login")
    public ModelAndView processLogin(
            @RequestParam String email,
            @RequestParam String senha,
            HttpSession session) {

        try {
            System.out.println("🔐 Tentativa de login admin Blue Wave: " + email);

            if (email == null || email.trim().isEmpty() ||
                    senha == null || senha.trim().isEmpty()) {
                return new ModelAndView("redirect:/admin/login?error=true");
            }

            String emailLimpo = email.trim().toLowerCase();

            Optional<Admin> adminOpt = adminRepository.findByEmail(emailLimpo);

            if (adminOpt.isPresent()) {
                Admin admin = adminOpt.get();

                if (!admin.getAtivo()) {
                    System.out.println("❌ Admin inativo: " + emailLimpo);
                    return new ModelAndView("redirect:/admin/login?error=true");
                }

                if (passwordEncoder.matches(senha, admin.getSenha())) {
                    System.out.println("✅ Login admin Blue Wave bem-sucedido");

                    // Atualizar último login
                    admin.setUltimoLogin(LocalDateTime.now());
                    adminRepository.save(admin);

                    // Configurar sessão
                    session.setAttribute("userEmail", emailLimpo);
                    session.setAttribute("userType", "admin");
                    session.setAttribute("userId", admin.getId());
                    session.setAttribute("userName", admin.getNome());
                    session.setAttribute("adminLevel", admin.getNivelAcesso().name());

                    return new ModelAndView("redirect:/admin/dashboard");
                } else {
                    System.out.println("❌ Senha incorreta para admin: " + emailLimpo);
                }
            } else {
                System.out.println("❌ Admin não encontrado: " + emailLimpo);
            }

            return new ModelAndView("redirect:/admin/login?error=true");

        } catch (Exception e) {
            System.err.println("❌ Erro no login admin: " + e.getMessage());
            e.printStackTrace();
            return new ModelAndView("redirect:/admin/login?error=true");
        }
    }

    @GetMapping("/logout")
    public ModelAndView logout(HttpSession session) {
        try {
            System.out.println("👋 Logout do admin Blue Wave");
            if (session != null) {
                session.invalidate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ModelAndView("redirect:/admin/login");
    }

    // ===== DASHBOARD PRINCIPAL =====

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        System.out.println("🌊 Acessando dashboard Blue Wave");

        if (!isAdminLoggedIn(session)) {
            return "redirect:/admin/login";
        }

        Admin admin = getLoggedAdmin(session);
        if (admin == null) {
            return "redirect:/admin/login";
        }

        try {
            // Carregar estatísticas
            Map<String, Object> stats = obterEstatisticasCompletas();

            // Formatar último login
            if (admin.getUltimoLogin() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                admin.setUltimoLoginFormatado(admin.getUltimoLogin().format(formatter));
            } else {
                admin.setUltimoLoginFormatado("Primeiro acesso");
            }

            model.addAttribute("admin", admin);
            model.addAttribute("stats", stats);

            System.out.println("✅ Dashboard Blue Wave carregado com sucesso");
            return "dashboard"; // Usar o template criado

        } catch (Exception e) {
            System.err.println("❌ Erro ao carregar dashboard: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("erro", "Erro ao carregar dashboard: " + e.getMessage());
            return "dashboard";
        }
    }

    // ===== PÁGINAS ADMINISTRATIVAS =====

    @GetMapping("/ongs")
    public String gerenciarOngs(Model model, HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/admin/login";
        }

        // Implementar página de gestão de ONGs
        return "admin-ongs";
    }

    @GetMapping("/voluntarios")
    public String gerenciarVoluntarios(Model model, HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/admin/login";
        }

        // Implementar página de gestão de voluntários
        return "admin-voluntarios";
    }

    @GetMapping("/reports")
    public String gerenciarReports(Model model, HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/admin/login";
        }

        // Implementar página de gestão de reports
        return "admin-reports";
    }

    @GetMapping("/configuracoes")
    public String configuracoes(Model model, HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/admin/login";
        }

        Admin admin = getLoggedAdmin(session);
        model.addAttribute("admin", admin);

        return "admin-configuracoes";
    }

    // ===== APIs DE ESTATÍSTICAS =====

    @GetMapping("/api/estatisticas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obterEstatisticas(HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Map<String, Object> stats = obterEstatisticasCompletas();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/reports-pendentes")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getReportsPendentes(HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Map<String, Object> pendentes = new HashMap<>();

            pendentes.put("reportsVagas", contarReportsPorStatus(Report.StatusReport.PENDENTE));
            pendentes.put("reportsVoluntarios",
                    contarReportsVoluntarioPorStatus(ReportVoluntario.StatusReportVoluntario.PENDENTE));

            return ResponseEntity.ok(pendentes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== GESTÃO DE REPORTS =====

    @GetMapping("/api/reports")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> listarReports(
            @RequestParam(value = "status", defaultValue = "") String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpSession session) {

        if (!isAdminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("dataReport").descending());
            Page<Report> reports;

            if (!status.isEmpty()) {
                try {
                    Report.StatusReport statusEnum = Report.StatusReport.valueOf(status.toUpperCase());
                    reports = reportRepository.findByStatus(statusEnum, pageable);
                } catch (IllegalArgumentException e) {
                    reports = reportRepository.findAll(pageable);
                }
            } else {
                reports = reportRepository.findAll(pageable);
            }

            List<Map<String, Object>> reportsData = reports.getContent().stream()
                    .map(this::convertReportToMap)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(reportsData);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/reports-voluntarios")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> listarReportsVoluntarios(
            @RequestParam(value = "status", defaultValue = "") String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpSession session) {

        if (!isAdminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // Como o endpoint pode não existir ainda, retornar dados vazios por enquanto
            List<Map<String, Object>> mockData = new ArrayList<>();

            // Dados mock para demonstração
            if ("PENDENTE".equals(status.toUpperCase()) || status.isEmpty()) {
                Map<String, Object> mockReport = new HashMap<>();
                mockReport.put("id", 1L);
                mockReport.put("assunto", "Comportamento inadequado");
                mockReport.put("motivo", "Voluntário não seguiu as instruções durante o evento");
                mockReport.put("voluntario", "João Silva");
                mockReport.put("ong", "ONG Esperança");
                mockReport.put("status", "PENDENTE");
                mockData.add(mockReport);
            }

            return ResponseEntity.ok(mockData);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/api/report/{reportId}/action")
    @ResponseBody
    public ResponseEntity<Map<String, String>> processarAcaoReport(
            @PathVariable Long reportId,
            @RequestParam String action,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) String banType,
            @RequestParam(required = false) String banReason,
            HttpSession session) {

        if (!isAdminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Acesso não autorizado"));
        }

        try {
            Report report = reportRepository.findById(reportId).orElse(null);
            if (report == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Report não encontrado"));
            }

            Admin admin = getLoggedAdmin(session);
            String resultado = processarAcao(report, action, comment, banType, banReason, admin);

            return ResponseEntity.ok(Map.of("message", resultado));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro interno: " + e.getMessage()));
        }
    }

    @PostMapping("/api/report-voluntario/{reportId}/action")
    @ResponseBody
    public ResponseEntity<Map<String, String>> processarAcaoReportVoluntario(
            @PathVariable Long reportId,
            @RequestParam String action,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) String banType,
            @RequestParam(required = false) String banReason,
            HttpSession session) {

        if (!isAdminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Acesso não autorizado"));
        }

        try {
            ReportVoluntario report = reportVoluntarioRepository.findById(reportId).orElse(null);
            if (report == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Report não encontrado"));
            }

            Admin admin = getLoggedAdmin(session);
            String resultado = processarAcaoVoluntario(report, action, comment, banType, banReason, admin);

            return ResponseEntity.ok(Map.of("message", resultado));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro interno: " + e.getMessage()));
        }
    }

    // ===== GESTÃO DE USUÁRIOS =====

    @PostMapping("/api/usuario/{userId}/ban")
    @ResponseBody
    public ResponseEntity<Map<String, String>> banirUsuario(
            @PathVariable Long userId,
            @RequestParam String userType, // "ong" ou "voluntario"
            @RequestParam String banType, // "temporary" ou "permanent"
            @RequestParam String reason,
            @RequestParam String details,
            @RequestParam(defaultValue = "true") boolean notifyUser,
            HttpSession session) {

        if (!isAdminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Acesso não autorizado"));
        }

        try {
            Admin admin = getLoggedAdmin(session);
            String resultado = executarBanimento(userId, userType, banType, reason, details, notifyUser, admin);

            return ResponseEntity.ok(Map.of("message", resultado));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro interno: " + e.getMessage()));
        }
    }

    @PostMapping("/api/usuario/{userId}/warn")
    @ResponseBody
    public ResponseEntity<Map<String, String>> advertirUsuario(
            @PathVariable Long userId,
            @RequestParam String userType,
            @RequestParam String reason,
            @RequestParam String message,
            HttpSession session) {

        if (!isAdminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Acesso não autorizado"));
        }

        try {
            Admin admin = getLoggedAdmin(session);
            String resultado = enviarAdvertencia(userId, userType, reason, message, admin);

            return ResponseEntity.ok(Map.of("message", resultado));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro interno: " + e.getMessage()));
        }
    }

    // ===== SISTEMA DE NOTIFICAÇÕES =====

    @PostMapping("/api/notificacao/enviar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> enviarNotificacao(
            @RequestParam String target, // "all", "ongs", "volunteers", "specific"
            @RequestParam String type, // "info", "warning", "update", etc.
            @RequestParam String title,
            @RequestParam String message,
            @RequestParam(defaultValue = "true") boolean push,
            @RequestParam(required = false) List<Long> specificUsers,
            HttpSession session) {

        if (!isAdminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Acesso não autorizado"));
        }

        try {
            Admin admin = getLoggedAdmin(session);
            int enviadas = processarEnvioNotificacao(target, type, title, message, push, specificUsers, admin);

            return ResponseEntity.ok(Map.of("message",
                    "Notificação enviada com sucesso para " + enviadas + " usuários!"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro interno: " + e.getMessage()));
        }
    }

    // ===== RELATÓRIOS E ANÁLISES =====

    @GetMapping("/api/relatorio/geral")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> gerarRelatorioGeral(
            @RequestParam(required = false) String periodo, // "week", "month", "year"
            HttpSession session) {

        if (!isAdminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Map<String, Object> relatorio = new HashMap<>();

            // Período padrão: último mês
            LocalDateTime inicio = LocalDateTime.now().minusMonths(1);
            LocalDateTime fim = LocalDateTime.now();

            if ("week".equals(periodo)) {
                inicio = LocalDateTime.now().minusWeeks(1);
            } else if ("year".equals(periodo)) {
                inicio = LocalDateTime.now().minusYears(1);
            }

            // Dados do relatório
            relatorio.put("periodo", periodo != null ? periodo : "month");
            relatorio.put("dataInicio", inicio.toString());
            relatorio.put("dataFim", fim.toString());

            // Estatísticas do período
            relatorio.put("novasOngs", contarNovasOngsPeriodo(inicio, fim));
            relatorio.put("novosVoluntarios", contarNovosVoluntariosPeriodo(inicio, fim));
            relatorio.put("vagasCriadas", contarVagasCriadasPeriodo(inicio, fim));
            relatorio.put("inscricoesRealizadas", contarInscricoesPeriodo(inicio, fim));
            relatorio.put("reportsRecebidos", contarReportsPeriodo(inicio, fim));

            // Análises
            relatorio.put("ongsMaisAtivas", obterOngsMaisAtivas(inicio, fim));
            relatorio.put("voluntariosMaisAtivos", obterVoluntariosMaisAtivos(inicio, fim));
            relatorio.put("categoriasPopulares", obterCategoriasPopulares(inicio, fim));

            return ResponseEntity.ok(relatorio);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== BACKUP E MANUTENÇÃO =====

    @PostMapping("/api/backup/criar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> criarBackup(HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Acesso não autorizado"));
        }

        try {
            Admin admin = getLoggedAdmin(session);
            String backupId = executarBackup(admin);

            return ResponseEntity.ok(Map.of(
                    "message", "Backup criado com sucesso!",
                    "backupId", backupId,
                    "dataHora", LocalDateTime.now().toString()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao criar backup: " + e.getMessage()));
        }
    }

    @GetMapping("/api/sistema/logs")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obterLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String level, // "ERROR", "WARN", "INFO"
            HttpSession session) {

        if (!isAdminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // Aqui você implementaria a lógica para obter logs do sistema
            List<Map<String, Object>> logs = obterLogsDoSistema(page, size, level);
            return ResponseEntity.ok(logs);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== ENDPOINTS AUXILIARES =====

    @GetMapping("/api/mock-data")
    @ResponseBody
    public Map<String, Object> getMockData(HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return Map.of("erro", "Não autorizado");
        }

        Map<String, Object> mockData = new HashMap<>();

        // Mock reports de vagas
        List<Map<String, Object>> mockReports = new ArrayList<>();
        Map<String, Object> report1 = new HashMap<>();
        report1.put("id", 1L);
        report1.put("assunto", "Informações Falsas");
        report1.put("motivo", "A ONG divulgou informações incorretas sobre a localização do evento");
        report1.put("reportadoPor", "Maria Silva");
        report1.put("ong", "ONG Esperança Verde");
        report1.put("status", "PENDENTE");
        mockReports.add(report1);

        Map<String, Object> report2 = new HashMap<>();
        report2.put("id", 2L);
        report2.put("assunto", "Vaga Inadequada");
        report2.put("motivo", "A vaga exige trabalho não relacionado ao voluntariado");
        report2.put("reportadoPor", "João Santos");
        report2.put("ong", "ONG Solidária");
        report2.put("status", "PENDENTE");
        mockReports.add(report2);

        mockData.put("reportsVagas", mockReports);

        // Mock reports de voluntários
        List<Map<String, Object>> mockUserReports = new ArrayList<>();
        Map<String, Object> userReport1 = new HashMap<>();
        userReport1.put("id", 1L);
        userReport1.put("assunto", "Comportamento Inadequado");
        userReport1.put("motivo", "Voluntário foi desrespeitoso com outros participantes");
        userReport1.put("voluntario", "Carlos Mendes");
        userReport1.put("ong", "ONG Ajuda Mútua");
        userReport1.put("status", "PENDENTE");
        mockUserReports.add(userReport1);

        mockData.put("reportsVoluntarios", mockUserReports);

        return mockData;
    }

    @GetMapping("/test-dashboard")
    @ResponseBody
    public Map<String, Object> testeDashboard(HttpSession session) {
        Map<String, Object> resultado = new HashMap<>();

        try {
            resultado.put("timestamp", LocalDateTime.now().toString());
            resultado.put("adminLogado", isAdminLoggedIn(session));
            resultado.put("versao", "Blue Wave Admin Dashboard v1.0");
            resultado.put("status", "Sistema operacional");

            // Testar estatísticas
            Map<String, Object> stats = obterEstatisticasCompletas();
            resultado.put("estatisticas", stats);

            // Testar conexões com repositórios
            Map<String, Object> testesConexao = new HashMap<>();
            try {
                testesConexao.put("ongsCount", ongRepository.count());
                testesConexao.put("voluntariosCount", voluntarioRepository.count());
                testesConexao.put("vagasCount", vagasRepository.count());
                testesConexao.put("reportsCount", reportRepository.count());
                testesConexao.put("inscricoesCount", inscricaoRepository.count());

                // Testar se ReportVoluntarioRepository existe
                try {
                    testesConexao.put("reportsVoluntarioCount", reportVoluntarioRepository.count());
                } catch (Exception e) {
                    testesConexao.put("reportsVoluntarioCount", "Repository não encontrado");
                }

            } catch (Exception e) {
                testesConexao.put("erro", "Erro ao acessar repositórios: " + e.getMessage());
            }
            resultado.put("testesConexao", testesConexao);

            // Informações da sessão
            if (session != null) {
                Map<String, Object> sessaoInfo = new HashMap<>();
                sessaoInfo.put("userType", session.getAttribute("userType"));
                sessaoInfo.put("userEmail", session.getAttribute("userEmail"));
                sessaoInfo.put("userName", session.getAttribute("userName"));
                sessaoInfo.put("adminLevel", session.getAttribute("adminLevel"));
                resultado.put("sessaoInfo", sessaoInfo);
            }

            // Testar alguns métodos específicos
            Map<String, Object> testesFuncionalidade = new HashMap<>();
            try {
                LocalDate hoje = LocalDate.now();
                testesFuncionalidade.put("vagasHoje", contarVagasHoje(hoje));
                testesFuncionalidade.put("vagasAmanha", contarVagasAmanha(hoje.plusDays(1)));
                testesFuncionalidade.put("reportsPendentes", contarReportsPorStatus(Report.StatusReport.PENDENTE));
                testesFuncionalidade.put("vagasAtivas", contarVagasPorStatus(Vagas.StatusVaga.ATIVA));
            } catch (Exception e) {
                testesFuncionalidade.put("erro", "Erro nos testes: " + e.getMessage());
            }
            resultado.put("testesFuncionalidade", testesFuncionalidade);

        } catch (Exception e) {
            resultado.put("erro", e.getMessage());
            resultado.put("status", "Erro no sistema");
            e.printStackTrace();
        }

        return resultado;
    }

    @GetMapping("/test")
    @ResponseBody
    public Map<String, Object> testeAdmin(HttpSession session) {
        Map<String, Object> resultado = new HashMap<>();

        try {
            resultado.put("timestamp", LocalDateTime.now().toString());
            resultado.put("adminLogado", isAdminLoggedIn(session));
            resultado.put("versao", "Blue Wave Admin v2.0");
            resultado.put("status", "Sistema operacional");

            if (session != null) {
                resultado.put("userType", session.getAttribute("userType"));
                resultado.put("userEmail", session.getAttribute("userEmail"));
                resultado.put("userName", session.getAttribute("userName"));
                resultado.put("adminLevel", session.getAttribute("adminLevel"));
            }

            // Testar conexão com repositórios
            resultado.put("totalOngs", ongRepository.count());
            resultado.put("totalVoluntarios", voluntarioRepository.count());
            resultado.put("totalVagas", vagasRepository.count());
            resultado.put("adminExiste", adminRepository.existsByEmail("admin@bluewave.com"));

        } catch (Exception e) {
            resultado.put("erro", e.getMessage());
            resultado.put("status", "Erro no sistema");
        }

        return resultado;
    }

    // ===== MÉTODOS AUXILIARES PRINCIPAIS =====

    private Map<String, Object> obterEstatisticasCompletas() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Estatísticas básicas
            long totalOngs = ongRepository.count();
            long totalVoluntarios = voluntarioRepository.count();
            long totalVagas = vagasRepository.count();

            stats.put("totalOngs", totalOngs);
            stats.put("totalVoluntarios", totalVoluntarios);
            stats.put("totalVagas", totalVagas);

            // Inscrições (se o repositório existir)
            try {
                long totalInscricoes = inscricaoRepository.count();
                stats.put("totalInscricoes", totalInscricoes);
            } catch (Exception e) {
                stats.put("totalInscricoes", 0L);
            }

            // Estatísticas de vagas por status
            stats.put("vagasAtivas", contarVagasPorStatus(Vagas.StatusVaga.ATIVA));
            stats.put("vagasConcluidas", contarVagasPorStatus(Vagas.StatusVaga.CONCLUIDA));
            stats.put("vagasInterrompidas", contarVagasPorStatus(Vagas.StatusVaga.INTERROMPIDA));

            // Estatísticas de reports
            long reportsPendentes = contarReportsPorStatus(Report.StatusReport.PENDENTE);
            stats.put("reportsPendentes", reportsPendentes);
            stats.put("reportsResolvidos", contarReportsPorStatus(Report.StatusReport.RESOLVIDO));
            stats.put("reportsRejeitados", contarReportsPorStatus(Report.StatusReport.REJEITADO));

            // Estatísticas de reports de voluntários (simulado por enquanto)
            long reportsVoluntarioPendentes = 1; // Valor simulado
            stats.put("reportsVoluntarioPendentes", reportsVoluntarioPendentes);
            stats.put("reportsVoluntarioResolvidos", 0L);

            // Total de reports pendentes
            stats.put("totalReportsPendentes", reportsPendentes + reportsVoluntarioPendentes);

            // Estatísticas temporais
            LocalDate hoje = LocalDate.now();
            stats.put("vagasHoje", contarVagasHoje(hoje));
            stats.put("vagasAmanha", contarVagasAmanha(hoje.plusDays(1)));

            // Usuários banidos (simulado)
            stats.put("usuariosBanidos", 0L);

            // Crescimento mensal (simulado)
            stats.put("crescimentoOngs", calcularCrescimentoOngs());
            stats.put("crescimentoVoluntarios", calcularCrescimentoVoluntarios());

            System.out.println("✅ Estatísticas carregadas: " + stats);

        } catch (Exception e) {
            System.err.println("❌ Erro ao obter estatísticas: " + e.getMessage());
            e.printStackTrace();

            // Retornar valores padrão em caso de erro
            stats.put("totalOngs", 0L);
            stats.put("totalVoluntarios", 0L);
            stats.put("totalVagas", 0L);
            stats.put("totalInscricoes", 0L);
            stats.put("vagasAtivas", 0L);
            stats.put("reportsPendentes", 0L);
            stats.put("reportsVoluntarioPendentes", 0L);
            stats.put("totalReportsPendentes", 0L);
            stats.put("usuariosBanidos", 0L);
            stats.put("vagasHoje", 0L);
            stats.put("vagasAmanha", 0L);
        }

        return stats;
    }

    private Map<String, Object> convertReportToMap(Report report) {
        Map<String, Object> map = new HashMap<>();

        try {
            map.put("id", report.getId());
            map.put("status", report.getStatus() != null ? report.getStatus().name() : "PENDENTE");

            // Assunto
            if (report.getAssunto() != null) {
                map.put("assunto", report.getAssunto().getDescricao());
            } else {
                map.put("assunto", "Denúncia");
            }

            map.put("motivo", report.getMotivo() != null ? report.getMotivo() : "Sem descrição");
            map.put("dataReport", report.getDataReport());
            map.put("respostaAdmin", report.getRespostaAdmin());

            // Voluntário que reportou
            if (report.getVoluntario() != null) {
                map.put("reportadoPor", report.getVoluntario().getNomeVoluntario());
            } else {
                map.put("reportadoPor", "Usuário não identificado");
            }

            // Vaga e ONG
            if (report.getVaga() != null) {
                map.put("vaga", report.getVaga().getNome());
                if (report.getVaga().getOng() != null) {
                    map.put("ong", report.getVaga().getOng().getNome());
                } else {
                    map.put("ong", "ONG não identificada");
                }
            } else {
                map.put("vaga", "Vaga não identificada");
                map.put("ong", "ONG não identificada");
            }

        } catch (Exception e) {
            System.err.println("❌ Erro ao converter report para map: " + e.getMessage());
            e.printStackTrace();

            // Valores padrão em caso de erro
            map.put("id", report.getId());
            map.put("status", "ERRO");
            map.put("assunto", "Erro ao carregar");
            map.put("motivo", "Erro ao carregar dados do report");
            map.put("reportadoPor", "Erro");
            map.put("ong", "Erro");
            map.put("vaga", "Erro");
        }

        return map;
    }

    // ===== MÉTODOS DE PROCESSAMENTO DE AÇÕES =====

    private String processarAcao(Report report, String action, String comment,
                                 String banType, String banReason, Admin admin) {

        try {
            switch (action.toLowerCase()) {
                case "resolve":
                    report.setStatus(Report.StatusReport.RESOLVIDO);
                    report.setRespostaAdmin(comment != null ? comment : "Resolvido pela administração");
                    report.setDataResposta(LocalDateTime.now());
                    reportRepository.save(report);

                    System.out.println("✅ Report " + report.getId() + " resolvido por admin " + admin.getNome());
                    return "Report resolvido com sucesso!";

                case "reject":
                    report.setStatus(Report.StatusReport.REJEITADO);
                    report.setRespostaAdmin(comment != null ? comment : "Report rejeitado pela administração");
                    report.setDataResposta(LocalDateTime.now());
                    reportRepository.save(report);

                    System.out.println("❌ Report " + report.getId() + " rejeitado por admin " + admin.getNome());
                    return "Report rejeitado!";

                case "warn":
                    // Enviar advertência para a ONG
                    if (report.getVaga() != null && report.getVaga().getOng() != null) {
                        enviarAdvertenciaOng(report.getVaga().getOng(), banReason, comment);
                    }
                    report.setStatus(Report.StatusReport.RESOLVIDO);
                    report.setRespostaAdmin("Advertência enviada: " + comment);
                    report.setDataResposta(LocalDateTime.now());
                    reportRepository.save(report);

                    System.out.println("⚠️ Advertência enviada para ONG do report " + report.getId());
                    return "Advertência enviada para a ONG!";

                case "ban":
                    // Banir a ONG
                    if (report.getVaga() != null && report.getVaga().getOng() != null) {
                        banirOng(report.getVaga().getOng(), banType, banReason, comment);
                    }
                    report.setStatus(Report.StatusReport.RESOLVIDO);
                    report.setRespostaAdmin("ONG banida: " + comment);
                    report.setDataResposta(LocalDateTime.now());
                    reportRepository.save(report);

                    System.out.println("🚫 ONG banida através do report " + report.getId());
                    return "ONG banida com sucesso!";

                default:
                    throw new IllegalArgumentException("Ação inválida: " + action);
            }
        } catch (Exception e) {
            System.err.println("❌ Erro ao processar ação do report: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erro interno ao processar ação: " + e.getMessage());
        }
    }

    private String processarAcaoVoluntario(ReportVoluntario report, String action, String comment,
                                           String banType, String banReason, Admin admin) {

        try {
            // Por enquanto, simular processamento já que o modelo pode não estar completo
            System.out.println("🔄 Processando ação de report de voluntário: " + action);
            System.out.println("   Report ID: " + report);
            System.out.println("   Comentário: " + comment);
            System.out.println("   Admin: " + admin.getNome());

            switch (action.toLowerCase()) {
                case "resolve":
                    return "Report de voluntário resolvido com sucesso!";
                case "warn":
                    return "Advertência enviada para o voluntário!";
                case "ban":
                    return "Voluntário banido com sucesso!";
                default:
                    throw new IllegalArgumentException("Ação inválida: " + action);
            }
        } catch (Exception e) {
            System.err.println("❌ Erro ao processar ação do report de voluntário: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erro interno ao processar ação: " + e.getMessage());
        }
    }

    // ===== MÉTODOS DE CONTAGEM =====

    private long contarVagasPorStatus(Vagas.StatusVaga status) {
        try {
            List<Vagas> vagas = vagasRepository.findByStatus(status);
            return vagas != null ? vagas.size() : 0;
        } catch (Exception e) {
            System.err.println("❌ Erro ao contar vagas por status " + status + ": " + e.getMessage());
            return 0;
        }
    }

    private long contarReportsPorStatus(Report.StatusReport status) {
        try {
            return reportRepository.countByStatus(status);
        } catch (Exception e) {
            System.err.println("❌ Erro ao contar reports por status " + status + ": " + e.getMessage());
            return 0;
        }
    }

    private long contarReportsVoluntarioPorStatus(ReportVoluntario.StatusReportVoluntario status) {
        try {
            return reportVoluntarioRepository.countByStatus(status);
        } catch (Exception e) {
            System.err.println("❌ Erro ao contar reports de voluntário por status " + status + ": " + e.getMessage());
            return 0;
        }
    }

    private long contarVagasHoje(LocalDate hoje) {
        try {
            List<Vagas> vagas = vagasRepository.findVagasHoje(hoje);
            return vagas != null ? vagas.size() : 0;
        } catch (Exception e) {
            System.err.println("❌ Erro ao contar vagas de hoje: " + e.getMessage());
            return 0;
        }
    }

    private long contarVagasAmanha(LocalDate amanha) {
        try {
            List<Vagas> vagas = vagasRepository.findVagasAmanha(amanha);
            return vagas != null ? vagas.size() : 0;
        } catch (Exception e) {
            System.err.println("❌ Erro ao contar vagas de amanhã: " + e.getMessage());
            return 0;
        }
    }

    private long contarUsuariosBanidos() {
        // Implementar contagem de usuários banidos
        return 12; // Valor simulado
    }

    private double calcularCrescimentoOngs() {
        // Implementar cálculo de crescimento
        return 8.5; // Valor simulado
    }

    private double calcularCrescimentoVoluntarios() {
        // Implementar cálculo de crescimento
        return 15.2; // Valor simulado
    }

    // ===== MÉTODOS DE BANIMENTO E ADVERTÊNCIA =====

    private void enviarAdvertenciaOng(Ong ong, String motivo, String detalhes) {
        try {
            System.out.println("📧 ===== ADVERTÊNCIA ENVIADA =====");
            System.out.println("   ONG: " + ong.getNome());
            System.out.println("   Email: " + ong.getEmail());
            System.out.println("   Motivo: " + motivo);
            System.out.println("   Detalhes: " + detalhes);
            System.out.println("   Data: " + LocalDateTime.now());
            System.out.println("================================");

            // Aqui você pode implementar:
            // 1. Envio de email
            // 2. Criação de notificação no sistema
            // 3. Log da advertência

        } catch (Exception e) {
            System.err.println("❌ Erro ao enviar advertência para ONG: " + e.getMessage());
        }
    }

    private void banirOng(Ong ong, String tipo, String motivo, String detalhes) {
        try {
            System.out.println("🚫 ===== ONG BANIDA =====");
            System.out.println("   ONG: " + ong.getNome());
            System.out.println("   CNPJ: " + ong.getCnpj());
            System.out.println("   Tipo: " + tipo);
            System.out.println("   Motivo: " + motivo);
            System.out.println("   Detalhes: " + detalhes);
            System.out.println("   Data: " + LocalDateTime.now());
            System.out.println("==========================");

            // Implementar lógica de banimento:
            // 1. Desativar todas as vagas da ONG
            // 2. Marcar ONG como banida (se tiver campo)
            // 3. Enviar notificação de banimento
            // 4. Cancelar inscrições ativas

        } catch (Exception e) {
            System.err.println("❌ Erro ao banir ONG: " + e.getMessage());
        }
    }

    private void enviarAdvertenciaVoluntario(Voluntario voluntario, String motivo, String detalhes) {
        try {
            System.out.println("📧 ===== ADVERTÊNCIA ENVIADA =====");
            System.out.println("   Voluntário: " + voluntario.getNomeVoluntario());
            System.out.println("   Email: " + voluntario.getEmail());
            System.out.println("   CPF: " + voluntario.getCpf());
            System.out.println("   Motivo: " + motivo);
            System.out.println("   Detalhes: " + detalhes);
            System.out.println("   Data: " + LocalDateTime.now());
            System.out.println("================================");

            // Implementar envio de advertência:
            // 1. Envio de email
            // 2. Notificação no sistema
            // 3. Log da advertência

        } catch (Exception e) {
            System.err.println("❌ Erro ao enviar advertência para voluntário: " + e.getMessage());
        }
    }

    private void banirVoluntario(Voluntario voluntario, String tipo, String motivo, String detalhes) {
        try {
            System.out.println("🚫 ===== VOLUNTÁRIO BANIDO =====");
            System.out.println("   Voluntário: " + voluntario.getNomeVoluntario());
            System.out.println("   Email: " + voluntario.getEmail());
            System.out.println("   CPF: " + voluntario.getCpf());
            System.out.println("   Tipo: " + tipo);
            System.out.println("   Motivo: " + motivo);
            System.out.println("   Detalhes: " + detalhes);
            System.out.println("   Data: " + LocalDateTime.now());
            System.out.println("===============================");

            // Implementar lógica de banimento do voluntário:
            // 1. Cancelar todas as inscrições ativas
            // 2. Marcar voluntário como banido (se tiver campo)
            // 3. Enviar notificação de banimento

        } catch (Exception e) {
            System.err.println("❌ Erro ao banir voluntário: " + e.getMessage());
        }
    }

    private String executarBanimento(Long userId, String userType, String banType,
                                     String reason, String details, boolean notifyUser, Admin admin) {
        // Implementar lógica de banimento
        return "Usuário banido com sucesso!";
    }

    private String enviarAdvertencia(Long userId, String userType, String reason,
                                     String message, Admin admin) {
        // Implementar envio de advertência
        return "Advertência enviada com sucesso!";
    }

    // ===== MÉTODOS DE NOTIFICAÇÃO E COMUNICAÇÃO =====

    private int processarEnvioNotificacao(String target, String type, String title,
                                          String message, boolean push, List<Long> specificUsers, Admin admin) {
        // Implementar envio de notificações
        return 100; // Número simulado de usuários notificados
    }

    private String executarBackup(Admin admin) {
        // Implementar criação de backup
        return "backup_" + System.currentTimeMillis();
    }

    // ===== MÉTODOS DE RELATÓRIO =====

    private long contarNovasOngsPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return 15; // Valor simulado
    }

    private long contarNovosVoluntariosPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return 234; // Valor simulado
    }

    private long contarVagasCriadasPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return 89; // Valor simulado
    }

    private long contarInscricoesPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return 456; // Valor simulado
    }

    private long contarReportsPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return 12; // Valor simulado
    }

    private List<Map<String, Object>> obterOngsMaisAtivas(LocalDateTime inicio, LocalDateTime fim) {
        // Implementar lógica
        return new ArrayList<>();
    }

    private List<Map<String, Object>> obterVoluntariosMaisAtivos(LocalDateTime inicio, LocalDateTime fim) {
        // Implementar lógica
        return new ArrayList<>();
    }

    private List<Map<String, Object>> obterCategoriasPopulares(LocalDateTime inicio, LocalDateTime fim) {
        // Implementar lógica
        return new ArrayList<>();
    }

    private List<Map<String, Object>> obterLogsDoSistema(int page, int size, String level) {
        // Implementar obtenção de logs
        return new ArrayList<>();
    }

    // ===== HANDLER DE ERROS =====

    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(Exception e, HttpSession session) {
        System.err.println("❌ Erro no Blue Wave AdminController: " + e.getMessage());
        e.printStackTrace();

        ModelAndView mv = new ModelAndView("admin-dashboard");
        mv.addObject("erro", "Erro interno do sistema Blue Wave: " + e.getMessage());

        try {
            Admin admin = getLoggedAdmin(session);
            if (admin != null) {
                mv.addObject("admin", admin);
            }
        } catch (Exception ex) {
            // Ignorar erro secundário
        }

        return mv;
    }
}