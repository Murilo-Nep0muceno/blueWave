package com.blueWave.BlueWave.repository;

import com.blueWave.BlueWave.model.Ong;
import com.blueWave.BlueWave.model.ReportVoluntario;
import com.blueWave.BlueWave.model.Vagas;
import com.blueWave.BlueWave.model.Voluntario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportVoluntarioRepository extends JpaRepository<ReportVoluntario, Long> {

    // Verificar se já existe report da ONG para o voluntário em uma vaga específica
    boolean existsByOngAndVoluntarioAndVaga(Ong ong, Voluntario voluntario, Vagas vaga);

    // Verificar se já existe report da ONG para o voluntário (sem vaga específica)
    boolean existsByOngAndVoluntario(Ong ong, Voluntario voluntario);

    // Buscar reports por ONG
    List<ReportVoluntario> findByOng(Ong ong);
    Page<ReportVoluntario> findByOng(Ong ong, Pageable pageable);

    // Buscar reports por ONG ordenados por data
    @Query("SELECT r FROM ReportVoluntario r WHERE r.ong = :ong ORDER BY r.dataReport DESC")
    Page<ReportVoluntario> findByOngOrderByDataReportDesc(@Param("ong") Ong ong, Pageable pageable);

    // Buscar reports por voluntário
    List<ReportVoluntario> findByVoluntario(Voluntario voluntario);
    Page<ReportVoluntario> findByVoluntario(Voluntario voluntario, Pageable pageable);

    // Buscar reports por vaga
    List<ReportVoluntario> findByVaga(Vagas vaga);

    // Buscar reports por status
    List<ReportVoluntario> findByStatus(ReportVoluntario.StatusReportVoluntario status);
    Page<ReportVoluntario> findByStatus(ReportVoluntario.StatusReportVoluntario status, Pageable pageable);

    // Buscar reports por prioridade
    List<ReportVoluntario> findByPrioridade(ReportVoluntario.PrioridadeReportVoluntario prioridade);
    Page<ReportVoluntario> findByPrioridade(ReportVoluntario.PrioridadeReportVoluntario prioridade, Pageable pageable);

    // Buscar reports por assunto
    List<ReportVoluntario> findByAssunto(ReportVoluntario.AssuntoReportVoluntario assunto);

    // Buscar reports por ONG e status
    List<ReportVoluntario> findByOngAndStatus(Ong ong, ReportVoluntario.StatusReportVoluntario status);

    // Buscar reports por ONG e status ordenados por data
    @Query("SELECT r FROM ReportVoluntario r WHERE r.ong = :ong AND r.status = :status ORDER BY r.dataReport DESC")
    List<ReportVoluntario> findByOngAndStatusOrderByDataReportDesc(@Param("ong") Ong ong, @Param("status") ReportVoluntario.StatusReportVoluntario status);

    // Buscar reports por voluntário e status
    List<ReportVoluntario> findByVoluntarioAndStatus(Voluntario voluntario, ReportVoluntario.StatusReportVoluntario status);

    // Buscar reports por ONG e nome do voluntário
    @Query("SELECT r FROM ReportVoluntario r WHERE r.ong = :ong AND LOWER(r.voluntario.nomeVoluntario) LIKE LOWER(CONCAT('%', :nome, '%'))")
    Page<ReportVoluntario> findByOngAndVoluntarioNome(@Param("ong") Ong ong, @Param("nome") String nome, Pageable pageable);

    // Buscar reports pendentes ordenados por prioridade e data
    @Query("SELECT r FROM ReportVoluntario r WHERE r.status = 'PENDENTE' " +
            "ORDER BY CASE r.prioridade " +
            "WHEN 'URGENTE' THEN 1 " +
            "WHEN 'ALTA' THEN 2 " +
            "WHEN 'NORMAL' THEN 3 " +
            "WHEN 'BAIXA' THEN 4 " +
            "END, r.dataReport DESC")
    Page<ReportVoluntario> findReportsPendentesOrdenadosPorPrioridade(Pageable pageable);

    // Buscar reports com evidência
    @Query("SELECT r FROM ReportVoluntario r WHERE r.evidenciaUrl IS NOT NULL")
    List<ReportVoluntario> findReportsComEvidencia();

    // Buscar reports por período
    @Query("SELECT r FROM ReportVoluntario r WHERE r.dataReport BETWEEN :inicio AND :fim")
    List<ReportVoluntario> findByPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    // Contar reports por status
    long countByStatus(ReportVoluntario.StatusReportVoluntario status);

    // Contar reports pendentes por prioridade
    long countByStatusAndPrioridade(ReportVoluntario.StatusReportVoluntario status, ReportVoluntario.PrioridadeReportVoluntario prioridade);

    // Contar reports por ONG
    long countByOng(Ong ong);

    // Contar reports por voluntário
    long countByVoluntario(Voluntario voluntario);

    // Buscar report específico por ID e ONG (para segurança)
    Optional<ReportVoluntario> findByIdAndOng(Long id, Ong ong);

    // Query customizada para buscar reports com filtros múltiplos
    @Query("SELECT r FROM ReportVoluntario r " +
            "WHERE (:ong IS NULL OR r.ong = :ong) " +
            "AND (:voluntario IS NULL OR r.voluntario = :voluntario) " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:prioridade IS NULL OR r.prioridade = :prioridade) " +
            "AND (:assunto IS NULL OR r.assunto = :assunto) " +
            "ORDER BY r.dataReport DESC")
    Page<ReportVoluntario> findReportsComFiltros(
            @Param("ong") Ong ong,
            @Param("voluntario") Voluntario voluntario,
            @Param("status") ReportVoluntario.StatusReportVoluntario status,
            @Param("prioridade") ReportVoluntario.PrioridadeReportVoluntario prioridade,
            @Param("assunto") ReportVoluntario.AssuntoReportVoluntario assunto,
            Pageable pageable
    );

    // Buscar últimos reports de uma ONG
    @Query("SELECT r FROM ReportVoluntario r WHERE r.ong = :ong ORDER BY r.dataReport DESC")
    List<ReportVoluntario> findUltimosReportsPorOng(@Param("ong") Ong ong, Pageable pageable);

    // Verificar se voluntário tem reports críticos ou de alta prioridade
    @Query("SELECT COUNT(r) > 0 FROM ReportVoluntario r " +
            "WHERE r.voluntario = :voluntario " +
            "AND r.status != 'RESOLVIDO' " +
            "AND r.prioridade IN ('URGENTE', 'ALTA')")
    boolean voluntarioTemReportsCriticos(@Param("voluntario") Voluntario voluntario);

    // Estatísticas de reports por assunto para uma ONG
    @Query("SELECT r.assunto, COUNT(r) FROM ReportVoluntario r " +
            "WHERE r.ong = :ong " +
            "GROUP BY r.assunto")
    List<Object[]> contarReportsPorAssuntoEOng(@Param("ong") Ong ong);

    // Buscar reports não resolvidos de um voluntário
    @Query("SELECT r FROM ReportVoluntario r " +
            "WHERE r.voluntario = :voluntario " +
            "AND r.status != 'RESOLVIDO' " +
            "ORDER BY r.dataReport DESC")
    List<ReportVoluntario> findReportsNaoResolvidosPorVoluntario(@Param("voluntario") Voluntario voluntario);
}