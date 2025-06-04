package com.blueWave.BlueWave.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "report")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vaga_id", nullable = false)
    private Vagas vaga;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "voluntario_id", nullable = false)
    private Voluntario voluntario;

    @Enumerated(EnumType.STRING)
    @Column(name = "assunto", nullable = false)
    private AssuntoReport assunto;

    @Column(name = "motivo", nullable = false, length = 2000)
    private String motivo;

    @Column(name = "avaliacao")
    private Integer avaliacao; // 1 a 5 estrelas, opcional

    @Column(name = "evidencia_url")
    private String evidenciaUrl; // URL do arquivo de evidência

    @Column(name = "evidencia_nome")
    private String evidenciaNome; // Nome original do arquivo

    @Column(name = "data_report", nullable = false)
    private LocalDateTime dataReport;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusReport status = StatusReport.PENDENTE;

    @Column(name = "resposta_admin", length = 1000)
    private String respostaAdmin;

    @Column(name = "data_resposta")
    private LocalDateTime dataResposta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_responsavel_id")
    private Voluntario adminResponsavel; // Admin que analisou o report

    @Column(name = "prioridade")
    @Enumerated(EnumType.STRING)
    private PrioridadeReport prioridade = PrioridadeReport.NORMAL;

    // Enum para os assuntos possíveis do report
    public enum AssuntoReport {
        INFORMACOES_FALSAS("Informações falsas ou enganosas"),
        VAGA_INEXISTENTE("Vaga não existe ou já foi preenchida"),
        DISCRIMINACAO("Discriminação ou preconceito"),
        CONTEUDO_INADEQUADO("Conteúdo inadequado ou ofensivo"),
        EXPLORACAO("Exploração de voluntários"),
        ONG_SUSPEITA("ONG suspeita ou fraudulenta"),
        LOCAL_PERIGOSO("Local perigoso ou inadequado"),
        NAO_PAGAMENTO("Não cumprimento de promessas"),
        OUTROS("Outros motivos");

        private final String descricao;

        AssuntoReport(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }

        // Método para converter string para enum
        public static AssuntoReport fromString(String value) {
            if (value == null) return OUTROS;

            try {
                return valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return OUTROS;
            }
        }
    }

    // Enum para os status possíveis do report
    public enum StatusReport {
        PENDENTE("Pendente"),
        ANALISANDO("Em Análise"),
        RESOLVIDO("Resolvido"),
        REJEITADO("Rejeitado"),
        ARQUIVADO("Arquivado");

        private final String descricao;

        StatusReport(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    // Enum para prioridade
    public enum PrioridadeReport {
        BAIXA("Baixa"),
        NORMAL("Normal"),
        ALTA("Alta"),
        URGENTE("Urgente");

        private final String descricao;

        PrioridadeReport(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }

    // Construtor para facilitar a criação
    public Report(Vagas vaga, Voluntario voluntario, AssuntoReport assunto, String motivo) {
        this.vaga = vaga;
        this.voluntario = voluntario;
        this.assunto = assunto;
        this.motivo = motivo;
        this.dataReport = LocalDateTime.now();
        this.status = StatusReport.PENDENTE;
        this.prioridade = PrioridadeReport.NORMAL;
    }

    // Construtor completo
    public Report(Vagas vaga, Voluntario voluntario, AssuntoReport assunto, String motivo,
                  Integer avaliacao, String evidenciaUrl, String evidenciaNome) {
        this(vaga, voluntario, assunto, motivo);
        this.avaliacao = avaliacao;
        this.evidenciaUrl = evidenciaUrl;
        this.evidenciaNome = evidenciaNome;
    }

    // Métodos utilitários

    /**
     * Verifica se o report tem evidência anexada
     */
    public boolean temEvidencia() {
        return evidenciaUrl != null && !evidenciaUrl.trim().isEmpty();
    }

    /**
     * Verifica se o report tem avaliação
     */
    public boolean temAvaliacao() {
        return avaliacao != null && avaliacao >= 1 && avaliacao <= 5;
    }

    /**
     * Retorna a avaliação em formato de estrelas
     */
    public String getAvaliacaoEstrelas() {
        if (!temAvaliacao()) return "Não avaliado";

        StringBuilder stars = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            if (i <= avaliacao) {
                stars.append("★");
            } else {
                stars.append("☆");
            }
        }
        return stars.toString();
    }

    /**
     * Verifica se o report está pendente
     */
    public boolean isPendente() {
        return this.status == StatusReport.PENDENTE;
    }

    /**
     * Verifica se o report foi resolvido
     */
    public boolean isResolvido() {
        return this.status == StatusReport.RESOLVIDO;
    }

    /**
     * Verifica se o report foi rejeitado
     */
    public boolean isRejeitado() {
        return this.status == StatusReport.REJEITADO;
    }

    /**
     * Calcula quantos dias se passaram desde o report
     */
    public long getDiasDesdeReport() {
        return java.time.Duration.between(dataReport, LocalDateTime.now()).toDays();
    }

    /**
     * Determina automaticamente a prioridade baseada no assunto
     */
    public void definirPrioridadeAutomatica() {
        switch (this.assunto) {
            case DISCRIMINACAO:
            case ONG_SUSPEITA:
            case LOCAL_PERIGOSO:
                this.prioridade = PrioridadeReport.ALTA;
                break;
            case EXPLORACAO:
            case CONTEUDO_INADEQUADO:
                this.prioridade = PrioridadeReport.NORMAL;
                break;
            case INFORMACOES_FALSAS:
            case VAGA_INEXISTENTE:
                if (temEvidencia()) {
                    this.prioridade = PrioridadeReport.NORMAL;
                } else {
                    this.prioridade = PrioridadeReport.BAIXA;
                }
                break;
            default:
                this.prioridade = PrioridadeReport.BAIXA;
        }

        // Se tem avaliação muito baixa (1-2 estrelas), aumenta prioridade
        if (temAvaliacao() && avaliacao <= 2 && prioridade == PrioridadeReport.BAIXA) {
            this.prioridade = PrioridadeReport.NORMAL;
        }
    }

    /**
     * Marca como resolvido
     */
    public void resolver(String resposta, Voluntario admin) {
        this.status = StatusReport.RESOLVIDO;
        this.respostaAdmin = resposta;
        this.dataResposta = LocalDateTime.now();
        this.adminResponsavel = admin;
    }

    /**
     * Marca como rejeitado
     */
    public void rejeitar(String motivo, Voluntario admin) {
        this.status = StatusReport.REJEITADO;
        this.respostaAdmin = motivo;
        this.dataResposta = LocalDateTime.now();
        this.adminResponsavel = admin;
    }

    @Override
    public String toString() {
        return "Report{" +
                "id=" + id +
                ", assunto=" + assunto +
                ", vaga=" + (vaga != null ? vaga.getNome() : "null") +
                ", voluntario=" + (voluntario != null ? voluntario.getNomeVoluntario() : "null") +
                ", status=" + status +
                ", prioridade=" + prioridade +
                ", dataReport=" + dataReport +
                '}';
    }
}