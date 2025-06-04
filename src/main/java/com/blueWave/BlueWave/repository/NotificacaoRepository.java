package com.blueWave.BlueWave.repository;

import com.blueWave.BlueWave.model.Notificacao;
import com.blueWave.BlueWave.model.Voluntario;
import com.blueWave.BlueWave.model.Ong;
import com.blueWave.BlueWave.model.ReportVoluntario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    // Buscar notificações por voluntário (usando dataEnvio conforme modelo atual)
    List<Notificacao> findByVoluntarioOrderByDataEnvioDesc(Voluntario voluntario);
    Page<Notificacao> findByVoluntarioOrderByDataEnvioDesc(Voluntario voluntario, Pageable pageable);

    // Buscar notificações não lidas por voluntário
    List<Notificacao> findByVoluntarioAndLidaFalseOrderByDataEnvioDesc(Voluntario voluntario);

    // Buscar notificações por ONG
    List<Notificacao> findByOngOrderByDataEnvioDesc(Ong ong);

    // Buscar notificações por tipo
    List<Notificacao> findByTipoOrderByDataEnvioDesc(Notificacao.TipoNotificacao tipo);

    // Buscar notificações por prioridade
    List<Notificacao> findByPrioridadeOrderByDataEnvioDesc(Notificacao.PrioridadeNotificacao prioridade);

    // Buscar notificações relacionadas a um report
    List<Notificacao> findByReportVoluntarioOrderByDataEnvioDesc(ReportVoluntario reportVoluntario);

    // Contar notificações não lidas por voluntário
    long countByVoluntarioAndLidaFalse(Voluntario voluntario);

    // Contar notificações por ONG
    long countByOng(Ong ong);

    // Buscar notificações recentes por voluntário (últimas 24h)
    @Query("SELECT n FROM Notificacao n WHERE n.voluntario = :voluntario AND n.dataEnvio >= :dataInicio ORDER BY n.dataEnvio DESC")
    List<Notificacao> findNotificacoesRecentes(@Param("voluntario") Voluntario voluntario, @Param("dataInicio") LocalDateTime dataInicio);

    // Buscar notificações urgentes não lidas por voluntário
    @Query("SELECT n FROM Notificacao n WHERE n.voluntario = :voluntario AND n.prioridade = 'URGENTE' AND n.lida = false ORDER BY n.dataEnvio ASC")
    List<Notificacao> findNotificacaoUrgentesNaoLidas(@Param("voluntario") Voluntario voluntario);

    // Buscar notificações por voluntário e tipo
    List<Notificacao> findByVoluntarioAndTipoOrderByDataEnvioDesc(Voluntario voluntario, Notificacao.TipoNotificacao tipo);

    // Buscar notificações por range de data
    @Query("SELECT n FROM Notificacao n WHERE n.dataEnvio BETWEEN :dataInicio AND :dataFim ORDER BY n.dataEnvio DESC")
    List<Notificacao> findByDataEnvioBetween(@Param("dataInicio") LocalDateTime dataInicio,
                                             @Param("dataFim") LocalDateTime dataFim);

    // Buscar notificações urgentes não lidas (geral)
    @Query("SELECT n FROM Notificacao n WHERE n.prioridade = 'URGENTE' AND n.lida = false ORDER BY n.dataEnvio ASC")
    List<Notificacao> findNotificacoesUrgentesNaoLidas();
    Page<Notificacao> findByVoluntarioAndLidaFalseOrderByDataEnvioDesc(Voluntario voluntario, Pageable pageable);
    // Estatísticas para dashboard
    @Query("SELECT COUNT(n) FROM Notificacao n WHERE n.lida = false")
    long countNotificacoesNaoLidas();

    @Query("SELECT n.tipo, COUNT(n) FROM Notificacao n GROUP BY n.tipo ORDER BY COUNT(n) DESC")
    List<Object[]> getEstatisticasPorTipo();

    // Limpeza de notificações antigas (mais de X dias)
    @Modifying
    @Query("DELETE FROM Notificacao n WHERE n.dataEnvio < :dataLimite AND n.lida = true")
    void limparNotificacoesAntigas(@Param("dataLimite") LocalDateTime dataLimite);
}