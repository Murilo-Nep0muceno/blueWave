package com.blueWave.BlueWave.controller;

import com.blueWave.BlueWave.model.Inscricao;
import com.blueWave.BlueWave.model.Voluntario;
import com.blueWave.BlueWave.repository.InscricaoRepository;
import com.blueWave.BlueWave.repository.VoluntarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/voluntario")
public class PerfilVoluntarioController {

    @Autowired
    private VoluntarioRepository voluntarioRepository;

    @Autowired
    private InscricaoRepository inscricaoRepository;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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
        return voluntarioRepository.findVoluntarioByEmail(userEmail);
    }

    @GetMapping("/perfil")
    public ModelAndView verPerfil(HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return new ModelAndView("redirect:/login/voluntario");
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return new ModelAndView("redirect:/login/voluntario");
        }

        ModelAndView mv = new ModelAndView("perfilVoluntario");

        try {
            // Buscar estatísticas do voluntário
            List<Inscricao> inscricoes = inscricaoRepository.findByVoluntario(voluntario);

            long totalInscricoes = inscricoes.size();
            long vagasAtivas = inscricoes.stream()
                    .filter(inscricao -> !inscricao.getVaga().getData().isBefore(LocalDate.now()))
                    .count();
            long vagasConcluidas = totalInscricoes - vagasAtivas;

            // Calcular horas de voluntariado (estimativa: 4 horas por vaga concluída)
            long horasVoluntariado = vagasConcluidas * 4;

            // Avaliação fictícia (implementar sistema de avaliação depois)
            double avaliacaoMedia = 4.2;
            int totalAvaliacoes = 15;

            mv.addObject("voluntario", voluntario);
            mv.addObject("totalInscricoes", totalInscricoes);
            mv.addObject("vagasAtivas", vagasAtivas);
            mv.addObject("vagasConcluidas", vagasConcluidas);
            mv.addObject("horasVoluntariado", horasVoluntariado);
            mv.addObject("avaliacaoMedia", avaliacaoMedia);
            mv.addObject("totalAvaliacoes", totalAvaliacoes);

        } catch (Exception e) {
            e.printStackTrace();
            mv.addObject("erro", "Erro ao carregar dados do perfil: " + e.getMessage());
        }

        return mv;
    }

    // API Endpoints
    @GetMapping("/api/perfil")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obterPerfilApi(HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return ResponseEntity.status(401).build();
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return ResponseEntity.status(404).build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", voluntario.getId());
        response.put("nomeVoluntario", voluntario.getNomeVoluntario());
        response.put("email", voluntario.getEmail());
        response.put("telefone", voluntario.getTelefone());
        response.put("dataNascimento", voluntario.getDataNascimento());
        response.put("sexo", voluntario.getSexo());
        response.put("cpf", voluntario.getCpf());

        // Avaliação fictícia
        response.put("rating", 4.2);
        response.put("ratingCount", 15);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/estatisticas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obterEstatisticas(HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return ResponseEntity.status(401).build();
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return ResponseEntity.status(404).build();
        }

        try {
            List<Inscricao> inscricoes = inscricaoRepository.findByVoluntario(voluntario);

            long totalInscricoes = inscricoes.size();
            long vagasAtivas = inscricoes.stream()
                    .filter(inscricao -> !inscricao.getVaga().getData().isBefore(LocalDate.now()))
                    .count();
            long vagasConcluidas = totalInscricoes - vagasAtivas;
            long horasVoluntariado = vagasConcluidas * 4; // Estimativa

            Map<String, Object> response = new HashMap<>();
            response.put("totalInscricoes", totalInscricoes);
            response.put("vagasAtivas", vagasAtivas);
            response.put("vagasConcluidas", vagasConcluidas);
            response.put("horasVoluntariado", horasVoluntariado);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/api/atividades-recentes")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> obterAtividadesRecentes(HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return ResponseEntity.status(401).build();
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return ResponseEntity.status(404).build();
        }

        try {
            List<Inscricao> inscricoes = inscricaoRepository.findByVoluntario(voluntario);

            List<Map<String, Object>> atividades = inscricoes.stream()
                    .limit(6) // Últimas 6 atividades
                    .map(inscricao -> {
                        Map<String, Object> atividade = new HashMap<>();
                        atividade.put("id", inscricao.getId());
                        atividade.put("vagaNome", inscricao.getVaga() != null ? inscricao.getVaga().getNome() : "Vaga não disponível");
                        atividade.put("ongNome", inscricao.getVaga() != null && inscricao.getVaga().getOng() != null ?
                                inscricao.getVaga().getOng().getNome() : "ONG não informada");
                        atividade.put("data", inscricao.getVaga() != null ? inscricao.getVaga().getData().toString() : "");
                        atividade.put("descricao", inscricao.getVaga() != null ? inscricao.getVaga().getDescri() : "");
                        atividade.put("local", inscricao.getVaga() != null ? inscricao.getVaga().getLocalReferencia() : "");
                        atividade.put("dataInscricao", inscricao.getDataInscricao().toString());
                        return atividade;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(atividades);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PutMapping("/api/atualizar-perfil")
    @ResponseBody
    @Transactional
    public ResponseEntity<String> atualizarPerfil(@RequestBody Map<String, String> dados, HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return ResponseEntity.status(401).body("Não autenticado");
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return ResponseEntity.status(404).body("Usuário não encontrado");
        }

        try {
            // Validar dados
            String nome = dados.get("nomeVoluntario");
            String telefone = dados.get("telefone");
            String dataNascimento = dados.get("dataNascimento");
            String sexo = dados.get("sexo");

            if (nome == null || nome.trim().length() < 3) {
                return ResponseEntity.badRequest().body("Nome deve ter pelo menos 3 caracteres");
            }

            if (telefone != null) {
                telefone = telefone.replaceAll("\\D", "");
                if (telefone.length() < 10 || telefone.length() > 11) {
                    return ResponseEntity.badRequest().body("Telefone inválido");
                }
            }

            if (sexo != null && !Arrays.asList("masculino", "feminino", "indefinido").contains(sexo)) {
                return ResponseEntity.badRequest().body("Sexo inválido");
            }

            // Atualizar dados
            voluntario.setNomeVoluntario(nome.trim());
            if (telefone != null) {
                voluntario.setTelefone(telefone);
            }
            if (dataNascimento != null && !dataNascimento.trim().isEmpty()) {
                voluntario.setDataNascimento(dataNascimento);
            }
            if (sexo != null) {
                voluntario.setSexo(sexo);
            }

            voluntarioRepository.save(voluntario);

            return ResponseEntity.ok("Perfil atualizado com sucesso");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro interno do servidor");
        }
    }

    @PutMapping("/api/alterar-senha")
    @ResponseBody
    @Transactional
    public ResponseEntity<String> alterarSenha(@RequestBody Map<String, String> dados, HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return ResponseEntity.status(401).body("Não autenticado");
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return ResponseEntity.status(404).body("Usuário não encontrado");
        }

        try {
            String senhaAtual = dados.get("senhaAtual");
            String novaSenha = dados.get("novaSenha");

            if (senhaAtual == null || novaSenha == null) {
                return ResponseEntity.badRequest().body("Senhas são obrigatórias");
            }

            // Verificar senha atual
            if (!passwordEncoder.matches(senhaAtual, voluntario.getSenha())) {
                return ResponseEntity.badRequest().body("Senha atual incorreta");
            }

            // Validar nova senha
            if (novaSenha.length() < 8) {
                return ResponseEntity.badRequest().body("Nova senha deve ter pelo menos 8 caracteres");
            }

            // Atualizar senha
            voluntario.setSenha(passwordEncoder.encode(novaSenha));
            voluntarioRepository.save(voluntario);

            return ResponseEntity.ok("Senha alterada com sucesso");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro interno do servidor");
        }
    }

    @PostMapping("/api/upload-avatar")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, String>> uploadAvatar(@RequestParam("avatar") MultipartFile arquivo, HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return ResponseEntity.status(401).build();
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return ResponseEntity.status(404).build();
        }

        try {
            // Validar arquivo
            if (arquivo.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            if (arquivo.getSize() > 5 * 1024 * 1024) { // 5MB
                return ResponseEntity.badRequest().build();
            }

            String contentType = arquivo.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().build();
            }

            // Salvar arquivo
            String nomeArquivo = "avatar_" + voluntario.getId() + "_" + System.currentTimeMillis() +
                    getFileExtension(arquivo.getOriginalFilename());

            String diretorioUpload = "uploads/avatars/";
            Path diretorio = Paths.get(diretorioUpload);

            if (!Files.exists(diretorio)) {
                Files.createDirectories(diretorio);
            }

            Path caminhoArquivo = diretorio.resolve(nomeArquivo);
            Files.copy(arquivo.getInputStream(), caminhoArquivo);

            String avatarUrl = "/" + diretorioUpload + nomeArquivo;

            Map<String, String> response = new HashMap<>();
            response.put("avatarUrl", avatarUrl);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/api/toggle-notifications")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> toggleNotifications(HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return ResponseEntity.status(401).build();
        }

        // Implementar lógica de notificações (por enquanto fictício)
        Map<String, Boolean> response = new HashMap<>();
        response.put("enabled", true);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/toggle-public-profile")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> togglePublicProfile(HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return ResponseEntity.status(401).build();
        }

        // Implementar lógica de perfil público (por enquanto fictício)
        Map<String, Boolean> response = new HashMap<>();
        response.put("isPublic", true);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/notificacoes")
    public ModelAndView verNotificacoes(HttpSession session) {
        if (!isVoluntarioLoggedIn(session)) {
            return new ModelAndView("redirect:/login/voluntario");
        }

        Voluntario voluntario = getLoggedVoluntario(session);
        if (voluntario == null) {
            return new ModelAndView("redirect:/login/voluntario");
        }

        ModelAndView mv = new ModelAndView("notificacoesVoluntario");
        mv.addObject("voluntario", voluntario);

        // Implementar busca de notificações quando o sistema estiver pronto
        mv.addObject("notificacoes", Collections.emptyList());

        return mv;
    }

    // Método utilitário para obter extensão do arquivo
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return ".jpg";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex);
        }

        return ".jpg";
    }
}