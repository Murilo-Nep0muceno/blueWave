package com.blueWave.BlueWave.repository;

import com.blueWave.BlueWave.model.Voluntario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoluntarioRepository extends JpaRepository<Voluntario, Long> {
    Voluntario findByEmail(String email);

}
