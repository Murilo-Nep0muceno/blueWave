package com.blueWave.BlueWave.repository;

import com.blueWave.BlueWave.model.Ong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OngRepository extends JpaRepository<Ong, Long> {

    // Verificações de existência básicas
    boolean existsByCnpj(String cnpj);
    boolean existsByEmail(String email);
    boolean existsByTelefone(String telefone);

    // Buscar por campos únicos - ambos os métodos para compatibilidade
    Optional<Ong> findByCnpj(String cnpj);
    Ong findOngByCnpj(String cnpj); // Método adicional que retorna diretamente

    Optional<Ong> findByEmail(String email);
    Ong findOngByEmail(String email); // Método adicional que retorna diretamente

    // Sistema de banimento
    long countByBanidoTrue();
    List<Ong> findByBanidoTrue();
    List<Ong> findByBanidoTrueOrderByDataBanimentoDesc();

    // Buscar ONGs com banimento temporário expirado
    @Query("SELECT o FROM Ong o WHERE o.banido = true AND o.tipoBanimento = :tipoBanimento AND o.dataFimBanimento < :data")
    List<Ong> findByBanidoTrueAndTipoBanimentoAndDataFimBanimentoBefore(
            @Param("tipoBanimento") Ong.TipoBanimento tipoBanimento,
            @Param("data") LocalDateTime data
    );

    // Buscar ONGs ativas (não banidas)
    List<Ong> findByAtivoTrueAndBanidoFalse();
    List<Ong> findByAtivoTrue();

    // Verificar se ONG pode logar (ativa e não banida)
    @Query("SELECT o FROM Ong o WHERE o.email = :email AND o.ativo = true AND o.banido = false")
    Optional<Ong> findByEmailAndCanLogin(@Param("email") String email);

    // Buscar ONGs por status de banimento
    @Query("SELECT o FROM Ong o WHERE o.banido = :banido ORDER BY o.dataBanimento DESC")
    List<Ong> findByBanido(@Param("banido") Boolean banido);

    // Buscar ONGs banidas por tipo de banimento
    @Query("SELECT o FROM Ong o WHERE o.banido = true AND o.tipoBanimento = :tipo ORDER BY o.dataBanimento DESC")
    List<Ong> findByBanidoTrueAndTipoBanimento(@Param("tipo") Ong.TipoBanimento tipo);

    // Verificar se existe ONG banida por email
    @Query("SELECT COUNT(o) > 0 FROM Ong o WHERE o.email = :email AND o.banido = true")
    boolean existsByEmailAndBanidoTrue(@Param("email") String email);

    // Verificar se existe ONG banida por CNPJ
    @Query("SELECT COUNT(o) > 0 FROM Ong o WHERE o.cnpj = :cnpj AND o.banido = true")
    boolean existsByCnpjAndBanidoTrue(@Param("cnpj") String cnpj);

    // Buscar por cidade
    List<Ong> findByCidade(String cidade);

    // Buscar por estado
    List<Ong> findByEstado(String estado);

    // Buscar ONGs com cadastro finalizado
    List<Ong> findByCadastroFinalizadoTrue();

    // Buscar ONGs com cadastro pendente
    List<Ong> findByCadastroFinalizadoFalse();
}