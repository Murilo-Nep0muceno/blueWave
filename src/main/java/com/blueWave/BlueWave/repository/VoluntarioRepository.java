package com.blueWave.BlueWave.repository;

import com.blueWave.BlueWave.model.Voluntario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoluntarioRepository extends JpaRepository<Voluntario, Long> {

    // Buscar por campos únicos - ambos os métodos para compatibilidade
    Optional<Voluntario> findByEmail(String email);
    Voluntario findVoluntarioByEmail(String email); // Método adicional que retorna diretamente

    Optional<Voluntario> findByCpf(String cpf);
    Voluntario findVoluntarioByCpf(String cpf); // Método adicional que retorna diretamente

    // Verificações de existência
    boolean existsByEmail(String email);
    boolean existsByCpf(String cpf);
    boolean existsByTelefone(String telefone);

    // Sistema de banimento
    long countByBanidoTrue();
    List<Voluntario> findByBanidoTrue();
    List<Voluntario> findByBanidoTrueOrderByDataBanimentoDesc();

    // Buscar voluntários com banimento temporário expirado
    @Query("SELECT v FROM Voluntario v WHERE v.banido = true AND v.tipoBanimento = :tipoBanimento AND v.dataFimBanimento < :data")
    List<Voluntario> findByBanidoTrueAndTipoBanimentoAndDataFimBanimentoBefore(
            @Param("tipoBanimento") Voluntario.TipoBanimento tipoBanimento,
            @Param("data") LocalDateTime data
    );

    // Buscar voluntários ativos (não banidos)
    List<Voluntario> findByAtivoTrueAndBanidoFalse();
    List<Voluntario> findByAtivoTrue();

    // Verificar se voluntário pode logar (ativo e não banido)
    @Query("SELECT v FROM Voluntario v WHERE v.email = :email AND v.ativo = true AND v.banido = false")
    Optional<Voluntario> findByEmailAndCanLogin(@Param("email") String email);

    // Buscar voluntários por status de banimento
    @Query("SELECT v FROM Voluntario v WHERE v.banido = :banido ORDER BY v.dataBanimento DESC")
    List<Voluntario> findByBanido(@Param("banido") Boolean banido);

    // Buscar voluntários banidos por tipo de banimento
    @Query("SELECT v FROM Voluntario v WHERE v.banido = true AND v.tipoBanimento = :tipo ORDER BY v.dataBanimento DESC")
    List<Voluntario> findByBanidoTrueAndTipoBanimento(@Param("tipo") Voluntario.TipoBanimento tipo);

    // Verificar se existe voluntário banido por email
    @Query("SELECT COUNT(v) > 0 FROM Voluntario v WHERE v.email = :email AND v.banido = true")
    boolean existsByEmailAndBanidoTrue(@Param("email") String email);

    // Verificar se existe voluntário banido por CPF
    @Query("SELECT COUNT(v) > 0 FROM Voluntario v WHERE v.cpf = :cpf AND v.banido = true")
    boolean existsByCpfAndBanidoTrue(@Param("cpf") String cpf);

    // Buscar por faixa etária (se necessário)
    @Query("SELECT v FROM Voluntario v WHERE v.dataNascimento BETWEEN :dataInicio AND :dataFim")
    List<Voluntario> findByDataNascimentoBetween(@Param("dataInicio") String dataInicio, @Param("dataFim") String dataFim);

    // Buscar por sexo
    List<Voluntario> findBySexo(String sexo);
}