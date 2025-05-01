package com.blueWave.BlueWave.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "inscricao")
public class Inscricao {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.EAGER)
        @JoinColumn(name = "voluntario_id", nullable = false)
        private Voluntario voluntario;

        @ManyToOne(fetch = FetchType.EAGER)
        @JoinColumn(name = "vaga_id", nullable = false)
        private Vagas vaga;

        @Column(name = "data_inscricao", nullable = false)
        private LocalDate dataInscricao;

        // Construtor para facilitar a criação
        public Inscricao(Voluntario voluntario, Vagas vaga, LocalDate dataInscricao) {
                this.voluntario = voluntario;
                this.vaga = vaga;
                this.dataInscricao = dataInscricao;
        }
}