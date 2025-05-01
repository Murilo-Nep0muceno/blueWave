package com.blueWave.BlueWave.repository;

import com.blueWave.BlueWave.model.Inscricao;
import com.blueWave.BlueWave.model.Ong;
import com.blueWave.BlueWave.model.Vagas;
import com.blueWave.BlueWave.model.Voluntario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OngRepository  extends JpaRepository<Ong, Long> {
    Ong findByEmail(String email);

    public interface VoluntarioRepository extends JpaRepository<Voluntario, Long> {
        Optional<Voluntario> findByEmail(String email);
    }

    public interface VagaRepository extends JpaRepository<Vagas, Long> {
        List<Vagas> findByOngId(Long ongId);
    }

    public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {
        boolean existsByVagaIdAndVoluntarioId(Long vagaId, Long voluntarioId);
}}
