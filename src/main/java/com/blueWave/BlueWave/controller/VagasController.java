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

    // Lista apenas as vagas da ONG logada
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

    @GetMapping("/homeOng")
    public ModelAndView cadastraVaga(HttpSession session) {
        if (!isOngLoggedIn(session)) {
            return new ModelAndView("redirect:/login/ong");
        }

        ModelAndView mv = new ModelAndView("homeOng");
        Ong ong = getLoggedOng(session);
        if (ong != null) {
            mv.addObject("nomeOng", ong.getNome());
        }
        return mv;
    }

    @PostMapping(value = "/homeOng", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> criarVaga(
            @RequestParam("nome") String nome,
            @RequestParam("quantidade") Integer quantidade,
            @RequestParam("descri") String descri,
            @RequestParam("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam("imagem") MultipartFile imagemFile,
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
                    Vagas salva = vagaRepository.save(vaga);
                    return ResponseEntity.ok(salva);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/{id}/com-imagem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateVagaComImagem(
            @PathVariable Long id,
            @RequestParam("nome") String nome,
            @RequestParam("quantidade") Integer quantidade,
            @RequestParam("descri") String descri,
            @RequestParam("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(value = "imagem", required = false) MultipartFile imagemFile,
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
}