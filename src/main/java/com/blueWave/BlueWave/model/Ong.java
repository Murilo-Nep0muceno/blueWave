package com.blueWave.BlueWave.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

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


    @NotBlank
    @Size(min = 3)
    private String nome;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8)
    private String senha;

    @NotBlank
    @Size(min = 14, max = 14)
    private String cnpj;

    @NotBlank
    @Size(min = 11, max = 11)
    private String telefone;

    @NotBlank
    @Size(min = 8)
    private String cep;

    @OneToMany(mappedBy = "ong", cascade = CascadeType.ALL)
    private List<Vagas> vagas = new ArrayList<>();

    public Ong() {

    }
}
