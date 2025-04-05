package com.blueWave.BlueWave.repository;

import com.blueWave.BlueWave.model.Voluntario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoluntarioRepository extends JpaRepository<Voluntario, Long> {
    Voluntario findByEmail(String email);

    Voluntario findByEmailAndSenha(@NotBlank @Email(message = "Email inválido") String email, @NotBlank String senha);

}
