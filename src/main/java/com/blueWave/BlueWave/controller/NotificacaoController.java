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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/notificacoes")
public class NotificacaoController {

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Autowired
    private VoluntarioRepository voluntarioRepository;

    @Autowired
    private OngRepository ongRepository;

    // Verificar se é voluntário logado
    private boolean isVoluntarioLoggedIn(HttpSession session) {
        String userType = (String) session.getAttribute("userType");
        String userEmail = (String) session.getAttribute("userEmail");
        return "voluntario".equals(userType) && userEmail != null;
    }

    // Verificar se é ONG logada
    private boolean isOngLoggedIn(HttpSession session) {
        String userType = (String) session.getAttribute("userType");
        String userEmail = (String) session.getAttribute("userEmail");
        return "ong".equals(userType) && userEmail != null;
    }

    // Obter voluntário logado
    private Voluntario getLoggedVoluntario(HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return null;
        }
        String userEmail = (String) session.getAttribute("userEmail");
        return voluntarioRepository.findVoluntarioByEmail(userEmail);
    }

    // Obter ONG logada
    private Ong getLoggedOng(HttpSession session) {
        if (!isOngLoggedIn(session)) {
            return null;
        }
        String userEmail = (String) session.getAttribute("userEmail");
        return ongRepository.findOngByEmail(userEmail);
    }

    // API: Listar notificações do voluntário
    @GetMapping("/api/minhas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listarMinhasNotificacoes(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "apenasNaoLidas", defaultValue = "false") boolean apenasNaoLidas,
            HttpSession session) {

        if (!isVoluntarioLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Notificacao> notificacoesPage;

            if (apenasNaoLidas) {
                // Corrigido: usando dataEnvio conforme o repository
                notificacoesPage = notificacaoRepository.findByVoluntarioAndLidaFalseOrderByDataEnvioDesc(voluntario, pageable);
            } else {
                notificacoesPage = notificacaoRepository.findByVoluntarioOrderByDataEnvioDesc(voluntario, pageable);
            }

            // Contar não lidas
            long naoLidas = notificacaoRepository.countByVoluntarioAndLidaFalse(voluntario);

            // Buscar notificações urgentes não lidas
            List<Notificacao> urgentes = notificacaoRepository.findNotificacaoUrgentesNaoLidas(voluntario);

            Map<String, Object> response = new HashMap<>();
            response.put("notificacoes", notificacoesPage.getContent());
            response.put("totalPages", notificacoesPage.getTotalPages());
            response.put("totalElements", notificacoesPage.getTotalElements());
            response.put("currentPage", page);
            response.put("size", size);
            response.put("hasNext", notificacoesPage.hasNext());
            response.put("hasPrevious", notificacoesPage.hasPrevious());
            response.put("naoLidas", naoLidas);
            response.put("urgentes", urgentes);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // API: Contar notificações não lidas
    @GetMapping("/api/contador")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> contarNaoLidas(HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            long naoLidas = notificacaoRepository.countByVoluntarioAndLidaFalse(voluntario);
            long urgentes = notificacaoRepository.findNotificacaoUrgentesNaoLidas(voluntario).size();

            Map<String, Object> response = new HashMap<>();
            response.put("naoLidas", naoLidas);
            response.put("urgentes", urgentes);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // API: Marcar notificação como lida
    @PostMapping("/api/{notificacaoId}/marcar-lida")
    @ResponseBody
    public ResponseEntity<String> marcarComoLida(
            @PathVariable Long notificacaoId,
            HttpSession session) {

        if (!isVoluntarioLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Não autorizado");
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Voluntário não encontrado");
        }

        try {
            Notificacao notificacao = notificacaoRepository.findById(notificacaoId).orElse(null);

            if (notificacao == null || !notificacao.getVoluntario().getId().equals(voluntario.getId())) {
                return ResponseEntity.notFound().build();
            }

            if (!notificacao.isLida()) {
                notificacao.setLida(true);
                notificacao.setDataLeitura(LocalDateTime.now());
                notificacaoRepository.save(notificacao);
            }

            return ResponseEntity.ok("Notificação marcada como lida");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno");
        }
    }

    // API: Marcar todas as notificações como lidas
    @PostMapping("/api/marcar-todas-lidas")
    @ResponseBody
    public ResponseEntity<String> marcarTodasComoLidas(HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Não autorizado");
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Voluntário não encontrado");
        }

        try {
            List<Notificacao> naoLidas = notificacaoRepository.findByVoluntarioAndLidaFalseOrderByDataEnvioDesc(voluntario);

            for (Notificacao notificacao : naoLidas) {
                notificacao.setLida(true);
                notificacao.setDataLeitura(LocalDateTime.now());
            }

            notificacaoRepository.saveAll(naoLidas);

            return ResponseEntity.ok("Todas as notificações foram marcadas como lidas");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno");
        }
    }

    // API: Deletar notificação
    @DeleteMapping("/api/{notificacaoId}")
    @ResponseBody
    public ResponseEntity<String> deletarNotificacao(
            @PathVariable Long notificacaoId,
            HttpSession session) {

        if (!isVoluntarioLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Não autorizado");
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Voluntário não encontrado");
        }

        try {
            Notificacao notificacao = notificacaoRepository.findById(notificacaoId).orElse(null);

            if (notificacao == null || !notificacao.getVoluntario().getId().equals(voluntario.getId())) {
                return ResponseEntity.notFound().build();
            }

            notificacaoRepository.deleteById(notificacaoId);

            return ResponseEntity.ok("Notificação deletada com sucesso");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno");
        }
    }

    // API: Buscar notificações recentes (últimas 24h)
    @GetMapping("/api/recentes")
    @ResponseBody
    public ResponseEntity<List<Notificacao>> buscarRecentes(HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            LocalDateTime ontemMesmoHorario = LocalDateTime.now().minusHours(24);
            List<Notificacao> recentes = notificacaoRepository.findNotificacoesRecentes(voluntario, ontemMesmoHorario);

            return ResponseEntity.ok(recentes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== ENDPOINTS PARA ONG ENVIAR NOTIFICAÇÕES =====

    // API: Enviar notificação para voluntário
    @PostMapping("/api/enviar")
    @ResponseBody
    public ResponseEntity<String> enviarNotificacao(
            @RequestParam("voluntarioId") Long voluntarioId,
            @RequestParam("tipo") String tipo,
            @RequestParam("titulo") String titulo,
            @RequestParam("mensagem") String mensagem,
            @RequestParam(value = "prioridade", defaultValue = "NORMAL") String prioridade,
            @RequestParam(value = "urlAcao", required = false) String urlAcao,
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
            if (titulo == null || titulo.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Título é obrigatório");
            }

            if (mensagem == null || mensagem.trim().length() < 10) {
                return ResponseEntity.badRequest().body("Mensagem deve ter pelo menos 10 caracteres");
            }

            // Buscar voluntário
            Voluntario voluntario = voluntarioRepository.findById(voluntarioId).orElse(null);
            if (voluntario == null) {
                return ResponseEntity.badRequest().body("Voluntário não encontrado");
            }

            // Converter enums
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

            // Criar notificação
            Notificacao notificacao = new Notificacao();
            notificacao.setVoluntario(voluntario);
            notificacao.setOng(ong);
            notificacao.setTipo(tipoEnum);
            notificacao.setTitulo(titulo.trim());
            notificacao.setMensagem(mensagem.trim());
            notificacao.setPrioridade(prioridadeEnum);
            notificacao.setDataEnvio(LocalDateTime.now());
            notificacao.setLida(false);

            if (urlAcao != null && !urlAcao.trim().isEmpty()) {
                notificacao.setUrlAcao(urlAcao.trim());
            }

            notificacaoRepository.save(notificacao);

            return ResponseEntity.ok("Notificação enviada com sucesso!");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno: " + e.getMessage());
        }
    }

    // API: Enviar notificação em lote
    @PostMapping("/api/enviar-lote")
    @ResponseBody
    public ResponseEntity<String> enviarNotificacaoLote(
            @RequestParam("voluntarioIds") List<Long> voluntarioIds,
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
            // Validações
            if (voluntarioIds == null || voluntarioIds.isEmpty()) {
                return ResponseEntity.badRequest().body("Liste pelo menos um voluntário");
            }

            if (titulo == null || titulo.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Título é obrigatório");
            }

            if (mensagem == null || mensagem.trim().length() < 10) {
                return ResponseEntity.badRequest().body("Mensagem deve ter pelo menos 10 caracteres");
            }

            // Converter enums
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

            int sucessos = 0;
            int erros = 0;

            // Enviar para cada voluntário
            for (Long voluntarioId : voluntarioIds) {
                try {
                    Voluntario voluntario = voluntarioRepository.findById(voluntarioId).orElse(null);
                    if (voluntario != null) {
                        Notificacao notificacao = new Notificacao();
                        notificacao.setVoluntario(voluntario);
                        notificacao.setOng(ong);
                        notificacao.setTipo(tipoEnum);
                        notificacao.setTitulo(titulo.trim());
                        notificacao.setMensagem(mensagem.trim());
                        notificacao.setPrioridade(prioridadeEnum);
                        notificacao.setDataEnvio(LocalDateTime.now());
                        notificacao.setLida(false);

                        notificacaoRepository.save(notificacao);
                        sucessos++;
                    } else {
                        erros++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    erros++;
                }
            }

            String resultado = String.format("Notificações enviadas: %d sucessos, %d erros", sucessos, erros);
            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno: " + e.getMessage());
        }
    }

    // ===== MÉTODOS UTILITÁRIOS =====

    // Método para criar notificação de sistema
    public static void criarNotificacaoSistema(NotificacaoRepository notificacaoRepository,
                                               Voluntario voluntario,
                                               String titulo,
                                               String mensagem,
                                               Notificacao.TipoNotificacao tipo) {
        try {
            Notificacao notificacao = new Notificacao();
            notificacao.setVoluntario(voluntario);
            notificacao.setTipo(tipo);
            notificacao.setTitulo(titulo);
            notificacao.setMensagem(mensagem);
            notificacao.setPrioridade(Notificacao.PrioridadeNotificacao.NORMAL);
            notificacao.setDataEnvio(LocalDateTime.now());
            notificacao.setLida(false);

            notificacaoRepository.save(notificacao);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Método para criar notificação de inscrição confirmada
    public static void criarNotificacaoInscricao(NotificacaoRepository notificacaoRepository,
                                                 Voluntario voluntario,
                                                 Vagas vaga,
                                                 Ong ong) {
        try {
            String titulo = "Inscrição Confirmada!";
            String mensagem = String.format(
                    "Sua inscrição na vaga '%s' da ONG %s foi confirmada com sucesso!\n\n" +
                            "Data da atividade: %s\n" +
                            "Local: %s\n\n" +
                            "Não esqueça de comparecer no dia e horário marcados. Em caso de imprevisto, " +
                            "cancele sua inscrição com antecedência.",
                    vaga.getNome(),
                    ong.getNome(),
                    vaga.getData(),
                    vaga.getEnderecoCompleto().isEmpty() ? ong.getCidade() + ", " + ong.getEstado() : vaga.getEnderecoCompleto()
            );

            Notificacao notificacao = new Notificacao();
            notificacao.setVoluntario(voluntario);
            notificacao.setOng(ong);
            notificacao.setTipo(Notificacao.TipoNotificacao.INSCRICAO_CONFIRMADA);
            notificacao.setTitulo(titulo);
            notificacao.setMensagem(mensagem);
            notificacao.setPrioridade(Notificacao.PrioridadeNotificacao.NORMAL);
            notificacao.setDataEnvio(LocalDateTime.now());
            notificacao.setLida(false);
            notificacao.setVaga(vaga);
            notificacao.setUrlAcao("/inscricao/minhasVagas");

            notificacaoRepository.save(notificacao);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Método para criar notificação de lembrete
    public static void criarNotificacaoLembrete(NotificacaoRepository notificacaoRepository,
                                                Voluntario voluntario,
                                                Vagas vaga,
                                                Ong ong) {
        try {
            String titulo = "Lembrete: Atividade Amanhã!";
            String mensagem = String.format(
                    "Lembrete: Você tem uma atividade voluntária amanhã!\n\n" +
                            "Vaga: %s\n" +
                            "ONG: %s\n" +
                            "Data: %s\n" +
                            "Local: %s\n\n" +
                            "Prepare-se e não esqueça de comparecer!",
                    vaga.getNome(),
                    ong.getNome(),
                    vaga.getData(),
                    vaga.getEnderecoCompleto().isEmpty() ? ong.getCidade() + ", " + ong.getEstado() : vaga.getEnderecoCompleto()
            );

            Notificacao notificacao = new Notificacao();
            notificacao.setVoluntario(voluntario);
            notificacao.setOng(ong);
            notificacao.setTipo(Notificacao.TipoNotificacao.LEMBRETE);
            notificacao.setTitulo(titulo);
            notificacao.setMensagem(mensagem);
            notificacao.setPrioridade(Notificacao.PrioridadeNotificacao.ALTA);
            notificacao.setDataEnvio(LocalDateTime.now());
            notificacao.setLida(false);
            notificacao.setVaga(vaga);

            notificacaoRepository.save(notificacao);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}