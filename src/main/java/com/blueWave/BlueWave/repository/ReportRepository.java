package com.blueWave.BlueWave.repository;

import com.blueWave.BlueWave.model.Report;
import com.blueWave.BlueWave.model.Vagas;
import com.blueWave.BlueWave.model.Voluntario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    // Buscar reports por voluntário
    @Query("SELECT r FROM Report r JOIN FETCH r.vaga v JOIN FETCH v.ong WHERE r.voluntario = :voluntario ORDER BY r.dataReport DESC")
    List<Report> findByVoluntario(@Param("voluntario") Voluntario voluntario);

    // Buscar reports por vaga
    @Query("SELECT r FROM Report r JOIN FETCH r.voluntario WHERE r.vaga = :vaga ORDER BY r.dataReport DESC")
    List<Report> findByVaga(@Param("vaga") Vagas vaga);

    // Buscar reports por status
    @Query("SELECT r FROM Report r JOIN FETCH r.vaga v JOIN FETCH v.ong JOIN FETCH r.voluntario WHERE r.status = :status ORDER BY r.dataReport DESC")
    Page<Report> findByStatus(@Param("status") Report.StatusReport status, Pageable pageable);

    // Buscar todos os reports com paginação para admin
    @Query("SELECT r FROM Report r JOIN FETCH r.vaga v JOIN FETCH v.ong JOIN FETCH r.voluntario ORDER BY r.dataReport DESC")
    Page<Report> findAllWithDetails(Pageable pageable);

    // Buscar reports pendentes
    @Query("SELECT r FROM Report r JOIN FETCH r.vaga v JOIN FETCH v.ong JOIN FETCH r.voluntario WHERE r.status = 'PENDENTE' ORDER BY r.dataReport ASC")
    List<Report> findPendingReports();

    // Contar reports por status
    long countByStatus(Report.StatusReport status);

    // Contar reports por vaga
    long countByVaga(Vagas vaga);

    // Buscar reports por data
    @Query("SELECT r FROM Report r JOIN FETCH r.vaga v JOIN FETCH v.ong JOIN FETCH r.voluntario WHERE r.dataReport BETWEEN :inicio AND :fim ORDER BY r.dataReport DESC")
    List<Report> findByDataReportBetween(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    // Verificar se voluntário já reportou a vaga
    boolean existsByVoluntarioAndVaga(Voluntario voluntario, Vagas vaga);

    // Buscar reports de uma ONG específica
    @Query("SELECT r FROM Report r JOIN FETCH r.vaga v JOIN FETCH r.voluntario WHERE v.ong.id = :ongId ORDER BY r.dataReport DESC")
    List<Report> findByOngId(@Param("ongId") Long ongId);

    // Estatísticas de reports
    @Query("SELECT COUNT(r) FROM Report r WHERE r.dataReport >= :dataInicio")
    long countReportsFromDate(@Param("dataInicio") LocalDate dataInicio);

    // Reports mais recentes
    @Query("SELECT r FROM Report r JOIN FETCH r.vaga v JOIN FETCH v.ong JOIN FETCH r.voluntario ORDER BY r.dataReport DESC")
    List<Report> findRecentReports(Pageable pageable);
}