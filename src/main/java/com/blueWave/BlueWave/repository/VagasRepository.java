package com.blueWave.BlueWave.repository;

import com.blueWave.BlueWave.model.Ong;
import com.blueWave.BlueWave.model.Vagas;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VagasRepository extends JpaRepository<Vagas, Long> {
    List<Vagas> findByOng(Ong ong);

    // Método para buscar vagas por ONG ID
    List<Vagas> findByOngId(Long ongId);
}
