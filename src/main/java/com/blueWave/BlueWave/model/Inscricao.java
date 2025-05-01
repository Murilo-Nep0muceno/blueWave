package com.blueWave.BlueWave.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@Getter
@Setter
public class Inscricao {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "voluntario_id")
        private Voluntario voluntario;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "vaga_id")
        private Vagas vaga;

        private LocalDate dataInscricao;

    public Inscricao() {

    }
}
