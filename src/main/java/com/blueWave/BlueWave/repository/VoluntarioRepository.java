package com.blueWave.BlueWave.repository;

import com.blueWave.BlueWave.model.Voluntario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoluntarioRepository extends JpaRepository<Voluntario, Long> {
    Voluntario findByEmail(String email);

    Voluntario findByEmailAndSenha(@NotBlank @Email(message = "Email inválido") String email, @NotBlank String senha);

    // Buscar voluntário por CPF
    Voluntario findByCpf(String cpf);

    // Verificar se existe voluntário com email específico
    boolean existsByEmail(String email);

    // Verificar se existe voluntário com CPF específico
    boolean existsByCpf(String cpf);
    boolean existsByTelefone(String telefone);
}
