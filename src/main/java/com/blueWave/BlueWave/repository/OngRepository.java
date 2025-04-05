package com.blueWave.BlueWave.repository;

import com.blueWave.BlueWave.model.Ong;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OngRepository  extends JpaRepository<Ong, Long> {
    Ong findByEmail(String email);
}
