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
            return "dashboard";

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
        return "admin-ongs";
    }

    @GetMapping("/voluntarios")
    public String gerenciarVoluntarios(Model model, HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/admin/login";
        }
        return "admin-voluntarios";
    }

    @GetMapping("/reports")
    public String gerenciarReports(Model model, HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return "redirect:/admin/login";
        }
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
                    reports = reportRepository.findAllWithDetails(pageable);
                }
            } else {
                reports = reportRepository.findAllWithDetails(pageable);
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
            Pageable pageable = PageRequest.of(page, size, Sort.by("dataReport").descending());
            Page<ReportVoluntario> reports;

            if (!status.isEmpty()) {
                try {
                    ReportVoluntario.StatusReportVoluntario statusEnum = ReportVoluntario.StatusReportVoluntario.valueOf(status.toUpperCase());
                    reports = reportVoluntarioRepository.findByStatus(statusEnum, pageable);
                } catch (IllegalArgumentException e) {
                    reports = reportVoluntarioRepository.findAll(pageable);
                }
            } else {
                reports = reportVoluntarioRepository.findAll(pageable);
            }

            List<Map<String, Object>> reportsData = reports.getContent().stream()
                    .map(this::convertReportVoluntarioToMap)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(reportsData);

        } catch (Exception e) {
            System.err.println("❌ Erro ao listar reports de voluntários: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== ENDPOINT PARA USUÁRIOS BANIDOS =====

    @GetMapping("/api/usuarios-banidos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarUsuariosBanidos(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "tipo", defaultValue = "") String tipo,
            HttpSession session) {

        if (!isAdminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Map<String, Object> resultado = new HashMap<>();
            List<Map<String, Object>> usuariosBanidos = new ArrayList<>();

            // Buscar ONGs banidas
            List<Ong> ongsBanidas = ongRepository.findByBanidoTrue();
            for (Ong ong : ongsBanidas) {
                Map<String, Object> ongMap = new HashMap<>();
                ongMap.put("id", ong.getId());
                ongMap.put("nome", ong.getNome());
                ongMap.put("email", ong.getEmail());
                ongMap.put("tipo", "ONG");
                ongMap.put("dataBanimento", ong.getDataBanimento());
                ongMap.put("motivoBanimento", ong.getMotivoBanimento());
                ongMap.put("tipoBanimento", ong.getTipoBanimento() != null ? ong.getTipoBanimento().name() : "PERMANENTE");
                ongMap.put("dataFimBanimento", ong.getDataFimBanimento());
                ongMap.put("adminResponsavel", ong.getAdminResponsavelBanimento());
                ongMap.put("banidoAtualmente", ong.isBanido());
                usuariosBanidos.add(ongMap);
            }

            // Buscar Voluntários banidos
            List<Voluntario> voluntariosBanidos = voluntarioRepository.findByBanidoTrue();
            for (Voluntario voluntario : voluntariosBanidos) {
                Map<String, Object> volMap = new HashMap<>();
                volMap.put("id", voluntario.getId());
                volMap.put("nome", voluntario.getNomeVoluntario());
                volMap.put("email", voluntario.getEmail());
                volMap.put("tipo", "Voluntário");
                volMap.put("dataBanimento", voluntario.getDataBanimento());
                volMap.put("motivoBanimento", voluntario.getMotivoBanimento());
                volMap.put("tipoBanimento", voluntario.getTipoBanimento() != null ? voluntario.getTipoBanimento().name() : "PERMANENTE");
                volMap.put("dataFimBanimento", voluntario.getDataFimBanimento());
                volMap.put("adminResponsavel", voluntario.getAdminResponsavelBanimento());
                volMap.put("banidoAtualmente", voluntario.isBanido());
                usuariosBanidos.add(volMap);
            }

            // Filtrar por tipo se especificado
            if (!tipo.isEmpty()) {
                usuariosBanidos = usuariosBanidos.stream()
                        .filter(u -> tipo.equalsIgnoreCase((String) u.get("tipo")))
                        .collect(Collectors.toList());
            }

            // Ordenar por data de banimento (mais recentes primeiro)
            usuariosBanidos.sort((a, b) -> {
                LocalDateTime dateA = (LocalDateTime) a.get("dataBanimento");
                LocalDateTime dateB = (LocalDateTime) b.get("dataBanimento");
                if (dateA == null) return 1;
                if (dateB == null) return -1;
                return dateB.compareTo(dateA);
            });

            // Implementar paginação manual
            int start = page * size;
            int end = Math.min(start + size, usuariosBanidos.size());
            List<Map<String, Object>> paginatedUsers = usuariosBanidos.subList(start, end);

            resultado.put("usuarios", paginatedUsers);
            resultado.put("totalElements", usuariosBanidos.size());
            resultado.put("totalPages", (int) Math.ceil((double) usuariosBanidos.size() / size));
            resultado.put("currentPage", page);
            resultado.put("pageSize", size);

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            System.err.println("❌ Erro ao listar usuários banidos: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== ENDPOINT PARA DESBANIR =====

    @PostMapping("/api/usuario/{userId}/desbanir")
    @ResponseBody
    public ResponseEntity<Map<String, String>> desbanirUsuario(
            @PathVariable Long userId,
            @RequestParam String userType,
            @RequestParam String motivo,
            HttpSession session) {

        if (!isAdminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Acesso não autorizado"));
        }

        try {
            Admin admin = getLoggedAdmin(session);

            if ("ONG".equalsIgnoreCase(userType)) {
                Optional<Ong> ongOpt = ongRepository.findById(userId);
                if (ongOpt.isPresent()) {
                    Ong ong = ongOpt.get();
                    ong.setBanido(false);
                    ong.setAtivo(true);
                    ong.setTipoBanimento(null);
                    ong.setDataFimBanimento(null);
                    ong.setMotivoBanimento("Desbanido: " + motivo);
                    ong.setAdminResponsavelBanimento(admin.getNome() + " (Desbanimento)");
                    ongRepository.save(ong);

                    System.out.println("✅ ONG " + ong.getNome() + " desbanida por " + admin.getNome());
                    return ResponseEntity.ok(Map.of("message", "ONG desbanida com sucesso!"));
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "ONG não encontrada"));
                }
            } else if ("Voluntário".equalsIgnoreCase(userType)) {
                Optional<Voluntario> volOpt = voluntarioRepository.findById(userId);
                if (volOpt.isPresent()) {
                    Voluntario voluntario = volOpt.get();
                    voluntario.setBanido(false);
                    voluntario.setAtivo(true);
                    voluntario.setTipoBanimento(null);
                    voluntario.setDataFimBanimento(null);
                    voluntario.setMotivoBanimento("Desbanido: " + motivo);
                    voluntario.setAdminResponsavelBanimento(admin.getNome() + " (Desbanimento)");
                    voluntarioRepository.save(voluntario);

                    System.out.println("✅ Voluntário " + voluntario.getNomeVoluntario() + " desbanido por " + admin.getNome());
                    return ResponseEntity.ok(Map.of("message", "Voluntário desbanido com sucesso!"));
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Voluntário não encontrado"));
                }
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Tipo de usuário inválido"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro interno: " + e.getMessage()));
        }
    }

    // ===== ENDPOINT PARA VERIFICAR BANIMENTOS TEMPORÁRIOS =====

    @PostMapping("/api/verificar-banimentos-temporarios")
    @ResponseBody
    public ResponseEntity<Map<String, String>> verificarBanimentosTemporariosExpirados(HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Acesso não autorizado"));
        }

        try {
            int desbanidos = 0;
            LocalDateTime agora = LocalDateTime.now();

            // Verificar ONGs com banimento temporário expirado
            List<Ong> ongsExpiradas = ongRepository.findByBanidoTrueAndTipoBanimentoAndDataFimBanimentoBefore(
                    Ong.TipoBanimento.TEMPORARIO, agora);

            for (Ong ong : ongsExpiradas) {
                ong.setBanido(false);
                ong.setAtivo(true);
                ong.setTipoBanimento(null);
                ong.setDataFimBanimento(null);
                ong.setMotivoBanimento(ong.getMotivoBanimento() + " (Banimento temporário expirado automaticamente)");
                ongRepository.save(ong);
                desbanidos++;
                System.out.println("✅ ONG " + ong.getNome() + " desbanida automaticamente (banimento temporário expirado)");
            }

            // Verificar Voluntários com banimento temporário expirado
            List<Voluntario> voluntariosExpirados = voluntarioRepository.findByBanidoTrueAndTipoBanimentoAndDataFimBanimentoBefore(
                    Voluntario.TipoBanimento.TEMPORARIO, agora);

            for (Voluntario voluntario : voluntariosExpirados) {
                voluntario.setBanido(false);
                voluntario.setAtivo(true);
                voluntario.setTipoBanimento(null);
                voluntario.setDataFimBanimento(null);
                voluntario.setMotivoBanimento(voluntario.getMotivoBanimento() + " (Banimento temporário expirado automaticamente)");
                voluntarioRepository.save(voluntario);
                desbanidos++;
                System.out.println("✅ Voluntário " + voluntario.getNomeVoluntario() + " desbanido automaticamente (banimento temporário expirado)");
            }

            return ResponseEntity.ok(Map.of("message", "Verificação concluída: " + desbanidos + " usuários desbanidos automaticamente"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao verificar banimentos: " + e.getMessage()));
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
            @RequestParam String userType,
            @RequestParam String banType,
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
            @RequestParam String target,
            @RequestParam String type,
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
            @RequestParam(required = false) String periodo,
            HttpSession session) {

        if (!isAdminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Map<String, Object> relatorio = new HashMap<>();

            LocalDateTime inicio = LocalDateTime.now().minusMonths(1);
            LocalDateTime fim = LocalDateTime.now();

            if ("week".equals(periodo)) {
                inicio = LocalDateTime.now().minusWeeks(1);
            } else if ("year".equals(periodo)) {
                inicio = LocalDateTime.now().minusYears(1);
            }

            relatorio.put("periodo", periodo != null ? periodo : "month");
            relatorio.put("dataInicio", inicio.toString());
            relatorio.put("dataFim", fim.toString());

            relatorio.put("novasOngs", contarNovasOngsPeriodo(inicio, fim));
            relatorio.put("novosVoluntarios", contarNovosVoluntariosPeriodo(inicio, fim));
            relatorio.put("vagasCriadas", contarVagasCriadasPeriodo(inicio, fim));
            relatorio.put("inscricoesRealizadas", contarInscricoesPeriodo(inicio, fim));
            relatorio.put("reportsRecebidos", contarReportsPeriodo(inicio, fim));

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
            @RequestParam(required = false) String level,
            HttpSession session) {

        if (!isAdminLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
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

            Map<String, Object> stats = obterEstatisticasCompletas();
            resultado.put("estatisticas", stats);

            Map<String, Object> testesConexao = new HashMap<>();
            try {
                testesConexao.put("ongsCount", ongRepository.count());
                testesConexao.put("voluntariosCount", voluntarioRepository.count());
                testesConexao.put("vagasCount", vagasRepository.count());
                testesConexao.put("reportsCount", reportRepository.count());
                testesConexao.put("inscricoesCount", inscricaoRepository.count());
                testesConexao.put("reportsVoluntarioCount", reportVoluntarioRepository.count());

            } catch (Exception e) {
                testesConexao.put("erro", "Erro ao acessar repositórios: " + e.getMessage());
            }
            resultado.put("testesConexao", testesConexao);

            if (session != null) {
                Map<String, Object> sessaoInfo = new HashMap<>();
                sessaoInfo.put("userType", session.getAttribute("userType"));
                sessaoInfo.put("userEmail", session.getAttribute("userEmail"));
                sessaoInfo.put("userName", session.getAttribute("userName"));
                sessaoInfo.put("adminLevel", session.getAttribute("adminLevel"));
                resultado.put("sessaoInfo", sessaoInfo);
            }

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

            // Inscrições
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

            // Estatísticas de reports de vagas
            long reportsPendentesVagas = contarReportsPorStatus(Report.StatusReport.PENDENTE);
            stats.put("reportsPendentesVagas", reportsPendentesVagas);
            stats.put("reportsResolvidosVagas", contarReportsPorStatus(Report.StatusReport.RESOLVIDO));
            stats.put("reportsRejeitadosVagas", contarReportsPorStatus(Report.StatusReport.REJEITADO));

            // Estatísticas de reports de voluntários
            long reportsPendentesVoluntarios = contarReportsVoluntarioPorStatus(ReportVoluntario.StatusReportVoluntario.PENDENTE);
            stats.put("reportsPendentesVoluntarios", reportsPendentesVoluntarios);
            stats.put("reportsResolvidosVoluntarios", contarReportsVoluntarioPorStatus(ReportVoluntario.StatusReportVoluntario.RESOLVIDO));
            stats.put("reportsRejeitadosVoluntarios", contarReportsVoluntarioPorStatus(ReportVoluntario.StatusReportVoluntario.REJEITADO));

            // Total de reports pendentes
            stats.put("reportsPendentes", reportsPendentesVagas);
            stats.put("reportsVoluntarioPendentes", reportsPendentesVoluntarios);
            stats.put("totalReportsPendentes", reportsPendentesVagas + reportsPendentesVoluntarios);

            // Estatísticas temporais
            LocalDate hoje = LocalDate.now();
            stats.put("vagasHoje", contarVagasHoje(hoje));
            stats.put("vagasAmanha", contarVagasAmanha(hoje.plusDays(1)));

            // Usuários banidos - DADOS REAIS
            long ongsBanidas = ongRepository.countByBanidoTrue();
            long voluntariosBanidos = voluntarioRepository.countByBanidoTrue();
            long totalBanidos = ongsBanidas + voluntariosBanidos;

            stats.put("usuariosBanidos", totalBanidos);
            stats.put("ongsBanidas", ongsBanidas);
            stats.put("voluntariosBanidos", voluntariosBanidos);

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
            stats.put("ongsBanidas", 0L);
            stats.put("voluntariosBanidos", 0L);
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

            if (report.getAssunto() != null) {
                map.put("assunto", report.getAssunto().getDescricao());
            } else {
                map.put("assunto", "Denúncia");
            }

            map.put("motivo", report.getMotivo() != null ? report.getMotivo() : "Sem descrição");
            map.put("dataReport", report.getDataReport());
            map.put("respostaAdmin", report.getRespostaAdmin());

            if (report.getVoluntario() != null) {
                map.put("reportadoPor", report.getVoluntario().getNomeVoluntario());
            } else {
                map.put("reportadoPor", "Usuário não identificado");
            }

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

    private Map<String, Object> convertReportVoluntarioToMap(ReportVoluntario report) {
        Map<String, Object> map = new HashMap<>();

        try {
            map.put("id", report.getId());
            map.put("status", report.getStatus() != null ? report.getStatus().name() : "PENDENTE");

            if (report.getAssunto() != null) {
                map.put("assunto", report.getAssunto().getDescricao());
            } else {
                map.put("assunto", "Denúncia de Voluntário");
            }

            map.put("motivo", report.getMotivo() != null ? report.getMotivo() : "Sem descrição");
            map.put("dataReport", report.getDataReport());
            map.put("respostaAdmin", report.getRespostaAdmin());

            if (report.getVoluntario() != null) {
                map.put("voluntarioReportado", report.getVoluntario().getNomeVoluntario());
                map.put("voluntarioId", report.getVoluntario().getId());
                map.put("voluntario", report.getVoluntario().getNomeVoluntario());
            } else {
                map.put("voluntarioReportado", "Voluntário não identificado");
                map.put("voluntarioId", null);
                map.put("voluntario", "Voluntário não identificado");
            }

            if (report.getOng() != null) {
                map.put("ongReportou", report.getOng().getNome());
                map.put("ongId", report.getOng().getId());
                map.put("ong", report.getOng().getNome());
            } else {
                map.put("ongReportou", "ONG não identificada");
                map.put("ongId", null);
                map.put("ong", "ONG não identificada");
            }

            if (report.getVaga() != null) {
                map.put("vaga", report.getVaga().getNome());
                map.put("vagaId", report.getVaga().getId());
            } else {
                map.put("vaga", "Não relacionado a vaga específica");
                map.put("vagaId", null);
            }

            if (report.getPrioridade() != null) {
                map.put("prioridade", report.getPrioridade().name());
            } else {
                map.put("prioridade", "NORMAL");
            }

            map.put("temEvidencia", report.temEvidencia());
            if (report.temEvidencia()) {
                map.put("evidenciaUrl", report.getEvidenciaUrl());
                map.put("evidenciaNome", report.getEvidenciaNome());
            }

        } catch (Exception e) {
            System.err.println("❌ Erro ao converter ReportVoluntario para map: " + e.getMessage());
            e.printStackTrace();

            map.put("id", report.getId());
            map.put("status", "ERRO");
            map.put("assunto", "Erro ao carregar");
            map.put("motivo", "Erro ao carregar dados do report");
            map.put("voluntario", "Erro");
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
                    if (report.getVaga() != null && report.getVaga().getOng() != null) {
                        enviarAdvertenciaOng(report.getVaga().getOng(), banReason, comment, admin);
                    }
                    report.setStatus(Report.StatusReport.RESOLVIDO);
                    report.setRespostaAdmin("Advertência enviada: " + comment);
                    report.setDataResposta(LocalDateTime.now());
                    reportRepository.save(report);

                    System.out.println("⚠️ Advertência enviada para ONG do report " + report.getId());
                    return "Advertência enviada para a ONG!";

                case "ban":
                    if (report.getVaga() != null && report.getVaga().getOng() != null) {
                        banirOngReal(report.getVaga().getOng(), banType, banReason, comment, admin);
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
            switch (action.toLowerCase()) {
                case "resolve":
                    report.setStatus(ReportVoluntario.StatusReportVoluntario.RESOLVIDO);
                    report.setRespostaAdmin(comment != null ? comment : "Resolvido pela administração");
                    report.setDataResposta(LocalDateTime.now());
                    reportVoluntarioRepository.save(report);

                    System.out.println("✅ Report de voluntário " + report.getId() + " resolvido por admin " + admin.getNome());
                    return "Report de voluntário resolvido com sucesso!";

                case "reject":
                    report.setStatus(ReportVoluntario.StatusReportVoluntario.REJEITADO);
                    report.setRespostaAdmin(comment != null ? comment : "Report rejeitado pela administração");
                    report.setDataResposta(LocalDateTime.now());
                    reportVoluntarioRepository.save(report);

                    System.out.println("❌ Report de voluntário " + report.getId() + " rejeitado por admin " + admin.getNome());
                    return "Report de voluntário rejeitado!";

                case "warn":
                    if (report.getVoluntario() != null) {
                        enviarAdvertenciaVoluntario(report.getVoluntario(), banReason, comment, admin);
                    }
                    report.setStatus(ReportVoluntario.StatusReportVoluntario.ADVERTENCIA_ENVIADA);
                    report.setRespostaAdmin("Advertência enviada: " + comment);
                    report.setDataResposta(LocalDateTime.now());
                    reportVoluntarioRepository.save(report);

                    System.out.println("⚠️ Advertência enviada para voluntário do report " + report.getId());
                    return "Advertência enviada para o voluntário!";

                case "ban":
                    if (report.getVoluntario() != null) {
                        banirVoluntarioReal(report.getVoluntario(), banType, banReason, comment, admin);
                    }
                    report.setStatus(ReportVoluntario.StatusReportVoluntario.SUSPENSO);
                    report.setRespostaAdmin("Voluntário banido: " + comment);
                    report.setDataResposta(LocalDateTime.now());
                    reportVoluntarioRepository.save(report);

                    System.out.println("🚫 Voluntário banido através do report " + report.getId());
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

    private double calcularCrescimentoOngs() {
        return 8.5; // Valor simulado
    }

    private double calcularCrescimentoVoluntarios() {
        return 15.2; // Valor simulado
    }

    // ===== MÉTODOS DE BANIMENTO E ADVERTÊNCIA =====

    private void enviarAdvertenciaOng(Ong ong, String motivo, String detalhes, Admin admin) {
        try {
            System.out.println("⚠️ ===== REGISTRO DE ADVERTÊNCIA PARA ONG =====");
            System.out.println("   ONG: " + ong.getNome() + " (ID: " + ong.getId() + ")");
            System.out.println("   Email: " + ong.getEmail());
            System.out.println("   Motivo: " + motivo);
            System.out.println("   Detalhes: " + detalhes);
            System.out.println("   Admin Responsável: " + admin.getNome());
            System.out.println("   Data/Hora: " + LocalDateTime.now());
            System.out.println("==============================================");
        } catch (Exception e) {
            System.err.println("❌ Erro ao registrar advertência para ONG: " + e.getMessage());
        }
    }

    private void enviarAdvertenciaVoluntario(Voluntario voluntario, String motivo, String detalhes, Admin admin) {
        try {
            System.out.println("⚠️ ===== REGISTRO DE ADVERTÊNCIA PARA VOLUNTÁRIO =====");
            System.out.println("   Voluntário: " + voluntario.getNomeVoluntario() + " (ID: " + voluntario.getId() + ")");
            System.out.println("   Email: " + voluntario.getEmail());
            System.out.println("   Motivo: " + motivo);
            System.out.println("   Detalhes: " + detalhes);
            System.out.println("   Admin Responsável: " + admin.getNome());
            System.out.println("   Data/Hora: " + LocalDateTime.now());
            System.out.println("=====================================================");
        } catch (Exception e) {
            System.err.println("❌ Erro ao registrar advertência para voluntário: " + e.getMessage());
        }
    }

    private void banirOngReal(Ong ong, String tipoBanimentoStr, String motivo, String detalhes, Admin admin) {
        Ong.TipoBanimento tipoBanimento = "PERMANENTE".equalsIgnoreCase(tipoBanimentoStr)
                ? Ong.TipoBanimento.PERMANENTE
                : Ong.TipoBanimento.TEMPORARIO;

        // Atualiza os campos de banimento da ONG
        ong.setBanido(true);
        ong.setAtivo(false);
        ong.setTipoBanimento(tipoBanimento);
        ong.setMotivoBanimento(motivo + ". Detalhes: " + detalhes);
        ong.setDataBanimento(LocalDateTime.now());
        ong.setAdminResponsavelBanimento(admin.getNome());

        if (tipoBanimento == Ong.TipoBanimento.TEMPORARIO) {
            ong.setDataFimBanimento(LocalDateTime.now().plusDays(30));
        } else {
            ong.setDataFimBanimento(null);
        }
        ongRepository.save(ong);
        System.out.println("✅ ONG '" + ong.getNome() + "' foi BANIDA por " + admin.getNome());

        // Desativa vagas ativas e cancela inscrições
        List<Vagas> vagasAtivas = vagasRepository.findByOngAndStatus(ong, Vagas.StatusVaga.ATIVA);
        for (Vagas vaga : vagasAtivas) {
            vaga.setStatus(Vagas.StatusVaga.INTERROMPIDA);
            List<Inscricao> inscricoes = inscricaoRepository.findByVaga(vaga);
            if (!inscricoes.isEmpty()) {
                inscricaoRepository.deleteAll(inscricoes);
                System.out.println("   -> " + inscricoes.size() + " inscrições canceladas para a vaga: " + vaga.getNome());
            }
        }
        vagasRepository.saveAll(vagasAtivas);
        System.out.println("   -> " + vagasAtivas.size() + " vagas ativas da ONG foram interrompidas.");
    }

    private void banirVoluntarioReal(Voluntario voluntario, String tipoBanimentoStr, String motivo, String detalhes, Admin admin) {
        Voluntario.TipoBanimento tipoBanimento = "PERMANENTE".equalsIgnoreCase(tipoBanimentoStr)
                ? Voluntario.TipoBanimento.PERMANENTE
                : Voluntario.TipoBanimento.TEMPORARIO;

        // Atualiza os campos de banimento do voluntário
        voluntario.setBanido(true);
        voluntario.setAtivo(false);
        voluntario.setTipoBanimento(tipoBanimento);
        voluntario.setMotivoBanimento(motivo + ". Detalhes: " + detalhes);
        voluntario.setDataBanimento(LocalDateTime.now());
        voluntario.setAdminResponsavelBanimento(admin.getNome());

        if (tipoBanimento == Voluntario.TipoBanimento.TEMPORARIO) {
            voluntario.setDataFimBanimento(LocalDateTime.now().plusDays(30));
        } else {
            voluntario.setDataFimBanimento(null);
        }
        voluntarioRepository.save(voluntario);
        System.out.println("✅ Voluntário '" + voluntario.getNomeVoluntario() + "' foi BANIDO por " + admin.getNome());

        // Cancela inscrições futuras
        List<Inscricao> inscricoesFuturas = inscricaoRepository.findInscricoesFuturas(voluntario, LocalDate.now());
        if (!inscricoesFuturas.isEmpty()) {
            inscricaoRepository.deleteAll(inscricoesFuturas);
            System.out.println("   -> " + inscricoesFuturas.size() + " inscrições futuras do voluntário foram canceladas.");
        }
    }

    private String executarBanimento(Long userId, String userType, String banType, String reason, String details, boolean notifyUser, Admin admin) {
        try {
            if ("ong".equals(userType)) {
                return ongRepository.findById(userId).map(ong -> {
                    banirOngReal(ong, banType, reason, details, admin);
                    if (notifyUser) {
                        // Aqui entraria a lógica de notificação real
                        // enviarNotificacaoBanimento(ong.getEmail(), ong.getNome(), reason, banType);
                    }
                    return "ONG '" + ong.getNome() + "' banida com sucesso!";
                }).orElse("ONG não encontrada!");
            } else if ("voluntario".equals(userType)) {
                return voluntarioRepository.findById(userId).map(voluntario -> {
                    banirVoluntarioReal(voluntario, banType, reason, details, admin);
                    if (notifyUser) {
                        // Aqui entraria a lógica de notificação real
                        // enviarNotificacaoBanimento(voluntario.getEmail(), voluntario.getNomeVoluntario(), reason, banType);
                    }
                    return "Voluntário '" + voluntario.getNomeVoluntario() + "' banido com sucesso!";
                }).orElse("Voluntário não encontrado!");
            } else {
                return "Tipo de usuário inválido!";
            }
        } catch (Exception e) {
            System.err.println("❌ Erro ao executar banimento para " + userType + " ID " + userId + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erro ao executar banimento: " + e.getMessage(), e);
        }
    }

    private String enviarAdvertencia(Long userId, String userType, String reason, String message, Admin admin) {
        try {
            if ("ong".equals(userType)) {
                return ongRepository.findById(userId).map(ong -> {
                    enviarAdvertenciaOng(ong, reason, message, admin);
                    return "Advertência registrada para a ONG '" + ong.getNome() + "'!";
                }).orElse("ONG não encontrada!");
            } else if ("voluntario".equals(userType)) {
                return voluntarioRepository.findById(userId).map(voluntario -> {
                    enviarAdvertenciaVoluntario(voluntario, reason, message, admin);
                    return "Advertência registrada para o voluntário '" + voluntario.getNomeVoluntario() + "'!";
                }).orElse("Voluntário não encontrado!");
            } else {
                return "Tipo de usuário inválido!";
            }
        } catch (Exception e) {
            System.err.println("❌ Erro ao enviar advertência para " + userType + " ID " + userId + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erro ao enviar advertência: " + e.getMessage(), e);
        }
    }

    // ===== MÉTODOS AUXILIARES =====

    private int processarEnvioNotificacao(String target, String type, String title, String message, boolean push, List<Long> specificUsers, Admin admin) {
        // Implementação simulada - retorna 0 usuários notificados
        System.out.println("📢 Simulando envio de notificação:");
        System.out.println("   Target: " + target);
        System.out.println("   Type: " + type);
        System.out.println("   Title: " + title);
        System.out.println("   Message: " + message);
        System.out.println("   Admin: " + admin.getNome());
        return 0;
    }

    private String executarBackup(Admin admin) {
        String backupId = "backup_" + System.currentTimeMillis();
        System.out.println("💾 Simulando criação de backup:");
        System.out.println("   Backup ID: " + backupId);
        System.out.println("   Admin responsável: " + admin.getNome());
        System.out.println("   Data/Hora: " + LocalDateTime.now());
        return backupId;
    }

    // ===== MÉTODOS DE RELATÓRIO (SIMULADOS) =====

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
        List<Map<String, Object>> ongs = new ArrayList<>();
        Map<String, Object> ong1 = new HashMap<>();
        ong1.put("nome", "ONG Esperança");
        ong1.put("vagasCriadas", 25);
        ongs.add(ong1);
        return ongs;
    }

    private List<Map<String, Object>> obterVoluntariosMaisAtivos(LocalDateTime inicio, LocalDateTime fim) {
        List<Map<String, Object>> voluntarios = new ArrayList<>();
        Map<String, Object> vol1 = new HashMap<>();
        vol1.put("nome", "João Silva");
        vol1.put("inscricoes", 15);
        voluntarios.add(vol1);
        return voluntarios;
    }

    private List<Map<String, Object>> obterCategoriasPopulares(LocalDateTime inicio, LocalDateTime fim) {
        List<Map<String, Object>> categorias = new ArrayList<>();
        Map<String, Object> cat1 = new HashMap<>();
        cat1.put("categoria", "Meio Ambiente");
        cat1.put("vagas", 45);
        categorias.add(cat1);
        return categorias;
    }

    private List<Map<String, Object>> obterLogsDoSistema(int page, int size, String level) {
        List<Map<String, Object>> logs = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            Map<String, Object> log = new HashMap<>();
            log.put("id", i + 1 + (page * size));
            log.put("timestamp", LocalDateTime.now().minusHours(i).toString());
            log.put("level", level != null ? level : "INFO");
            log.put("message", "Log simulado #" + (i + 1));
            log.put("source", "AdminController");
            logs.add(log);
        }

        return logs;
    }

    // ===== HANDLER DE ERROS =====

    @ExceptionHandler(Exception.class)
    public ModelAndView handleException(Exception e, HttpSession session) {
        System.err.println("❌ Erro no Blue Wave AdminController: " + e.getMessage());
        e.printStackTrace();

        ModelAndView mv = new ModelAndView("dashboard");
        mv.addObject("erro", "Erro interno do sistema Blue Wave: " + e.getMessage());

        try {
            Admin admin = getLoggedAdmin(session);
            if (admin != null) {
                mv.addObject("admin", admin);
                mv.addObject("stats", obterEstatisticasCompletas());
            }
        } catch (Exception ex) {
            System.err.println("❌ Erro secundário ao tratar exceção: " + ex.getMessage());
        }

        return mv;
    }
}

