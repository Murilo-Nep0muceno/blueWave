package com.blueWave.BlueWave.repository;

import com.blueWave.BlueWave.model.Ong;
import com.blueWave.BlueWave.model.Vagas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VagasRepository extends JpaRepository<Vagas, Long> {

    // ===== MÉTODOS EXISTENTES =====
    List<Vagas> findByOng(Ong ong);
    List<Vagas> findByOngId(Long ongId);

    @Query("SELECT v FROM Vagas v WHERE v.data >= :dataAtual ORDER BY v.data ASC")
    List<Vagas> findVagasAtivas(@Param("dataAtual") LocalDate dataAtual);

    @Query("SELECT v FROM Vagas v JOIN FETCH v.ong WHERE v.data >= :dataAtual ORDER BY v.data ASC")
    List<Vagas> findVagasAtivasComOng(@Param("dataAtual") LocalDate dataAtual);

    @Query("SELECT v FROM Vagas v JOIN v.ong o WHERE o.cidade = :cidade AND v.data >= :dataAtual ORDER BY v.data ASC")
    List<Vagas> findVagasAtivasPorCidade(@Param("cidade") String cidade, @Param("dataAtual") LocalDate dataAtual);

    @Query("SELECT v FROM Vagas v JOIN v.ong o WHERE o.estado = :estado AND v.data >= :dataAtual ORDER BY v.data ASC")
    List<Vagas> findVagasAtivasPorEstado(@Param("estado") String estado, @Param("dataAtual") LocalDate dataAtual);

    @Query("SELECT v FROM Vagas v WHERE LOWER(v.nome) LIKE LOWER(CONCAT('%', :nome, '%')) AND v.data >= :dataAtual ORDER BY v.data ASC")
    List<Vagas> findVagasAtivasPorNome(@Param("nome") String nome, @Param("dataAtual") LocalDate dataAtual);

    @Query("SELECT COUNT(v) FROM Vagas v WHERE v.ong.id = :ongId AND v.data >= :dataAtual")
    Long countVagasAtivasByOngId(@Param("ongId") Long ongId, @Param("dataAtual") LocalDate dataAtual);

    @Query("SELECT v FROM Vagas v JOIN FETCH v.ong WHERE v.data >= :dataAtual AND v.quantidade <= :quantidade ORDER BY v.quantidade ASC, v.data ASC")
    List<Vagas> findVagasUrgentes(@Param("dataAtual") LocalDate dataAtual, @Param("quantidade") Integer quantidade);

    @Query("SELECT v FROM Vagas v " +
            "JOIN FETCH v.ong " +
            "WHERE v.status = 'ATIVA' " +
            "AND v.data >= :dataAtual " +
            "ORDER BY v.data ASC")
    List<Vagas> findActiveVagasWithOng(@Param("dataAtual") LocalDate dataAtual);

    // ===== NOVOS MÉTODOS PARA ADMIN =====
    long countByOng(Ong ong);

    @Query("SELECT v FROM Vagas v WHERE v.status = :status")
    List<Vagas> findByStatus(@Param("status") Vagas.StatusVaga status);

    @Query("SELECT v FROM Vagas v WHERE v.ong = :ong AND v.status = :status")
    List<Vagas> findByOngAndStatus(@Param("ong") Ong ong, @Param("status") Vagas.StatusVaga status);

    @Query("SELECT v FROM Vagas v WHERE v.data = :amanha AND v.status = 'ATIVA'")
    List<Vagas> findVagasAmanha(@Param("amanha") LocalDate amanha);

    @Query("SELECT v FROM Vagas v WHERE v.data = :hoje AND v.status = 'ATIVA'")
    List<Vagas> findVagasHoje(@Param("hoje") LocalDate hoje);
}