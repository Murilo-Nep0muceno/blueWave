package com.blueWave.BlueWave.repository;

import com.blueWave.BlueWave.model.Inscricao;
import com.blueWave.BlueWave.model.Voluntario;
import com.blueWave.BlueWave.model.Vagas;
import com.blueWave.BlueWave.model.Ong;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {

    List<Inscricao> findByVoluntario(Voluntario voluntario);
    List<Inscricao> findByVoluntarioOrderByDataInscricaoDesc(Voluntario voluntario);
    List<Inscricao> findByVagaOrderByDataInscricaoDesc(Vagas vaga);
    Page<Inscricao> findByVaga(Vagas vaga, Pageable pageable);

    @Query("SELECT i FROM Inscricao i WHERE i.vaga.ong = :ong ORDER BY i.dataInscricao DESC")
    List<Inscricao> findByOngOrderByDataInscricaoDesc(@Param("ong") Ong ong);

    @Query("SELECT i FROM Inscricao i WHERE i.vaga.ong = :ong ORDER BY i.dataInscricao DESC")
    Page<Inscricao> findByOngOrderByDataInscricaoDesc(@Param("ong") Ong ong, Pageable pageable);

    boolean existsByVoluntarioAndVaga(Voluntario voluntario, Vagas vaga);

    @Query("SELECT COUNT(i) > 0 FROM Inscricao i WHERE i.voluntario = :voluntario AND i.vaga.data = :data")
    boolean existsByVoluntarioAndVagaData(@Param("voluntario") Voluntario voluntario, @Param("data") LocalDate data);

    Optional<Inscricao> findByVoluntarioAndVaga(Voluntario voluntario, Vagas vaga);

    long countByVoluntario(Voluntario voluntario);
    long countByVaga(Vagas vaga);
    long countByVagaId(Long vagaId);

    @Query("SELECT COUNT(i) FROM Inscricao i WHERE i.vaga.ong = :ong")
    long countByOng(@Param("ong") Ong ong);

    @Query("SELECT COUNT(DISTINCT i.voluntario) FROM Inscricao i WHERE i.vaga.ong = :ong")
    long countDistinctVoluntariosByOng(@Param("ong") Ong ong);

    @Query("SELECT COUNT(DISTINCT i.vaga) FROM Inscricao i WHERE i.vaga.ong = :ong")
    long countDistinctVagasWithInscricoesByOng(@Param("ong") Ong ong);

    @Query("SELECT i FROM Inscricao i WHERE i.vaga.ong = :ong AND " +
            "LOWER(i.voluntario.nomeVoluntario) LIKE LOWER(CONCAT('%', :nome, '%')) " +
            "ORDER BY i.dataInscricao DESC")
    Page<Inscricao> findByOngAndVoluntarioNome(@Param("ong") Ong ong,
                                               @Param("nome") String nome,
                                               Pageable pageable);

    @Query("SELECT i FROM Inscricao i WHERE i.vaga = :vaga AND " +
            "LOWER(i.voluntario.nomeVoluntario) LIKE LOWER(CONCAT('%', :nome, '%')) " +
            "ORDER BY i.dataInscricao DESC")
    Page<Inscricao> findByVagaAndVoluntarioNomeVoluntarioContainingIgnoreCase(
            @Param("vaga") Vagas vaga,
            @Param("nome") String nome,
            Pageable pageable);

    @Query("SELECT i FROM Inscricao i WHERE i.voluntario = :voluntario AND i.vaga.data >= :dataAtual " +
            "ORDER BY i.vaga.data ASC")
    List<Inscricao> findInscricoesFuturas(@Param("voluntario") Voluntario voluntario,
                                          @Param("dataAtual") LocalDate dataAtual);

    @Query("SELECT i FROM Inscricao i WHERE i.voluntario = :voluntario AND i.vaga.data < :dataAtual " +
            "ORDER BY i.vaga.data DESC")
    List<Inscricao> findInscricoesPassadas(@Param("voluntario") Voluntario voluntario,
                                           @Param("dataAtual") LocalDate dataAtual);

    @Query("SELECT i FROM Inscricao i WHERE i.vaga.data = :dataAmanha " +
            "ORDER BY i.dataInscricao")
    List<Inscricao> findInscricoesParaLembrete(@Param("dataAmanha") LocalDate dataAmanha);

    @Query("SELECT i FROM Inscricao i WHERE i.vaga.id IN :vagaIds ORDER BY i.dataInscricao DESC")
    List<Inscricao> findByVagaIdIn(@Param("vagaIds") List<Long> vagaIds);

    @Query("SELECT i FROM Inscricao i WHERE i.vaga.id IN :vagaIds ORDER BY i.dataInscricao DESC")
    Page<Inscricao> findByVagaIdIn(@Param("vagaIds") List<Long> vagaIds, Pageable pageable);

    @Query("SELECT COUNT(i) FROM Inscricao i WHERE i.vaga.id IN :vagaIds")
    long countByVagaIdIn(@Param("vagaIds") List<Long> vagaIds);

    @Query("SELECT i FROM Inscricao i WHERE i.vaga.id IN :vagaIds AND " +
            "LOWER(i.voluntario.nomeVoluntario) LIKE LOWER(CONCAT('%', :nome, '%')) " +
            "ORDER BY i.dataInscricao DESC")
    Page<Inscricao> findByVagaIdInAndVoluntarioNomeVoluntarioContainingIgnoreCase(
            @Param("vagaIds") List<Long> vagaIds,
            @Param("nome") String nome,
            Pageable pageable);

    @Query("SELECT i FROM Inscricao i WHERE i.vaga.id = :vagaId ORDER BY i.dataInscricao DESC")
    Page<Inscricao> findByVagaId(@Param("vagaId") Long vagaId, Pageable pageable);

    @Query("SELECT i FROM Inscricao i WHERE i.vaga.id = :vagaId AND " +
            "LOWER(i.voluntario.nomeVoluntario) LIKE LOWER(CONCAT('%', :nome, '%')) " +
            "ORDER BY i.dataInscricao DESC")
    Page<Inscricao> findByVagaIdAndVoluntarioNomeVoluntarioContainingIgnoreCase(
            @Param("vagaId") Long vagaId,
            @Param("nome") String nome,
            Pageable pageable);

    @Query("SELECT i FROM Inscricao i " +
            "JOIN FETCH i.vaga v " +
            "JOIN FETCH v.ong " +
            "WHERE i.voluntario = :voluntario " +
            "ORDER BY i.dataInscricao DESC")
    List<Inscricao> findByVoluntarioWithVagaAndOng(@Param("voluntario") Voluntario voluntario);

    List<Inscricao> findByVaga(Vagas vaga);
}