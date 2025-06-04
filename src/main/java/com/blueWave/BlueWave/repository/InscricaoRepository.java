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

    // Buscar inscrições por voluntário (método simples para compatibilidade)
    List<Inscricao> findByVoluntario(Voluntario voluntario);

    // Buscar inscrições por voluntário ordenado por data
    List<Inscricao> findByVoluntarioOrderByDataInscricaoDesc(Voluntario voluntario);

    // Buscar inscrições por vaga
    List<Inscricao> findByVagaOrderByDataInscricaoDesc(Vagas vaga);
    Page<Inscricao> findByVaga(Vagas vaga, Pageable pageable);

    // Buscar inscrições por ONG
    @Query("SELECT i FROM Inscricao i WHERE i.vaga.ong = :ong ORDER BY i.dataInscricao DESC")
    List<Inscricao> findByOngOrderByDataInscricaoDesc(@Param("ong") Ong ong);

    // Buscar inscrições por ONG com paginação
    @Query("SELECT i FROM Inscricao i WHERE i.vaga.ong = :ong ORDER BY i.dataInscricao DESC")
    Page<Inscricao> findByOngOrderByDataInscricaoDesc(@Param("ong") Ong ong, Pageable pageable);

    // Verificar se voluntário já está inscrito na vaga
    boolean existsByVoluntarioAndVaga(Voluntario voluntario, Vagas vaga);

    // Verificar se voluntário já está inscrito em alguma vaga na mesma data
    @Query("SELECT COUNT(i) > 0 FROM Inscricao i WHERE i.voluntario = :voluntario AND i.vaga.data = :data")
    boolean existsByVoluntarioAndVagaData(@Param("voluntario") Voluntario voluntario, @Param("data") LocalDate data);

    // Buscar inscrição específica
    Optional<Inscricao> findByVoluntarioAndVaga(Voluntario voluntario, Vagas vaga);

    // Contar inscrições por voluntário
    long countByVoluntario(Voluntario voluntario);

    // Contar inscrições por vaga
    long countByVaga(Vagas vaga);
    long countByVagaId(Long vagaId);

    // Contar inscrições por ONG
    @Query("SELECT COUNT(i) FROM Inscricao i WHERE i.vaga.ong = :ong")
    long countByOng(@Param("ong") Ong ong);

    // Buscar voluntários únicos por ONG
    @Query("SELECT COUNT(DISTINCT i.voluntario) FROM Inscricao i WHERE i.vaga.ong = :ong")
    long countDistinctVoluntariosByOng(@Param("ong") Ong ong);

    // Buscar vagas com inscrições por ONG
    @Query("SELECT COUNT(DISTINCT i.vaga) FROM Inscricao i WHERE i.vaga.ong = :ong")
    long countDistinctVagasWithInscricoesByOng(@Param("ong") Ong ong);

    // Buscar inscrições por nome do voluntário (para ONG)
    @Query("SELECT i FROM Inscricao i WHERE i.vaga.ong = :ong AND " +
            "LOWER(i.voluntario.nomeVoluntario) LIKE LOWER(CONCAT('%', :nome, '%')) " +
            "ORDER BY i.dataInscricao DESC")
    Page<Inscricao> findByOngAndVoluntarioNome(@Param("ong") Ong ong,
                                               @Param("nome") String nome,
                                               Pageable pageable);

    // Buscar inscrições por vaga e nome do voluntário
    @Query("SELECT i FROM Inscricao i WHERE i.vaga = :vaga AND " +
            "LOWER(i.voluntario.nomeVoluntario) LIKE LOWER(CONCAT('%', :nome, '%')) " +
            "ORDER BY i.dataInscricao DESC")
    Page<Inscricao> findByVagaAndVoluntarioNomeVoluntarioContainingIgnoreCase(
            @Param("vaga") Vagas vaga,
            @Param("nome") String nome,
            Pageable pageable);

    // Buscar inscrições futuras do voluntário
    @Query("SELECT i FROM Inscricao i WHERE i.voluntario = :voluntario AND i.vaga.data >= :dataAtual " +
            "ORDER BY i.vaga.data ASC")
    List<Inscricao> findInscricoesFuturas(@Param("voluntario") Voluntario voluntario,
                                          @Param("dataAtual") LocalDate dataAtual);

    // Buscar inscrições passadas do voluntário
    @Query("SELECT i FROM Inscricao i WHERE i.voluntario = :voluntario AND i.vaga.data < :dataAtual " +
            "ORDER BY i.vaga.data DESC")
    List<Inscricao> findInscricoesPassadas(@Param("voluntario") Voluntario voluntario,
                                           @Param("dataAtual") LocalDate dataAtual);

    // Buscar inscrições para lembrete (vagas que acontecem amanhã)
    @Query("SELECT i FROM Inscricao i WHERE i.vaga.data = :dataAmanha " +
            "ORDER BY i.dataInscricao")
    List<Inscricao> findInscricoesParaLembrete(@Param("dataAmanha") LocalDate dataAmanha);

    // ===== MÉTODOS AUXILIARES PARA COMPATIBILIDADE =====

    // Buscar por lista de IDs de vagas
    @Query("SELECT i FROM Inscricao i WHERE i.vaga.id IN :vagaIds ORDER BY i.dataInscricao DESC")
    List<Inscricao> findByVagaIdIn(@Param("vagaIds") List<Long> vagaIds);

    @Query("SELECT i FROM Inscricao i WHERE i.vaga.id IN :vagaIds ORDER BY i.dataInscricao DESC")
    Page<Inscricao> findByVagaIdIn(@Param("vagaIds") List<Long> vagaIds, Pageable pageable);

    // Contar por lista de IDs de vagas
    @Query("SELECT COUNT(i) FROM Inscricao i WHERE i.vaga.id IN :vagaIds")
    long countByVagaIdIn(@Param("vagaIds") List<Long> vagaIds);

    // Buscar por lista de IDs de vagas e nome do voluntário
    @Query("SELECT i FROM Inscricao i WHERE i.vaga.id IN :vagaIds AND " +
            "LOWER(i.voluntario.nomeVoluntario) LIKE LOWER(CONCAT('%', :nome, '%')) " +
            "ORDER BY i.dataInscricao DESC")
    Page<Inscricao> findByVagaIdInAndVoluntarioNomeVoluntarioContainingIgnoreCase(
            @Param("vagaIds") List<Long> vagaIds,
            @Param("nome") String nome,
            Pageable pageable);

    // Buscar por ID da vaga específica
    @Query("SELECT i FROM Inscricao i WHERE i.vaga.id = :vagaId ORDER BY i.dataInscricao DESC")
    Page<Inscricao> findByVagaId(@Param("vagaId") Long vagaId, Pageable pageable);

    // Buscar por ID da vaga e nome do voluntário
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


}