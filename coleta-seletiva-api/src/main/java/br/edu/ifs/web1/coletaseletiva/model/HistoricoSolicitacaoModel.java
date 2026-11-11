package br.edu.ifs.web1.coletaseletiva.model;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Uma linha por alteração da solicitação (RN03): situação anterior e nova, data, responsável e motivo. */
@Entity
@Table(name = "historico_solicitacao")
public class HistoricoSolicitacaoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private SolicitacaoModel solicitacao;

    /** Nulo só na criação. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SituacaoSolicitacao situacaoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoSolicitacao situacaoNova;

    @Column(nullable = false)
    private OffsetDateTime ocorridoEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ResponsavelTipo responsavelTipo;

    /** Id do morador ou do operador; sem chave estrangeira porque depende do tipo. */
    private Long responsavelId;

    @Column(columnDefinition = "text")
    private String motivo;

    private OffsetDateTime agendamentoNovo;

    protected HistoricoSolicitacaoModel() {
    }

    public HistoricoSolicitacaoModel(SolicitacaoModel solicitacao, SituacaoSolicitacao situacaoAnterior,
            SituacaoSolicitacao situacaoNova, OffsetDateTime ocorridoEm, ResponsavelTipo responsavelTipo,
            Long responsavelId, String motivo) {
        this.solicitacao = solicitacao;
        this.situacaoAnterior = situacaoAnterior;
        this.situacaoNova = situacaoNova;
        this.ocorridoEm = ocorridoEm;
        this.responsavelTipo = responsavelTipo;
        this.responsavelId = responsavelId;
        this.motivo = motivo;
    }

    public Long getId() {
        return id;
    }

    public SituacaoSolicitacao getSituacaoAnterior() {
        return situacaoAnterior;
    }

    public SituacaoSolicitacao getSituacaoNova() {
        return situacaoNova;
    }

    public OffsetDateTime getOcorridoEm() {
        return ocorridoEm;
    }

    public ResponsavelTipo getResponsavelTipo() {
        return responsavelTipo;
    }

    public Long getResponsavelId() {
        return responsavelId;
    }

    public String getMotivo() {
        return motivo;
    }

    public OffsetDateTime getAgendamentoNovo() {
        return agendamentoNovo;
    }

    public void setAgendamentoNovo(OffsetDateTime agendamentoNovo) {
        this.agendamentoNovo = agendamentoNovo;
    }
}
