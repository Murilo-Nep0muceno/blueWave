package com.blueWave.BlueWave.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "admin")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    @Column(name = "nome", length = 100, nullable = false)
    private String nome;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter um formato válido")
    @Column(unique = true, length = 100, nullable = false)
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter pelo menos 8 caracteres")
    @Column(length = 255, nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_acesso", nullable = false)
    private NivelAcesso nivelAcesso = NivelAcesso.ADMIN;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "ultimo_login")
    private LocalDateTime ultimoLogin;

    @Column(name = "tentativas_login")
    private Integer tentativasLogin = 0;

    @Column(name = "bloqueado_ate")
    private LocalDateTime bloqueadoAte;

    public void setUltimoLoginFormatado(String format) {
    }

    // Enum para níveis de acesso
    public enum NivelAcesso {
        SUPER_ADMIN("Super Administrador"),
        ADMIN("Administrador"),
        MODERADOR("Moderador");

        private final String descricao;

        NivelAcesso(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    // Construtor para facilitar criação
    public Admin(String nome, String email, String senha) {
        this.nome = nome;
        this.email = email.toLowerCase().trim();
        this.senha = senha;
        this.nivelAcesso = NivelAcesso.ADMIN;
        this.ativo = true;
        this.dataCriacao = LocalDateTime.now();
        this.tentativasLogin = 0;
    }

    // Métodos utilitários

    /**
     * Verifica se o admin está bloqueado
     */
    public boolean isBloqueado() {
        return bloqueadoAte != null && bloqueadoAte.isAfter(LocalDateTime.now());
    }

    /**
     * Verifica se pode fazer login
     */
    public boolean podeLogar() {
        return ativo && !isBloqueado();
    }

    /**
     * Registra tentativa de login falhada
     */
    public void registrarTentativaFalha() {
        this.tentativasLogin++;

        // Bloquear após 5 tentativas
        if (this.tentativasLogin >= 5) {
            this.bloqueadoAte = LocalDateTime.now().plusMinutes(30);
        }
    }

    /**
     * Registra login bem-sucedido
     */
    public void registrarLoginSucesso() {
        this.ultimoLogin = LocalDateTime.now();
        this.tentativasLogin = 0;
        this.bloqueadoAte = null;
    }

    /**
     * Verifica se é super admin
     */
    public boolean isSuperAdmin() {
        return this.nivelAcesso == NivelAcesso.SUPER_ADMIN;
    }

    /**
     * Verifica se é admin
     */
    public boolean isAdmin() {
        return this.nivelAcesso == NivelAcesso.ADMIN || isSuperAdmin();
    }

    /**
     * Verifica se é moderador
     */
    public boolean isModerador() {
        return this.nivelAcesso == NivelAcesso.MODERADOR || isAdmin();
    }

    /**
     * Formata o último login
     */
    public String getUltimoLoginFormatado() {
        return null;
    }

    /**
     * Limpa dados antes de salvar
     */
    @PrePersist
    @PreUpdate
    public void limparDados() {
        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
        if (this.nome != null) {
            this.nome = this.nome.trim();
        }
        if (this.dataCriacao == null) {
            this.dataCriacao = LocalDateTime.now();
        }
    }

    @Override
    public String toString() {
        return "Admin{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", email='" + email + '\'' +
                ", nivelAcesso=" + nivelAcesso +
                ", ativo=" + ativo +
                ", dataCriacao=" + dataCriacao +
                ", ultimoLogin=" + ultimoLogin +
                '}';
    }
}