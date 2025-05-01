package com.blueWave.BlueWave.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.br.CPF;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "voluntario")
public class Voluntario {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @CPF(message = "CPF inválido")
    @NotBlank(message = "CPF é obrigatório")
    @Column(unique = true, length = 11)
    private String cpf;

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    @Column(name = "nome_voluntario", length = 100)
    private String nomeVoluntario;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Column(unique = true, length = 100)
    private String email;

    @NotBlank(message = "Telefone é obrigatório")
    @Pattern(regexp = "^\\d{10,11}$", message = "Telefone deve ter 10 ou 11 dígitos")
    @Column(length = 11)
    private String telefone;

    @NotBlank(message = "Data de nascimento é obrigatória")
    @Column(name = "data_nascimento")
    private String dataNascimento;

    @NotBlank(message = "Sexo é obrigatório")
    @Pattern(regexp = "^(masculino|feminino|indefinido)$", message = "Sexo deve ser: masculino, feminino ou indefinido")
    @Column(length = 20)
    private String sexo;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter pelo menos 8 caracteres")
    @Column(length = 255) // BCrypt gera hashes de ~60 caracteres, mas é bom ter margem
    private String senha;

    @OneToMany(mappedBy = "voluntario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Inscricao> inscricoes = new ArrayList<>();

    // Método para limpar dados antes de salvar
    @PrePersist
    @PreUpdate
    public void limparDados() {
        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
        if (this.cpf != null) {
            this.cpf = this.cpf.replaceAll("\\D", "");
        }
        if (this.telefone != null) {
            this.telefone = this.telefone.replaceAll("\\D", "");
        }
        if (this.nomeVoluntario != null) {
            this.nomeVoluntario = this.nomeVoluntario.trim();
        }
    }
}