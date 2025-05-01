package com.blueWave.BlueWave.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@Setter
@Getter
public class Vagas {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotBlank
    private String nome;

    private LocalDate data;

    private int quantidade;

    @NotBlank
    private String descri;

    private String imagemPath;

    // Novos campos para localização
    private String logradouro;

    private String numero;

    private String complemento;

    private String bairro;

    private String cidade;

    private String estado;

    private String cep;

    // Campo para referência do local (ex: "Sede da ONG", "Parque Municipal", etc)
    private String localReferencia;

    // Enum para status da vaga
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(20) DEFAULT 'ATIVA'")
    private StatusVaga status = StatusVaga.ATIVA;

    @ManyToOne
    @JoinColumn(name = "ong_id")
    private Ong ong;

    @OneToMany(mappedBy = "vaga", cascade = CascadeType.ALL)
    private List<Inscricao> inscricoes = new ArrayList<>();

    // Enum para os status possíveis
    public enum StatusVaga {
        ATIVA("Ativa"),
        CONCLUIDA("Concluída"),
        INTERROMPIDA("Interrompida");

        private final String descricao;

        StatusVaga(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    public Vagas() {
        this.status = StatusVaga.ATIVA; // Define status padrão como ATIVA
    }

    // Getters e Setters
    public String getImagemPath() {
        return imagemPath;
    }

    public void setImagemPath(String imagemPath) {
        this.imagemPath = imagemPath;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public String getDescri() {
        return descri;
    }

    public void setDescri(String descri) {
        this.descri = descri;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public void setComplemento(String complemento) {
        this.complemento = complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getLocalReferencia() {
        return localReferencia;
    }

    public void setLocalReferencia(String localReferencia) {
        this.localReferencia = localReferencia;
    }

    public StatusVaga getStatus() {
        return status;
    }

    public void setStatus(StatusVaga status) {
        this.status = status;
    }

    public Ong getOng() {
        return ong;
    }

    public void setOng(Ong ong) {
        this.ong = ong;
    }

    public List<Inscricao> getInscricoes() {
        return inscricoes;
    }

    public void setInscricoes(List<Inscricao> inscricoes) {
        this.inscricoes = inscricoes;
    }

    // Método auxiliar para obter o endereço completo formatado
    public String getEnderecoCompleto() {
        StringBuilder endereco = new StringBuilder();

        if (logradouro != null && !logradouro.isEmpty()) {
            endereco.append(logradouro);

            if (numero != null && !numero.isEmpty()) {
                endereco.append(", ").append(numero);
            }

            if (complemento != null && !complemento.isEmpty()) {
                endereco.append(" - ").append(complemento);
            }

            if (bairro != null && !bairro.isEmpty()) {
                endereco.append(", ").append(bairro);
            }

            if (cidade != null && !cidade.isEmpty()) {
                endereco.append(", ").append(cidade);
            }

            if (estado != null && !estado.isEmpty()) {
                endereco.append(" - ").append(estado);
            }

            if (cep != null && !cep.isEmpty()) {
                endereco.append(", CEP: ").append(formatarCep(cep));
            }
        }

        return endereco.toString();
    }

    // Método auxiliar para formatar CEP
    private String formatarCep(String cep) {
        if (cep != null && cep.length() == 8) {
            return cep.substring(0, 5) + "-" + cep.substring(5);
        }
        return cep;
    }

    // Método para verificar se a vaga está ativa
    public boolean isAtiva() {
        return this.status == StatusVaga.ATIVA;
    }

    // Método para verificar se a vaga está concluída
    public boolean isConcluida() {
        return this.status == StatusVaga.CONCLUIDA;
    }

    // Método para verificar se a vaga está interrompida
    public boolean isInterrompida() {
        return this.status == StatusVaga.INTERROMPIDA;
    }

    // Método para verificar se a vaga já passou da data
    public boolean isExpirada() {
        return this.data != null && this.data.isBefore(LocalDate.now());
    }
}