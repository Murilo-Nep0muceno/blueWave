package com.blueWave.BlueWave.repository;

import com.blueWave.BlueWave.model.Inscricao;
import com.blueWave.BlueWave.model.Vagas;
import com.blueWave.BlueWave.model.Voluntario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {

    // Buscar inscrições por voluntário com EAGER fetch
    @Query("SELECT i FROM Inscricao i JOIN FETCH i.vaga v JOIN FETCH v.ong WHERE i.voluntario = :voluntario ORDER BY v.data DESC")
    List<Inscricao> findByVoluntario(@Param("voluntario") Voluntario voluntario);

    // Buscar inscrições por vaga com paginação
    Page<Inscricao> findByVagaId(Long vagaId, Pageable pageable);

    // Buscar inscrições por lista de vagas com paginação
    Page<Inscricao> findByVagaIdIn(List<Long> vagaIds, Pageable pageable);

    // Buscar inscrições por vaga e nome do voluntário (filtro)
    @Query("SELECT i FROM Inscricao i JOIN FETCH i.vaga v JOIN FETCH v.ong WHERE v.id = :vagaId AND LOWER(i.voluntario.nomeVoluntario) LIKE LOWER(CONCAT('%', :nome, '%'))")
    Page<Inscricao> findByVagaIdAndVoluntarioNomeVoluntarioContainingIgnoreCase(
            @Param("vagaId") Long vagaId, @Param("nome") String nome, Pageable pageable);

    // Buscar inscrições por lista de vagas e nome do voluntário (filtro)
    @Query("SELECT i FROM Inscricao i JOIN FETCH i.vaga v JOIN FETCH v.ong WHERE v.id IN :vagaIds AND LOWER(i.voluntario.nomeVoluntario) LIKE LOWER(CONCAT('%', :nome, '%'))")
    Page<Inscricao> findByVagaIdInAndVoluntarioNomeVoluntarioContainingIgnoreCase(
            @Param("vagaIds") List<Long> vagaIds, @Param("nome") String nome, Pageable pageable);

    // Contar inscrições por vaga
    long countByVagaId(Long vagaId);

    // Contar inscrições por lista de vagas
    long countByVagaIdIn(List<Long> vagaIds);

    // Buscar inscrições por lista de vagas (sem paginação) com EAGER fetch
    @Query("SELECT i FROM Inscricao i JOIN FETCH i.vaga v JOIN FETCH v.ong WHERE v.id IN :vagaIds")
    List<Inscricao> findByVagaIdIn(@Param("vagaIds") List<Long> vagaIds);

    // Verificar se já existe inscrição do voluntário na vaga
    boolean existsByVoluntarioAndVaga(Voluntario voluntario, Vagas vaga);

    // Buscar inscrições com detalhes completos por voluntário
    @Query("SELECT i FROM Inscricao i JOIN FETCH i.vaga v JOIN FETCH v.ong o JOIN FETCH i.voluntario vol WHERE vol = :voluntario")
    List<Inscricao> findByVoluntarioWithFullDetails(@Param("voluntario") Voluntario voluntario);

    // Contar inscrições ativas por voluntário
    @Query("SELECT COUNT(i) FROM Inscricao i WHERE i.voluntario = :voluntario AND i.vaga.data >= CURRENT_DATE")
    long countActiveInscricoesByVoluntario(@Param("voluntario") Voluntario voluntario);

    // Buscar inscrições por vaga com detalhes
    @Query("SELECT i FROM Inscricao i JOIN FETCH i.voluntario v WHERE i.vaga = :vaga ORDER BY i.dataInscricao DESC")
    List<Inscricao> findByVagaWithVoluntario(@Param("vaga") Vagas vaga);
}