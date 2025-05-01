package com.blueWave.BlueWave.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.br.CPF;

import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@Getter
@Setter
public class Voluntario {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @CPF
    private String cpf;

    @NotBlank
    private String nomeVoluntario;

    @NotBlank
    @Email(message = "Email inválido")
    @Column(unique = true)
    private String email;

    @NotBlank
    @Pattern(regexp = "^\\d{11}$", message = "Telefone inválido. Deve ter 11 dígitos.")
    private String telefone;

    @NotBlank
    private String dataNascimento;

    @NotBlank
    private String sexo;

    @NotBlank
    private String senha;

    @OneToMany(mappedBy = "voluntario", cascade = CascadeType.ALL)
    private List<Inscricao> inscricoes = new ArrayList<>();

    public Voluntario() {

    }
}



