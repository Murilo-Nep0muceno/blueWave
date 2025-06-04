package com.blueWave.BlueWave.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@Setter
@Getter
public class Ong {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotBlank(message = "Nome da ONG é obrigatório")
    @Size(min = 3, message = "Nome deve ter pelo menos 3 caracteres")
    private String nome;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter um formato válido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter pelo menos 8 caracteres")
    private String senha;

    @NotBlank(message = "CNPJ é obrigatório")
    @Pattern(regexp = "\\d{14}", message = "CNPJ deve conter exatamente 14 dígitos numéricos")
    private String cnpj;

    @NotBlank(message = "Telefone é obrigatório")
    @Pattern(regexp = "\\d{10,11}", message = "Telefone deve conter entre 10 e 11 dígitos numéricos")
    private String telefone;

    @NotBlank(message = "CEP é obrigatório")
    @Pattern(regexp = "\\d{8}", message = "CEP deve conter exatamente 8 dígitos numéricos")
    private String cep;

    // Campos para finalizar cadastro - AGORA SEM RESTRIÇÕES OBRIGATÓRIAS
    @Column(columnDefinition = "TEXT")
    private String descricao;

    private String logradouro;

    private String numero;

    private String complemento;

    private String bairro; // CAMPO BAIRRO ADICIONADO

    private String cidade;

    private String estado;

    // Redes sociais - OPCIONAIS, sem validações restritivas
    private String site;

    private String instagram;

    private String facebook;

    private LocalDate dataFundacao;

    private String logoUrl;

    // Campo para controlar se o cadastro foi finalizado
    @Column(columnDefinition = "boolean default false")
    private Boolean cadastroFinalizado = false;

    // Campo para data de criação do cadastro (opcional)
    @Column(name = "data_cadastro")
    private LocalDate dataCadastro;

    @OneToMany(mappedBy = "ong", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Vagas> vagas = new ArrayList<>();

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @Column(name = "banido", nullable = false)
    private Boolean banido = false;

    @Column(name = "data_banimento")
    private LocalDateTime dataBanimento;

    @Column(name = "motivo_banimento", length = 500)
    private String motivoBanimento;

    @Column(name = "admin_responsavel_banimento", length = 100)
    private String adminResponsavelBanimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_banimento")
    private TipoBanimento tipoBanimento;

    @Column(name = "data_fim_banimento")
    private LocalDateTime dataFimBanimento; // Para banimentos temporários



    public enum TipoBanimento {
        TEMPORARIO("Temporário"),
        PERMANENTE("Permanente");

        private final String descricao;

        TipoBanimento(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    // Métodos utilitários para banimento
    public boolean isAtivo() {
        return ativo && !isBanido();
    }

    public boolean isBanido() {
        if (!banido) return false;

        // Se for banimento temporário, verificar se ainda está em vigor
        if (tipoBanimento == TipoBanimento.TEMPORARIO && dataFimBanimento != null) {
            return LocalDateTime.now().isBefore(dataFimBanimento);
        }

        // Banimento permanente
        return true;
    }

    public boolean podeLogar() {
        return isAtivo();
    }


    // Construtor padrão
    public Ong() {
        this.dataCadastro = LocalDate.now();
        this.cadastroFinalizado = false;
    }

    // Construtor personalizado para facilitar a criação
    public Ong(String nome, String email, String senha, String cnpj, String telefone, String cep) {
        this();
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.cnpj = cnpj;
        this.telefone = telefone;
        this.cep = cep;
    }

    // Métodos utilitários

    /**
     * Verifica se o cadastro básico está completo
     */
    public boolean isCadastroBasicoCompleto() {
        return nome != null && !nome.trim().isEmpty() &&
                email != null && !email.trim().isEmpty() &&
                cnpj != null && !cnpj.trim().isEmpty() &&
                telefone != null && !telefone.trim().isEmpty() &&
                cep != null && !cep.trim().isEmpty();
    }

    /**
     * Verifica se tem informações de endereço completas
     */
    public boolean isEnderecoCompleto() {
        return logradouro != null && !logradouro.trim().isEmpty() &&
                numero != null && !numero.trim().isEmpty() &&
                cidade != null && !cidade.trim().isEmpty() &&
                estado != null && !estado.trim().isEmpty();
    }

    /**
     * Verifica se tem pelo menos uma rede social cadastrada
     */
    public boolean temRedesSociais() {
        return (instagram != null && !instagram.trim().isEmpty()) ||
                (facebook != null && !facebook.trim().isEmpty()) ||
                (site != null && !site.trim().isEmpty());
    }

    /**
     * Calcula a porcentagem de completude do perfil
     */
    public int getPercentualCompletude() {
        int total = 0;
        int preenchidos = 0;

        // Campos obrigatórios (sempre contam)
        total += 5; // nome, email, cnpj, telefone, cep
        if (isCadastroBasicoCompleto()) preenchidos += 5;

        // Campos opcionais importantes
        total += 7; // descrição, logradouro, numero, bairro, cidade, estado, dataFundacao

        if (descricao != null && !descricao.trim().isEmpty()) preenchidos++;
        if (logradouro != null && !logradouro.trim().isEmpty()) preenchidos++;
        if (numero != null && !numero.trim().isEmpty()) preenchidos++;
        if (bairro != null && !bairro.trim().isEmpty()) preenchidos++;
        if (cidade != null && !cidade.trim().isEmpty()) preenchidos++;
        if (estado != null && !estado.trim().isEmpty()) preenchidos++;
        if (dataFundacao != null) preenchidos++;

        // Logo
        total += 1;
        if (logoUrl != null && !logoUrl.trim().isEmpty()) preenchidos++;

        // Pelo menos uma rede social
        total += 1;
        if (temRedesSociais()) preenchidos++;

        return total > 0 ? (preenchidos * 100) / total : 0;
    }

    /**
     * Retorna uma versão formatada do CNPJ
     */
    public String getCnpjFormatado() {
        if (cnpj == null || cnpj.length() != 14) {
            return cnpj;
        }
        return cnpj.replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
    }

    /**
     * Retorna uma versão formatada do telefone
     */
    public String getTelefoneFormatado() {
        if (telefone == null) {
            return null;
        }

        String clean = telefone.replaceAll("\\D", "");
        if (clean.length() == 11) {
            return clean.replaceAll("(\\d{2})(\\d{5})(\\d{4})", "($1) $2-$3");
        } else if (clean.length() == 10) {
            return clean.replaceAll("(\\d{2})(\\d{4})(\\d{4})", "($1) $2-$3");
        }
        return telefone;
    }

    /**
     * Retorna uma versão formatada do CEP
     */
    public String getCepFormatado() {
        if (cep == null || cep.length() != 8) {
            return cep;
        }
        return cep.replaceAll("(\\d{5})(\\d{3})", "$1-$2");
    }

    /**
     * Retorna o endereço completo em uma linha
     */
    public String getEnderecoCompleto() {
        StringBuilder endereco = new StringBuilder();

        if (logradouro != null && !logradouro.trim().isEmpty()) {
            endereco.append(logradouro);

            if (numero != null && !numero.trim().isEmpty()) {
                endereco.append(", ").append(numero);
            }

            if (complemento != null && !complemento.trim().isEmpty()) {
                endereco.append(" - ").append(complemento);
            }
        }

        if (bairro != null && !bairro.trim().isEmpty()) {
            if (endereco.length() > 0) {
                endereco.append(", ");
            }
            endereco.append(bairro);
        }

        if (cidade != null && !cidade.trim().isEmpty()) {
            if (endereco.length() > 0) {
                endereco.append(", ");
            }
            endereco.append(cidade);

            if (estado != null && !estado.trim().isEmpty()) {
                endereco.append(" - ").append(estado);
            }
        }

        if (cep != null && !cep.trim().isEmpty()) {
            if (endereco.length() > 0) {
                endereco.append(" - ");
            }
            endereco.append(getCepFormatado());
        }

        return endereco.toString();
    }

    /**
     * Limpa e formata o Instagram (remove @ se presente)
     */
    public void setInstagram(String instagram) {
        if (instagram != null && !instagram.trim().isEmpty()) {
            this.instagram = instagram.trim().replaceAll("^@", "");
        } else {
            this.instagram = instagram;
        }
    }

    /**
     * Formata e valida a URL do site
     */
    public void setSite(String site) {
        if (site != null && !site.trim().isEmpty()) {
            String cleanSite = site.trim();
            if (!cleanSite.startsWith("http://") && !cleanSite.startsWith("https://")) {
                cleanSite = "https://" + cleanSite;
            }
            this.site = cleanSite;
        } else {
            this.site = site;
        }
    }

    /**
     * Retorna URL do Instagram formatada
     */
    public String getInstagramUrl() {
        if (instagram != null && !instagram.trim().isEmpty()) {
            return "https://instagram.com/" + instagram.trim().replaceAll("^@", "");
        }
        return null;
    }

    /**
     * Retorna URL do Facebook formatada
     */
    public String getFacebookUrl() {
        if (facebook != null && !facebook.trim().isEmpty()) {
            return "https://facebook.com/" + facebook.trim();
        }
        return null;
    }

    // Método toString para debug
    @Override
    public String toString() {
        return "Ong{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", email='" + email + '\'' +
                ", cnpj='" + cnpj + '\'' +
                ", telefone='" + telefone + '\'' +
                ", bairro='" + bairro + '\'' +
                ", cidade='" + cidade + '\'' +
                ", estado='" + estado + '\'' +
                ", cadastroFinalizado=" + cadastroFinalizado +
                '}';
    }
}