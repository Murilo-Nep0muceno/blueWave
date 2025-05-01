package com.blueWave.BlueWave.repository;

import com.blueWave.BlueWave.model.Inscricao;
import com.blueWave.BlueWave.model.Vagas;
import com.blueWave.BlueWave.model.Voluntario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {
    List<Inscricao> findByVoluntario(Voluntario voluntario);

    boolean existsByVoluntarioAndVaga(Voluntario voluntario, Vagas vaga);

}
