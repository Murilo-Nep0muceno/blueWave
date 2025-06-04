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
@Table(name = "report_voluntario")
public class ReportVoluntario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "voluntario_id", nullable = false)
    private Voluntario voluntario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ong_id", nullable = false)
    private Ong ong;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vaga_id", nullable = true)
    private Vagas vaga; // Opcional - pode ser relacionado a uma vaga específica

    @Enumerated(EnumType.STRING)
    @Column(name = "assunto", nullable = false)
    private AssuntoReportVoluntario assunto;

    @Column(name = "motivo", nullable = false, length = 2000)
    private String motivo;

    @Column(name = "evidencia_url")
    private String evidenciaUrl;

    @Column(name = "evidencia_nome")
    private String evidenciaNome;

    @Column(name = "data_report", nullable = false)
    private LocalDateTime dataReport;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusReportVoluntario status = StatusReportVoluntario.PENDENTE;

    @Column(name = "resposta_admin", length = 1000)
    private String respostaAdmin;

    @Column(name = "data_resposta")
    private LocalDateTime dataResposta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_responsavel_id")
    private Voluntario adminResponsavel;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade")
    private PrioridadeReportVoluntario prioridade = PrioridadeReportVoluntario.NORMAL;

    // Enum para os assuntos possíveis do report de voluntário
    public enum AssuntoReportVoluntario {
        NAO_COMPARECEU("Não compareceu à atividade"),
        COMPORTAMENTO_INADEQUADO("Comportamento inadequado"),
        DESRESPEITO("Desrespeito a outros voluntários ou beneficiários"),
        NAO_SEGUIU_INSTRUCOES("Não seguiu instruções da ONG"),
        ABANDONO_ATIVIDADE("Abandonou a atividade sem aviso"),
        INFORMACOES_FALSAS("Forneceu informações falsas"),
        VIOLACAO_REGRAS("Violação das regras da ONG"),
        FALTA_COMPROMISSO("Falta de compromisso"),
        OUTROS("Outros motivos");

        private final String descricao;

        AssuntoReportVoluntario(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }

        public static AssuntoReportVoluntario fromString(String value) {
            if (value == null) return OUTROS;
            try {
                return valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return OUTROS;
            }
        }
    }

    // Enum para os status possíveis
    public enum StatusReportVoluntario {
        PENDENTE("Pendente"),
        ANALISANDO("Em Análise"),
        RESOLVIDO("Resolvido"),
        REJEITADO("Rejeitado"),
        ADVERTENCIA_ENVIADA("Advertência Enviada"),
        SUSPENSO("Voluntário Suspenso");

        private final String descricao;

        StatusReportVoluntario(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    // Enum para prioridade
    public enum PrioridadeReportVoluntario {
        BAIXA("Baixa"),
        NORMAL("Normal"),
        ALTA("Alta"),
        URGENTE("Urgente");

        private final String descricao;

        PrioridadeReportVoluntario(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    // Construtor para facilitar a criação
    public ReportVoluntario(Voluntario voluntario, Ong ong, AssuntoReportVoluntario assunto, String motivo) {
        this.voluntario = voluntario;
        this.ong = ong;
        this.assunto = assunto;
        this.motivo = motivo;
        this.dataReport = LocalDateTime.now();
        this.status = StatusReportVoluntario.PENDENTE;
        this.prioridade = PrioridadeReportVoluntario.NORMAL;
    }

    // Construtor completo
    public ReportVoluntario(Voluntario voluntario, Ong ong, Vagas vaga, AssuntoReportVoluntario assunto,
                            String motivo, String evidenciaUrl, String evidenciaNome) {
        this(voluntario, ong, assunto, motivo);
        this.vaga = vaga;
        this.evidenciaUrl = evidenciaUrl;
        this.evidenciaNome = evidenciaNome;
    }

    // Métodos utilitários
    public boolean temEvidencia() {
        return evidenciaUrl != null && !evidenciaUrl.trim().isEmpty();
    }

    public boolean isPendente() {
        return this.status == StatusReportVoluntario.PENDENTE;
    }

    public boolean isResolvido() {
        return this.status == StatusReportVoluntario.RESOLVIDO;
    }

    public long getDiasDesdeReport() {
        return java.time.Duration.between(dataReport, LocalDateTime.now()).toDays();
    }

    public void definirPrioridadeAutomatica() {
        switch (this.assunto) {
            case COMPORTAMENTO_INADEQUADO:
            case DESRESPEITO:
            case VIOLACAO_REGRAS:
                this.prioridade = PrioridadeReportVoluntario.ALTA;
                break;
            case NAO_COMPARECEU:
            case ABANDONO_ATIVIDADE:
            case FALTA_COMPROMISSO:
                this.prioridade = PrioridadeReportVoluntario.NORMAL;
                break;
            case INFORMACOES_FALSAS:
                this.prioridade = PrioridadeReportVoluntario.URGENTE;
                break;
            default:
                this.prioridade = PrioridadeReportVoluntario.BAIXA;
        }
    }

    public void resolver(String resposta, Voluntario admin) {
        this.status = StatusReportVoluntario.RESOLVIDO;
        this.respostaAdmin = resposta;
        this.dataResposta = LocalDateTime.now();
        this.adminResponsavel = admin;
    }

    public void rejeitar(String motivo, Voluntario admin) {
        this.status = StatusReportVoluntario.REJEITADO;
        this.respostaAdmin = motivo;
        this.dataResposta = LocalDateTime.now();
        this.adminResponsavel = admin;
    }

    @Override
    public String toString() {
        return "ReportVoluntario{" +
                "id=" + id +
                ", assunto=" + assunto +
                ", voluntario=" + (voluntario != null ? voluntario.getNomeVoluntario() : "null") +
                ", ong=" + (ong != null ? ong.getNome() : "null") +
                ", status=" + status +
                ", prioridade=" + prioridade +
                ", dataReport=" + dataReport +
                '}';
    }
}