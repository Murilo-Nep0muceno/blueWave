package com.blueWave.BlueWave.controller;

import com.blueWave.BlueWave.model.Vagas;
import com.blueWave.BlueWave.repository.VagasRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class VagasApiController {

    @Autowired
    private VagasRepository vagasRepository;

    // Construtor para verificar se o controller está sendo carregado
    public VagasApiController() {
        System.out.println("🚀 VagasApiController inicializado!");
    }

    // Endpoint de teste básico
    @GetMapping(value = "/test", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> test() {
        System.out.println("✅ Endpoint /api/test acessado");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"status\":\"API funcionando!\",\"timestamp\":\"" + LocalDate.now() + "\"}");
    }

    // Endpoint principal para listar vagas públicas
    @GetMapping(value = "/vagas/publicas", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<VagaPublicaDTO>> listarVagasPublicas() {
        System.out.println("📡 === INICIANDO /api/vagas/publicas ===");

        try {
            // Verificação do repository
            if (vagasRepository == null) {
                System.err.println("❌ VagasRepository é null!");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ArrayList<>());
            }

            System.out.println("🔍 Buscando vagas no banco de dados...");

            // Buscar todas as vagas primeiro
            List<Vagas> todasVagas;
            try {
                todasVagas = vagasRepository.findAll();
                System.out.println("📊 Total de vagas no banco: " + todasVagas.size());

                // Log detalhado das vagas encontradas
                for (Vagas vaga : todasVagas) {
                    System.out.println("  - Vaga: " + vaga.getId() +
                            " | Nome: " + vaga.getNome() +
                            " | Status: " + vaga.getStatus() +
                            " | Data: " + vaga.getData() +
                            " | ONG: " + (vaga.getOng() != null ? vaga.getOng().getNome() : "null"));
                }

            } catch (Exception e) {
                System.err.println("❌ Erro ao buscar vagas: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ArrayList<>());
            }

            // Filtrar vagas ativas
            LocalDate hoje = LocalDate.now();
            List<Vagas> vagasAtivas = todasVagas.stream()
                    .filter(vaga -> {
                        boolean statusOk = vaga.getStatus() == Vagas.StatusVaga.ATIVA;
                        boolean dataOk = vaga.getData() != null && !vaga.getData().isBefore(hoje);
                        boolean ongOk = vaga.getOng() != null;

                        boolean isValida = statusOk && dataOk && ongOk;

                        if (!isValida) {
                            System.out.println("🔍 Vaga " + vaga.getId() + " filtrada: " +
                                    "status=" + statusOk + ", data=" + dataOk + ", ong=" + ongOk);
                        }

                        return isValida;
                    })
                    .collect(Collectors.toList());

            System.out.println("✅ Vagas ativas encontradas: " + vagasAtivas.size());

            // Converter para DTO
            List<VagaPublicaDTO> vagasPublicas = vagasAtivas.stream()
                    .map(this::converterParaDTO)
                    .collect(Collectors.toList());

            System.out.println("📤 Retornando " + vagasPublicas.size() + " vagas para o frontend");
            System.out.println("📡 === FINALIZANDO /api/vagas/publicas ===");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(vagasPublicas);

        } catch (Exception e) {
            System.err.println("❌ Erro geral no endpoint: " + e.getMessage());
            e.printStackTrace();

            // Em caso de erro, retornar lista vazia ao invés de erro 500
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ArrayList<>());
        }
    }

    // Método auxiliar para converter Vaga para DTO
    private VagaPublicaDTO converterParaDTO(Vagas vaga) {
        try {
            // Determinar localização (priorizar vaga, depois ONG)
            String cidade = null;
            String estado = null;

            if (vaga.getCidade() != null && !vaga.getCidade().trim().isEmpty()) {
                cidade = vaga.getCidade();
            } else if (vaga.getOng() != null && vaga.getOng().getCidade() != null) {
                cidade = vaga.getOng().getCidade();
            }

            if (vaga.getEstado() != null && !vaga.getEstado().trim().isEmpty()) {
                estado = vaga.getEstado();
            } else if (vaga.getOng() != null && vaga.getOng().getEstado() != null) {
                estado = vaga.getOng().getEstado();
            }

            OngBasicaDTO ongDTO = null;
            if (vaga.getOng() != null) {
                ongDTO = new OngBasicaDTO(
                        vaga.getOng().getNome(),
                        cidade,
                        estado
                );
            }

            return new VagaPublicaDTO(
                    vaga.getId(),
                    vaga.getNome(),
                    vaga.getDescri(),
                    vaga.getData(),
                    vaga.getQuantidade(),
                    vaga.getImagemPath(),
                    ongDTO
            );
        } catch (Exception e) {
            System.err.println("❌ Erro ao converter vaga ID " + vaga.getId() + ": " + e.getMessage());
            throw e;
        }
    }

    // Endpoint de debug
    @GetMapping(value = "/vagas/debug", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> debugVagas() {
        System.out.println("🔧 === DEBUG ENDPOINT ACESSADO ===");

        try {
            if (vagasRepository == null) {
                return ResponseEntity.ok("{\"erro\":\"Repository é null\"}");
            }

            List<Vagas> todasVagas = vagasRepository.findAll();

            StringBuilder debug = new StringBuilder();
            debug.append("{\n");
            debug.append("  \"status\": \"debug_ok\",\n");
            debug.append("  \"totalVagas\": ").append(todasVagas.size()).append(",\n");
            debug.append("  \"dataAtual\": \"").append(LocalDate.now()).append("\",\n");
            debug.append("  \"vagas\": [\n");

            for (int i = 0; i < todasVagas.size(); i++) {
                Vagas vaga = todasVagas.get(i);
                debug.append("    {\n");
                debug.append("      \"id\": ").append(vaga.getId()).append(",\n");
                debug.append("      \"nome\": \"").append(escapeJson(vaga.getNome())).append("\",\n");
                debug.append("      \"data\": \"").append(vaga.getData()).append("\",\n");
                debug.append("      \"status\": \"").append(vaga.getStatus()).append("\",\n");
                debug.append("      \"ong\": \"").append(vaga.getOng() != null ? escapeJson(vaga.getOng().getNome()) : "null").append("\",\n");
                debug.append("      \"ongCidade\": \"").append(vaga.getOng() != null ? escapeJson(vaga.getOng().getCidade()) : "null").append("\",\n");
                debug.append("      \"vagaCidade\": \"").append(vaga.getCidade() != null ? escapeJson(vaga.getCidade()) : "null").append("\"\n");
                debug.append("    }");
                if (i < todasVagas.size() - 1) debug.append(",");
                debug.append("\n");
            }

            debug.append("  ]\n");
            debug.append("}");

            System.out.println("🔧 Debug response preparada");
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(debug.toString());

        } catch (Exception e) {
            System.err.println("❌ Erro no debug: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok("{\"erro\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    // Método utilitário para escapar JSON
    private String escapeJson(String str) {
        if (str == null) return "null";
        return str.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    // DTOs
    public static class VagaPublicaDTO {
        private Long id;
        private String nome;
        private String descri;
        private LocalDate data;
        private int quantidade;
        private String imagemPath;
        private OngBasicaDTO ong;

        public VagaPublicaDTO(Long id, String nome, String descri, LocalDate data,
                              int quantidade, String imagemPath, OngBasicaDTO ong) {
            this.id = id;
            this.nome = nome;
            this.descri = descri;
            this.data = data;
            this.quantidade = quantidade;
            this.imagemPath = imagemPath;
            this.ong = ong;
        }

        // Getters e Setters
        public Long getId() { return id; }
        public String getNome() { return nome; }
        public String getDescri() { return descri; }
        public LocalDate getData() { return data; }
        public int getQuantidade() { return quantidade; }
        public String getImagemPath() { return imagemPath; }
        public OngBasicaDTO getOng() { return ong; }

        public void setId(Long id) { this.id = id; }
        public void setNome(String nome) { this.nome = nome; }
        public void setDescri(String descri) { this.descri = descri; }
        public void setData(LocalDate data) { this.data = data; }
        public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
        public void setImagemPath(String imagemPath) { this.imagemPath = imagemPath; }
        public void setOng(OngBasicaDTO ong) { this.ong = ong; }
    }

    public static class OngBasicaDTO {
        private String nome;
        private String cidade;
        private String estado;

        public OngBasicaDTO(String nome, String cidade, String estado) {
            this.nome = nome;
            this.cidade = cidade;
            this.estado = estado;
        }

        public String getNome() { return nome; }
        public String getCidade() { return cidade; }
        public String getEstado() { return estado; }

        public void setNome(String nome) { this.nome = nome; }
        public void setCidade(String cidade) { this.cidade = cidade; }
        public void setEstado(String estado) { this.estado = estado; }
    }
}