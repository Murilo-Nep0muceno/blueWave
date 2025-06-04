package com.blueWave.BlueWave.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "notificacao")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "voluntario_id", nullable = false)
    private Voluntario voluntario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ong_id", nullable = false)
    private Ong ong;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaga_id")
    private Vagas vaga; // Opcional - relacionado a uma vaga específica

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_voluntario_id") // Corrigido para ser mais específico
    private ReportVoluntario reportVoluntario; // Opcional - relacionado a um report

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoNotificacao tipo;

    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    @Column(name = "mensagem", nullable = false, length = 2000)
    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade", nullable = false)
    private PrioridadeNotificacao prioridade = PrioridadeNotificacao.NORMAL;

    @Column(name = "data_envio", nullable = false)
    private LocalDateTime dataEnvio;

    @Column(name = "lida", nullable = false)
    private boolean lida = false;

    @Column(name = "data_leitura")
    private LocalDateTime dataLeitura;

    @Column(name = "url_acao")
    private String urlAcao; // URL para ação relacionada à notificação

    @Column(name = "dados_extras", length = 1000)
    private String dadosExtras; // JSON com dados adicionais se necessário

    // Enum para tipos de notificação
    public enum TipoNotificacao {
        INFORMACAO_GERAL("Informação Geral"),
        LEMBRETE("Lembrete"),
        ADVERTENCIA("Advertência"),
        PARABENIZACAO("Parabéns"),
        VAGA_ATUALIZADA("Vaga Atualizada"),
        VAGA_CANCELADA("Vaga Cancelada"),
        INSCRICAO_CONFIRMADA("Inscrição Confirmada"),
        INSCRICAO_CANCELADA("Inscrição Cancelada"),
        REPORT_RECEBIDO("Report Recebido"),
        RESPOSTA_REPORT("Resposta do Report"),
        SISTEMA("Sistema"),
        URGENTE("Urgente");

        private final String descricao;

        TipoNotificacao(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    // Enum para prioridade
    public enum PrioridadeNotificacao {
        BAIXA("Baixa"),
        NORMAL("Normal"),
        ALTA("Alta"),
        URGENTE("Urgente");

        private final String descricao;

        PrioridadeNotificacao(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    // Construtor básico para notificação simples
    public Notificacao(Voluntario voluntario, Ong ong, TipoNotificacao tipo,
                       String titulo, String mensagem, PrioridadeNotificacao prioridade) {
        this.voluntario = voluntario;
        this.ong = ong;
        this.tipo = tipo;
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.prioridade = prioridade;
        this.dataEnvio = LocalDateTime.now();
        this.lida = false;
    }

    // Construtor para notificação relacionada a report
    public Notificacao(Voluntario voluntario, Ong ong, TipoNotificacao tipo,
                       String titulo, String mensagem, ReportVoluntario reportVoluntario,
                       PrioridadeNotificacao prioridade) {
        this(voluntario, ong, tipo, titulo, mensagem, prioridade);
        this.reportVoluntario = reportVoluntario;
    }

    // Construtor para notificação relacionada a vaga
    public Notificacao(Voluntario voluntario, Ong ong, TipoNotificacao tipo,
                       String titulo, String mensagem, Vagas vaga,
                       PrioridadeNotificacao prioridade) {
        this(voluntario, ong, tipo, titulo, mensagem, prioridade);
        this.vaga = vaga;
    }

    // Métodos utilitários
    public boolean isLida() {
        return lida;
    }

    public boolean isNaoLida() {
        return !lida;
    }

    public boolean isUrgente() {
        return this.prioridade == PrioridadeNotificacao.URGENTE;
    }

    public boolean isRecente() {
        return this.dataEnvio.isAfter(LocalDateTime.now().minusHours(24));
    }

    public void marcarComoLida() {
        this.lida = true;
        this.dataLeitura = LocalDateTime.now();
    }

    public long getMinutosDesdeEnvio() {
        return java.time.Duration.between(dataEnvio, LocalDateTime.now()).toMinutes();
    }

    public long getHorasDesdeEnvio() {
        return java.time.Duration.between(dataEnvio, LocalDateTime.now()).toHours();
    }

    public long getDiasDesdeEnvio() {
        return java.time.Duration.between(dataEnvio, LocalDateTime.now()).toDays();
    }

    // Método para gerar URL de ação automática baseada no tipo
    public void gerarUrlAcao() {
        switch (this.tipo) {
            case VAGA_ATUALIZADA:
            case VAGA_CANCELADA:
            case INSCRICAO_CONFIRMADA:
            case INSCRICAO_CANCELADA:
                if (this.vaga != null) {
                    this.urlAcao = "/inscricao/detalhes/" + this.vaga.getId();
                }
                break;
            case REPORT_RECEBIDO:
            case RESPOSTA_REPORT:
                if (this.reportVoluntario != null) {
                    this.urlAcao = "/voluntario/reports/" + this.reportVoluntario.getId();
                }
                break;
            case LEMBRETE:
                this.urlAcao = "/inscricao/minhasVagas";
                break;
            default:
                this.urlAcao = "/voluntario/notificacoes";
        }
    }

    // Método para definir prioridade automaticamente baseada no tipo
    public void definirPrioridadeAutomatica() {
        switch (this.tipo) {
            case REPORT_RECEBIDO:
            case ADVERTENCIA:
            case URGENTE:
                this.prioridade = PrioridadeNotificacao.URGENTE;
                break;
            case VAGA_CANCELADA:
            case RESPOSTA_REPORT:
                this.prioridade = PrioridadeNotificacao.ALTA;
                break;
            case LEMBRETE:
            case VAGA_ATUALIZADA:
            case INSCRICAO_CONFIRMADA:
                this.prioridade = PrioridadeNotificacao.NORMAL;
                break;
            default:
                this.prioridade = PrioridadeNotificacao.BAIXA;
        }
    }

    @Override
    public String toString() {
        return "Notificacao{" +
                "id=" + id +
                ", tipo=" + tipo +
                ", titulo='" + titulo + '\'' +
                ", voluntario=" + (voluntario != null ? voluntario.getNomeVoluntario() : "null") +
                ", ong=" + (ong != null ? ong.getNome() : "null") +
                ", prioridade=" + prioridade +
                ", lida=" + lida +
                ", dataEnvio=" + dataEnvio +
                '}';
    }
}