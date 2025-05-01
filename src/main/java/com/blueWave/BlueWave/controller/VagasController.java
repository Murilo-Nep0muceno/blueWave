package com.blueWave.BlueWave.controller;

import com.blueWave.BlueWave.model.Ong;
import com.blueWave.BlueWave.model.Vagas;
import com.blueWave.BlueWave.repository.OngRepository;
import com.blueWave.BlueWave.repository.VagasRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vagas")
public class VagasController {

    @Autowired
    private OngRepository ongRepository;

    @Autowired
    private VagasRepository vagaRepository;

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + File.separator + "uploads";

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

    // Método utilitário para verificar se uma string é válida
    private boolean isValidString(String str) {
        return str != null && !str.trim().isEmpty() && !str.trim().equalsIgnoreCase("null");
    }

    // ====== ROTAS ESPECÍFICAS (DEVEM VIR ANTES DAS ROTAS COM PARÂMETROS) ======

    // Página inicial para redirecionamento
    @GetMapping({"", "/"})
    public String home(HttpSession session) {
        if (!isOngLoggedIn(session)) {
            return "redirect:/login/ong";
        }
        return "redirect:/vagas/homeOng";
    }

    // Página home para cadastrar vagas
    @GetMapping("/homeOng")
    public ModelAndView cadastraVaga(HttpSession session) {
        if (!isOngLoggedIn(session)) {
            return new ModelAndView("redirect:/login/ong");
        }

        ModelAndView mv = new ModelAndView("homeOng");
        Ong ong = getLoggedOng(session);
        if (ong != null) {
            mv.addObject("nomeOng", ong.getNome());
            mv.addObject("ong", ong);
        }
        return mv;
    }

    // Listar vagas da ONG
    @GetMapping("/listaVaga")
    public ModelAndView listarVagasOng(HttpSession session) {
        if (!isOngLoggedIn(session)) {
            return new ModelAndView("redirect:/login/ong");
        }

        ModelAndView mv = new ModelAndView("listaVaga");
        try {
            Ong ong = getLoggedOng(session);
            if (ong != null) {
                List<Vagas> vagas = vagaRepository.findByOng(ong);
                mv.addObject("vagas", vagas);
                mv.addObject("nomeOng", ong.getNome());
            }
        } catch (Exception e) {
            e.printStackTrace();
            mv.addObject("vagas", List.of());
            mv.addObject("erro", "Erro ao carregar vagas: " + e.getMessage());
        }
        return mv;
    }

    // Página para finalizar cadastro
    @GetMapping("/finalizarCadastro")
    public ModelAndView finalizarCadastro(HttpSession session) {
        System.out.println("Acessando GET /vagas/finalizarCadastro");

        if (!isOngLoggedIn(session)) {
            System.out.println("Usuário não logado, redirecionando para login");
            return new ModelAndView("redirect:/login/ong");
        }

        try {
            ModelAndView mv = new ModelAndView("finalizarCadastro");
            Ong ong = getLoggedOng(session);

            if (ong != null) {
                System.out.println("ONG encontrada: " + ong.getNome());
                mv.addObject("nomeOng", ong.getNome());
                mv.addObject("ong", ong);
            } else {
                System.out.println("ONG não encontrada");
                return new ModelAndView("redirect:/login/ong");
            }

            return mv;
        } catch (Exception e) {
            System.err.println("Erro ao carregar página de finalizar cadastro: " + e.getMessage());
            e.printStackTrace();
            return new ModelAndView("redirect:/login/ong");
        }
    }

    // ====== NOVA ROTA PARA PERFIL DA ONG ======
    @GetMapping("/perfil")
    public ModelAndView perfilOng(HttpSession session) {
        System.out.println("Acessando GET /vagas/perfil");

        if (!isOngLoggedIn(session)) {
            System.out.println("Usuário não logado, redirecionando para login");
            return new ModelAndView("redirect:/login/ong");
        }

        try {
            ModelAndView mv = new ModelAndView("perfilOng"); // template perfilOng.html
            Ong ong = getLoggedOng(session);

            if (ong != null) {
                System.out.println("ONG encontrada para perfil: " + ong.getNome());

                // Calcular estatísticas das vagas
                List<Vagas> todasVagas = vagaRepository.findByOng(ong);
                long totalVagas = todasVagas.size();
                long vagasAtivas = todasVagas.stream()
                        .filter(v -> v.getStatus() == Vagas.StatusVaga.ATIVA)
                        .count();

                // Adicionar objetos ao model
                mv.addObject("ong", ong);
                mv.addObject("totalVagas", totalVagas);
                mv.addObject("vagasAtivas", vagasAtivas);
                mv.addObject("totalVoluntarios", 0); // Por enquanto fixo em 0

            } else {
                System.out.println("ONG não encontrada para perfil");
                return new ModelAndView("redirect:/login/ong");
            }

            return mv;
        } catch (Exception e) {
            System.err.println("Erro ao carregar página de perfil: " + e.getMessage());
            e.printStackTrace();
            return new ModelAndView("redirect:/vagas/homeOng");
        }
    }

    // Página de configurações removida - existe ConfiguracoesController dedicado

    // Endpoint para obter estatísticas das vagas da ONG
    @GetMapping("/estatisticas")
    public ResponseEntity<?> obterEstatisticas(HttpSession session) {
        if (!isOngLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Acesso não autorizado");
        }

        Ong ong = getLoggedOng(session);
        if (ong == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("ONG não encontrada");
        }

        try {
            List<Vagas> todasVagas = vagaRepository.findByOng(ong);

            long vagasAtivas = todasVagas.stream()
                    .filter(v -> v.getStatus() == Vagas.StatusVaga.ATIVA)
                    .count();

            long vagasConcluidas = todasVagas.stream()
                    .filter(v -> v.getStatus() == Vagas.StatusVaga.CONCLUIDA)
                    .count();

            long vagasInterrompidas = todasVagas.stream()
                    .filter(v -> v.getStatus() == Vagas.StatusVaga.INTERROMPIDA)
                    .count();

            // Criar objeto com estatísticas
            var estatisticas = new java.util.HashMap<String, Object>();
            estatisticas.put("totalVagas", todasVagas.size());
            estatisticas.put("vagasAtivas", vagasAtivas);
            estatisticas.put("vagasConcluidas", vagasConcluidas);
            estatisticas.put("vagasInterrompidas", vagasInterrompidas);

            return ResponseEntity.ok(estatisticas);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao obter estatísticas: " + e.getMessage());
        }
    }

    // Endpoint para buscar vagas por status
    @GetMapping("/porStatus")
    public ResponseEntity<?> listarVagasPorStatus(
            @RequestParam(value = "status", required = false) String status,
            HttpSession session) {

        if (!isOngLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Acesso não autorizado");
        }

        Ong ong = getLoggedOng(session);
        if (ong == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("ONG não encontrada");
        }

        List<Vagas> vagas;

        if (status == null || status.isEmpty()) {
            // Retorna todas as vagas da ONG
            vagas = vagaRepository.findByOng(ong);
        } else {
            try {
                Vagas.StatusVaga statusEnum = Vagas.StatusVaga.valueOf(status.toUpperCase());

                // Filtra manualmente (você pode criar um método no repositório se preferir)
                vagas = vagaRepository.findByOng(ong).stream()
                        .filter(v -> v.getStatus() == statusEnum)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Status inválido. Use: ATIVA, CONCLUIDA ou INTERROMPIDA");
            }
        }

        return ResponseEntity.ok(vagas);
    }

    // Endpoint para servir imagens de upload
    @GetMapping("/uploads/{filename}")
    public ResponseEntity<org.springframework.core.io.Resource> servirImagem(@PathVariable String filename) {
        try {
            File file = new File(UPLOAD_DIR, filename);
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(file);

            // Determinar o tipo de conteúdo
            String contentType = "image/jpeg"; // padrão
            String fileExtension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

            switch (fileExtension) {
                case "png":
                    contentType = "image/png";
                    break;
                case "gif":
                    contentType = "image/gif";
                    break;
                case "webp":
                    contentType = "image/webp";
                    break;
                default:
                    contentType = "image/jpeg";
            }

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    // ====== ROTAS COM PARÂMETROS (DEVEM VIR POR ÚLTIMO) ======

    // Página para editar vaga
    @GetMapping("/editar/{id}")
    public ModelAndView editarVaga(@PathVariable Long id, HttpSession session) {
        if (!isOngLoggedIn(session)) {
            return new ModelAndView("redirect:/login/ong");
        }

        Ong ong = getLoggedOng(session);
        if (ong == null) {
            return new ModelAndView("redirect:/login/ong");
        }

        ModelAndView mv = new ModelAndView("editarVaga");
        Optional<Vagas> vagaOpt = vagaRepository.findById(id);

        if (vagaOpt.isPresent() && vagaOpt.get().getOng().getId().equals(ong.getId())) {
            mv.addObject("vaga", vagaOpt.get());
        } else {
            mv = new ModelAndView("redirect:/vagas/listaVaga");
            mv.addObject("erro", "Vaga não encontrada ou não autorizada");
        }
        return mv;
    }

    // Método para obter vaga por ID (API) - AGORA NO FINAL
    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Vagas> obterVagaPorId(@PathVariable Long id, HttpSession session) {
        if (!isOngLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Ong ong = getLoggedOng(session);
        if (ong == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return vagaRepository.findById(id)
                .filter(vaga -> vaga.getOng().getId().equals(ong.getId()))
                .map(vaga -> ResponseEntity.ok(vaga))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ====== MÉTODOS POST, PUT, PATCH, DELETE ======

    // Criar vaga com imagem
    @PostMapping(value = "/homeOng", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> criarVaga(
            @RequestParam("nome") String nome,
            @RequestParam("quantidade") Integer quantidade,
            @RequestParam("descri") String descri,
            @RequestParam("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam("imagem") MultipartFile imagemFile,
            @RequestParam(value = "localReferencia", required = false) String localReferencia,
            @RequestParam(value = "cep", required = false) String cep,
            @RequestParam(value = "logradouro", required = false) String logradouro,
            @RequestParam(value = "numero", required = false) String numero,
            @RequestParam(value = "complemento", required = false) String complemento,
            @RequestParam(value = "bairro", required = false) String bairro,
            @RequestParam(value = "cidade", required = false) String cidade,
            @RequestParam(value = "estado", required = false) String estado,
            HttpSession session) {

        if (!isOngLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Acesso não autorizado");
        }

        try {
            // Validações básicas
            if (nome == null || nome.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Nome da vaga é obrigatório.");
            }

            if (quantidade == null || quantidade <= 0) {
                return ResponseEntity.badRequest().body("Quantidade deve ser maior que zero.");
            }

            if (data == null) {
                return ResponseEntity.badRequest().body("Data da vaga é obrigatória.");
            }

            if (imagemFile == null || imagemFile.isEmpty()) {
                return ResponseEntity.badRequest().body("Imagem é obrigatória.");
            }

            // Validação do tipo de arquivo
            String contentType = imagemFile.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body("Arquivo deve ser uma imagem válida.");
            }

            // Validação do tamanho do arquivo (max 5MB)
            if (imagemFile.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body("Imagem deve ter no máximo 5MB.");
            }

            // Cria diretório se não existir
            File uploadsDir = new File(UPLOAD_DIR);
            if (!uploadsDir.exists()) {
                boolean created = uploadsDir.mkdirs();
                if (!created) {
                    return ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Erro ao criar diretório de upload.");
                }
            }

            // Gera nome único para o arquivo
            String originalFilename = imagemFile.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String filename = System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + extension;
            File destinationFile = new File(uploadsDir, filename);

            // Salva o arquivo
            imagemFile.transferTo(destinationFile);

            // Obtém a ONG logada
            Ong ong = getLoggedOng(session);
            if (ong == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("ONG não encontrada");
            }

            // Cria e salva a vaga
            Vagas vaga = new Vagas();
            vaga.setNome(nome.trim());
            vaga.setQuantidade(quantidade);
            vaga.setDescri(descri != null ? descri.trim() : "");
            vaga.setData(data);
            vaga.setImagemPath("/uploads/" + filename);
            vaga.setOng(ong);

            // Define os campos de localização
            if (isValidString(localReferencia)) {
                vaga.setLocalReferencia(localReferencia.trim());
            }

            if (isValidString(cep)) {
                String cleanCep = cep.replaceAll("[^0-9]", "");
                if (cleanCep.length() == 8) {
                    vaga.setCep(cleanCep);
                }
            }

            if (isValidString(logradouro)) {
                vaga.setLogradouro(logradouro.trim());
            }

            if (isValidString(numero)) {
                vaga.setNumero(numero.trim());
            }

            if (isValidString(complemento)) {
                vaga.setComplemento(complemento.trim());
            }

            if (isValidString(bairro)) {
                vaga.setBairro(bairro.trim());
            }

            if (isValidString(cidade)) {
                vaga.setCidade(cidade.trim());
            }

            if (isValidString(estado)) {
                vaga.setEstado(estado.trim());
            }

            // Status padrão é ATIVA (definido no construtor do modelo)

            vagaRepository.save(vaga);

            return ResponseEntity.ok("Vaga criada com sucesso!");

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao salvar imagem: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno do servidor: " + e.getMessage());
        }
    }

    // Método alternativo para criar vaga via JSON (sem imagem)
    @PostMapping(value = "/criar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> criarVagaJson(@RequestBody Vagas vaga, HttpSession session) {
        if (!isOngLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Acesso não autorizado");
        }

        try {
            Ong ong = getLoggedOng(session);
            if (ong == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("ONG não encontrada");
            }

            vaga.setOng(ong);
            vagaRepository.save(vaga);
            return ResponseEntity.ok("Vaga criada com sucesso!");
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao criar vaga: " + e.getMessage());
        }
    }

    // Processar finalização/edição do cadastro (SEM arquivo de logo)
    @PostMapping("/finalizarCadastro")
    public ResponseEntity<String> processarFinalizarCadastro(
            @RequestParam(value = "nome", required = false) String nome,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "descricao", required = false) String descricao,
            @RequestParam(value = "logradouro", required = false) String logradouro,
            @RequestParam(value = "numero", required = false) String numero,
            @RequestParam(value = "complemento", required = false) String complemento,
            @RequestParam(value = "bairro", required = false) String bairro,
            @RequestParam(value = "cidade", required = false) String cidade,
            @RequestParam(value = "estado", required = false) String estado,
            @RequestParam(value = "cep", required = false) String cep,
            @RequestParam(value = "telefone", required = false) String telefone,
            @RequestParam(value = "site", required = false) String site,
            @RequestParam(value = "instagram", required = false) String instagram,
            @RequestParam(value = "facebook", required = false) String facebook,
            @RequestParam(value = "dataFundacao", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFundacao,
            @RequestParam(value = "logoUrl", required = false) String logoUrl,
            HttpSession session) {

        System.out.println("=== PROCESSANDO EDIÇÃO DE PERFIL ===");

        if (!isOngLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Acesso não autorizado");
        }

        try {
            Ong ong = getLoggedOng(session);
            if (ong == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("ONG não encontrada");
            }

            // Atualizar NOME da ONG
            if (isValidString(nome)) {
                ong.setNome(nome.trim());
            }

            // Atualizar EMAIL da ONG
            if (isValidString(email)) {
                // Verificar se o novo email já existe no banco
                String trimmedEmail = email.trim().toLowerCase();
                Ong existingOng = ongRepository.findByEmail(trimmedEmail);

                if (existingOng != null && !existingOng.getId().equals(ong.getId())) {
                    return ResponseEntity.badRequest().body("Este email já está cadastrado para outra ONG.");
                }

                ong.setEmail(trimmedEmail);

                // Atualizar a sessão com o novo email
                session.setAttribute("userEmail", trimmedEmail);
            }

            // Atualizar descrição
            if (isValidString(descricao)) {
                ong.setDescricao(descricao.trim());
            }

            // Atualizar endereço
            if (isValidString(logradouro)) {
                ong.setLogradouro(logradouro.trim());
            }

            if (isValidString(numero)) {
                ong.setNumero(numero.trim());
            }

            if (isValidString(complemento)) {
                ong.setComplemento(complemento.trim());
            }

            if (isValidString(bairro)) {
                ong.setBairro(bairro.trim());
            }

            if (isValidString(cidade)) {
                ong.setCidade(cidade.trim());
            }

            if (isValidString(estado)) {
                ong.setEstado(estado.trim());
            }

            if (isValidString(cep)) {
                // Remove formatação do CEP antes de salvar
                String cleanCep = cep.replaceAll("[^0-9]", "");
                if (cleanCep.length() == 8) {
                    ong.setCep(cleanCep);
                }
            }

            if (isValidString(telefone)) {
                // Remove formatação do telefone antes de salvar
                String cleanTelefone = telefone.replaceAll("[^0-9]", "");
                if (cleanTelefone.length() >= 10 && cleanTelefone.length() <= 11) {
                    ong.setTelefone(cleanTelefone);
                }
            }

            if (isValidString(site)) {
                String cleanSite = site.trim();
                // Adiciona http:// se não tiver protocolo
                if (!cleanSite.startsWith("http://") && !cleanSite.startsWith("https://")) {
                    cleanSite = "https://" + cleanSite;
                }
                ong.setSite(cleanSite);
            }

            if (isValidString(instagram)) {
                // Remove @ se estiver presente
                String cleanInstagram = instagram.trim().replaceAll("^@", "");
                ong.setInstagram(cleanInstagram);
            }

            if (isValidString(facebook)) {
                ong.setFacebook(facebook.trim());
            }

            if (dataFundacao != null) {
                ong.setDataFundacao(dataFundacao);
            }

            if (isValidString(logoUrl)) {
                ong.setLogoUrl(logoUrl.trim());
            }

            // Marcar cadastro como finalizado
            ong.setCadastroFinalizado(true);

            // Salvar as alterações
            ongRepository.save(ong);

            System.out.println("Perfil da ONG atualizado com sucesso: " + ong.getNome());
            return ResponseEntity.ok("Perfil atualizado com sucesso!");

        } catch (Exception e) {
            System.err.println("Erro ao processar finalização do cadastro: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno do servidor: " + e.getMessage());
        }
    }

    // Processar finalização/edição do cadastro COM arquivo de logo
    @PostMapping(value = "/finalizarCadastro/comLogo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> processarFinalizarCadastroComLogo(
            @RequestParam(value = "nome", required = false) String nome,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "descricao", required = false) String descricao,
            @RequestParam(value = "logradouro", required = false) String logradouro,
            @RequestParam(value = "numero", required = false) String numero,
            @RequestParam(value = "complemento", required = false) String complemento,
            @RequestParam(value = "bairro", required = false) String bairro,
            @RequestParam(value = "cidade", required = false) String cidade,
            @RequestParam(value = "estado", required = false) String estado,
            @RequestParam(value = "cep", required = false) String cep,
            @RequestParam(value = "telefone", required = false) String telefone,
            @RequestParam(value = "site", required = false) String site,
            @RequestParam(value = "instagram", required = false) String instagram,
            @RequestParam(value = "facebook", required = false) String facebook,
            @RequestParam(value = "dataFundacao", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFundacao,
            @RequestParam(value = "logo", required = false) MultipartFile logoFile,
            HttpSession session) {

        System.out.println("=== PROCESSANDO EDIÇÃO DE PERFIL COM LOGO ===");

        if (!isOngLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Acesso não autorizado");
        }

        try {
            Ong ong = getLoggedOng(session);
            if (ong == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("ONG não encontrada");
            }

            // Processar upload do logo se fornecido
            if (logoFile != null && !logoFile.isEmpty()) {
                // Validação do tipo de arquivo
                String contentType = logoFile.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return ResponseEntity.badRequest().body("Logo deve ser uma imagem válida.");
                }

                // Validação do tamanho do arquivo (max 2MB)
                if (logoFile.getSize() > 2 * 1024 * 1024) {
                    return ResponseEntity.badRequest().body("Logo deve ter no máximo 2MB.");
                }

                // Remove logo antigo se existir
                if (ong.getLogoUrl() != null) {
                    try {
                        File oldLogoFile = new File(UPLOAD_DIR, ong.getLogoUrl().replace("/uploads/", ""));
                        if (oldLogoFile.exists()) {
                            oldLogoFile.delete();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // Salva novo logo
                File uploadsDir = new File(UPLOAD_DIR);
                if (!uploadsDir.exists()) {
                    uploadsDir.mkdirs();
                }

                String originalFilename = logoFile.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }

                String filename = "logo_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + extension;
                File destinationFile = new File(uploadsDir, filename);
                logoFile.transferTo(destinationFile);

                ong.setLogoUrl("/uploads/" + filename);
            }

            // Atualizar todos os outros campos (mesmo código do método sem logo)
            if (isValidString(nome)) {
                ong.setNome(nome.trim());
            }

            if (isValidString(email)) {
                String trimmedEmail = email.trim().toLowerCase();
                Ong existingOng = ongRepository.findByEmail(trimmedEmail);

                if (existingOng != null && !existingOng.getId().equals(ong.getId())) {
                    return ResponseEntity.badRequest().body("Este email já está cadastrado para outra ONG.");
                }

                ong.setEmail(trimmedEmail);
                session.setAttribute("userEmail", trimmedEmail);
            }

            if (isValidString(descricao)) {
                ong.setDescricao(descricao.trim());
            }

            if (isValidString(logradouro)) {
                ong.setLogradouro(logradouro.trim());
            }

            if (isValidString(numero)) {
                ong.setNumero(numero.trim());
            }

            if (isValidString(complemento)) {
                ong.setComplemento(complemento.trim());
            }

            if (isValidString(bairro)) {
                ong.setBairro(bairro.trim());
            }

            if (isValidString(cidade)) {
                ong.setCidade(cidade.trim());
            }

            if (isValidString(estado)) {
                ong.setEstado(estado.trim());
            }

            if (isValidString(cep)) {
                String cleanCep = cep.replaceAll("[^0-9]", "");
                if (cleanCep.length() == 8) {
                    ong.setCep(cleanCep);
                }
            }

            if (isValidString(telefone)) {
                String cleanTelefone = telefone.replaceAll("[^0-9]", "");
                if (cleanTelefone.length() >= 10 && cleanTelefone.length() <= 11) {
                    ong.setTelefone(cleanTelefone);
                }
            }

            if (isValidString(site)) {
                String cleanSite = site.trim();
                if (!cleanSite.startsWith("http://") && !cleanSite.startsWith("https://")) {
                    cleanSite = "https://" + cleanSite;
                }
                ong.setSite(cleanSite);
            }

            if (isValidString(instagram)) {
                String cleanInstagram = instagram.trim().replaceAll("^@", "");
                ong.setInstagram(cleanInstagram);
            }

            if (isValidString(facebook)) {
                ong.setFacebook(facebook.trim());
            }

            if (dataFundacao != null) {
                ong.setDataFundacao(dataFundacao);
            }

            ong.setCadastroFinalizado(true);
            ongRepository.save(ong);

            System.out.println("Perfil da ONG atualizado com sucesso (com logo): " + ong.getNome());
            return ResponseEntity.ok("Perfil atualizado com sucesso!");

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao salvar logo: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro ao processar finalização do cadastro com logo: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno do servidor: " + e.getMessage());
        }
    }

    // Atualizar vaga via JSON
    @PutMapping("/{id}")
    public ResponseEntity<Vagas> updateVaga(
            @PathVariable Long id,
            @RequestBody Vagas novaVaga,
            HttpSession session) {

        if (!isOngLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Ong ong = getLoggedOng(session);
        if (ong == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return vagaRepository.findById(id)
                .filter(vaga -> vaga.getOng().getId().equals(ong.getId())) // Verifica se a vaga pertence à ONG
                .map(vaga -> {
                    vaga.setNome(novaVaga.getNome());
                    vaga.setDescri(novaVaga.getDescri());
                    vaga.setQuantidade(novaVaga.getQuantidade());
                    if (novaVaga.getData() != null) {
                        vaga.setData(novaVaga.getData());
                    }

                    // Atualiza campos de localização
                    vaga.setLocalReferencia(novaVaga.getLocalReferencia());
                    vaga.setCep(novaVaga.getCep());
                    vaga.setLogradouro(novaVaga.getLogradouro());
                    vaga.setNumero(novaVaga.getNumero());
                    vaga.setComplemento(novaVaga.getComplemento());
                    vaga.setBairro(novaVaga.getBairro());
                    vaga.setCidade(novaVaga.getCidade());
                    vaga.setEstado(novaVaga.getEstado());

                    // Atualiza status se fornecido
                    if (novaVaga.getStatus() != null) {
                        vaga.setStatus(novaVaga.getStatus());
                    }

                    Vagas salva = vagaRepository.save(vaga);
                    return ResponseEntity.ok(salva);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Atualizar vaga com imagem
    @PutMapping(value = "/{id}/com-imagem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateVagaComImagem(
            @PathVariable Long id,
            @RequestParam("nome") String nome,
            @RequestParam("quantidade") Integer quantidade,
            @RequestParam("descri") String descri,
            @RequestParam("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(value = "imagem", required = false) MultipartFile imagemFile,
            @RequestParam(value = "localReferencia", required = false) String localReferencia,
            @RequestParam(value = "cep", required = false) String cep,
            @RequestParam(value = "logradouro", required = false) String logradouro,
            @RequestParam(value = "numero", required = false) String numero,
            @RequestParam(value = "complemento", required = false) String complemento,
            @RequestParam(value = "bairro", required = false) String bairro,
            @RequestParam(value = "cidade", required = false) String cidade,
            @RequestParam(value = "estado", required = false) String estado,
            @RequestParam(value = "status", required = false) String status,
            HttpSession session) {

        if (!isOngLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Acesso não autorizado");
        }

        Ong ong = getLoggedOng(session);
        if (ong == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("ONG não encontrada");
        }

        return vagaRepository.findById(id)
                .filter(vaga -> vaga.getOng().getId().equals(ong.getId())) // Verifica se a vaga pertence à ONG
                .map(vaga -> {
                    try {
                        vaga.setNome(nome);
                        vaga.setDescri(descri);
                        vaga.setQuantidade(quantidade);
                        vaga.setData(data);

                        // Se uma nova imagem foi enviada
                        if (imagemFile != null && !imagemFile.isEmpty()) {
                            // Remove a imagem antiga se existir
                            if (vaga.getImagemPath() != null) {
                                try {
                                    File oldImageFile = new File(UPLOAD_DIR,
                                            vaga.getImagemPath().replace("/uploads/", ""));
                                    if (oldImageFile.exists()) {
                                        oldImageFile.delete();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            // Salva a nova imagem
                            File uploadsDir = new File(UPLOAD_DIR);
                            if (!uploadsDir.exists()) {
                                uploadsDir.mkdirs();
                            }

                            String originalFilename = imagemFile.getOriginalFilename();
                            String extension = "";
                            if (originalFilename != null && originalFilename.contains(".")) {
                                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                            }

                            String filename = System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + extension;
                            File destinationFile = new File(uploadsDir, filename);
                            imagemFile.transferTo(destinationFile);

                            vaga.setImagemPath("/uploads/" + filename);
                        }

                        // Atualiza campos de localização
                        if (isValidString(localReferencia)) {
                            vaga.setLocalReferencia(localReferencia.trim());
                        } else {
                            vaga.setLocalReferencia(null);
                        }

                        if (isValidString(cep)) {
                            String cleanCep = cep.replaceAll("[^0-9]", "");
                            if (cleanCep.length() == 8) {
                                vaga.setCep(cleanCep);
                            }
                        } else {
                            vaga.setCep(null);
                        }

                        if (isValidString(logradouro)) {
                            vaga.setLogradouro(logradouro.trim());
                        } else {
                            vaga.setLogradouro(null);
                        }

                        if (isValidString(numero)) {
                            vaga.setNumero(numero.trim());
                        } else {
                            vaga.setNumero(null);
                        }

                        if (isValidString(complemento)) {
                            vaga.setComplemento(complemento.trim());
                        } else {
                            vaga.setComplemento(null);
                        }

                        if (isValidString(bairro)) {
                            vaga.setBairro(bairro.trim());
                        } else {
                            vaga.setBairro(null);
                        }

                        if (isValidString(cidade)) {
                            vaga.setCidade(cidade.trim());
                        } else {
                            vaga.setCidade(null);
                        }

                        if (isValidString(estado)) {
                            vaga.setEstado(estado.trim());
                        } else {
                            vaga.setEstado(null);
                        }

                        // Atualiza status se fornecido
                        if (isValidString(status)) {
                            try {
                                Vagas.StatusVaga statusEnum = Vagas.StatusVaga.valueOf(status.toUpperCase());
                                vaga.setStatus(statusEnum);
                            } catch (IllegalArgumentException e) {
                                // Status inválido, mantém o status atual
                            }
                        }

                        vagaRepository.save(vaga);
                        return ResponseEntity.ok("Vaga atualizada com sucesso!");
                    } catch (IOException e) {
                        e.printStackTrace();
                        return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Erro ao salvar imagem: " + e.getMessage());
                    }
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Endpoint para alterar o status de uma vaga
    @PatchMapping("/{id}/status")
    public ResponseEntity<String> alterarStatusVaga(
            @PathVariable Long id,
            @RequestParam("status") String status,
            HttpSession session) {

        if (!isOngLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Acesso não autorizado");
        }

        Ong ong = getLoggedOng(session);
        if (ong == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("ONG não encontrada");
        }

        return vagaRepository.findById(id)
                .filter(vaga -> vaga.getOng().getId().equals(ong.getId()))
                .map(vaga -> {
                    try {
                        Vagas.StatusVaga statusEnum = Vagas.StatusVaga.valueOf(status.toUpperCase());
                        vaga.setStatus(statusEnum);
                        vagaRepository.save(vaga);
                        return ResponseEntity.ok("Status da vaga atualizado para: " + statusEnum.getDescricao());
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body("Status inválido. Use: ATIVA, CONCLUIDA ou INTERROMPIDA");
                    }
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Deletar vaga
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVaga(@PathVariable Long id, HttpSession session) {
        if (!isOngLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Ong ong = getLoggedOng(session);
        if (ong == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return vagaRepository.findById(id)
                .filter(vaga -> vaga.getOng().getId().equals(ong.getId())) // Verifica se a vaga pertence à ONG
                .map(vaga -> {
                    // Remove a imagem associada se existir
                    if (vaga.getImagemPath() != null) {
                        try {
                            File imageFile = new File(UPLOAD_DIR,
                                    vaga.getImagemPath().replace("/uploads/", ""));
                            if (imageFile.exists()) {
                                imageFile.delete();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    vagaRepository.deleteById(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Método para logout (limpar sessão)
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        try {
            session.invalidate();
            return ResponseEntity.ok("Logout realizado com sucesso");
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao realizar logout");
        }
    }

    @GetMapping("/detalhes/{id}")
    @ResponseBody
    public ResponseEntity<?> obterDetalhesVaga(@PathVariable Long id, HttpSession session) {
        if (!isOngLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Acesso não autorizado");
        }

        Ong ong = getLoggedOng(session);
        if (ong == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("ONG não encontrada");
        }

        return vagaRepository.findById(id)
                .filter(vaga -> vaga.getOng().getId().equals(ong.getId()))
                .map(vaga -> {
                    // Usar HashMap simples para evitar problemas de serialização
                    var detalhes = new java.util.HashMap<String, Object>();
                    detalhes.put("id", vaga.getId());
                    detalhes.put("nome", vaga.getNome());
                    detalhes.put("descri", vaga.getDescri());
                    detalhes.put("quantidade", vaga.getQuantidade());
                    detalhes.put("data", vaga.getData());
                    detalhes.put("status", vaga.getStatus().name()); // Usar .name() para garantir String

                    // Localização
                    detalhes.put("localReferencia", vaga.getLocalReferencia());
                    detalhes.put("logradouro", vaga.getLogradouro());
                    detalhes.put("numero", vaga.getNumero());
                    detalhes.put("complemento", vaga.getComplemento());
                    detalhes.put("bairro", vaga.getBairro());
                    detalhes.put("cidade", vaga.getCidade());
                    detalhes.put("estado", vaga.getEstado());
                    detalhes.put("cep", vaga.getCep());

                    return ResponseEntity.ok(detalhes);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}