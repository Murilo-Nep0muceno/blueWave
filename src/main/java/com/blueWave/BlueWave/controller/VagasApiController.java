package com.blueWave.BlueWave.controller;

import com.blueWave.BlueWave.model.Vagas;
import com.blueWave.BlueWave.repository.VagasRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class VagasApiController {

    @Autowired
    private VagasRepository vagasRepository;

    // Endpoint para listar vagas públicas (para usuários não logados e voluntários)
    @GetMapping("/vagas/publicas")
    public ResponseEntity<List<VagaPublicaDTO>> listarVagasPublicas() {
        try {
            // Busca todas as vagas ativas (com data futura ou igual a hoje)
            List<Vagas> vagas = vagasRepository.findAll()
                    .stream()
                    .filter(vaga -> vaga.getData() != null &&
                            !vaga.getData().isBefore(LocalDate.now()))
                    .collect(Collectors.toList());

            // Converte para DTO com apenas as informações públicas necessárias
            List<VagaPublicaDTO> vagasPublicas = vagas.stream()
                    .map(vaga -> new VagaPublicaDTO(
                            vaga.getId(),
                            vaga.getNome(),
                            vaga.getDescri(),
                            vaga.getData(),
                            vaga.getQuantidade(),
                            vaga.getImagemPath(),
                            vaga.getOng() != null ? new OngBasicaDTO(
                                    vaga.getOng().getNome(),
                                    vaga.getOng().getCidade(),
                                    vaga.getOng().getEstado()
                            ) : null
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(vagasPublicas);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // DTO para resposta pública das vagas
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

        // Getters
        public Long getId() { return id; }
        public String getNome() { return nome; }
        public String getDescri() { return descri; }
        public LocalDate getData() { return data; }
        public int getQuantidade() { return quantidade; }
        public String getImagemPath() { return imagemPath; }
        public OngBasicaDTO getOng() { return ong; }

        // Setters
        public void setId(Long id) { this.id = id; }
        public void setNome(String nome) { this.nome = nome; }
        public void setDescri(String descri) { this.descri = descri; }
        public void setData(LocalDate data) { this.data = data; }
        public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
        public void setImagemPath(String imagemPath) { this.imagemPath = imagemPath; }
        public void setOng(OngBasicaDTO ong) { this.ong = ong; }
    }

    // DTO para informações básicas da ONG
    public static class OngBasicaDTO {
        private String nome;
        private String cidade;
        private String estado;

        public OngBasicaDTO(String nome, String cidade, String estado) {
            this.nome = nome;
            this.cidade = cidade;
            this.estado = estado;
        }

        // Getters
        public String getNome() { return nome; }
        public String getCidade() { return cidade; }
        public String getEstado() { return estado; }

        // Setters
        public void setNome(String nome) { this.nome = nome; }
        public void setCidade(String cidade) { this.cidade = cidade; }
        public void setEstado(String estado) { this.estado = estado; }
    }
}