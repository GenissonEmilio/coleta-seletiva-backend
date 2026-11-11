package br.edu.ifs.web1.coletaseletiva.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "item_solicitacao")
public class ItemSolicitacaoModel {

    @EmbeddedId
    private ItemSolicitacaoId id;

    @MapsId("solicitacaoId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private SolicitacaoModel solicitacao;

    @MapsId("materialId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private MaterialModel material;

    /** Cópia da unidade do material no momento da solicitação (RN04). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UnidadeMaterial unidade;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantidadeEstimada;

    @Column(precision = 12, scale = 3)
    private BigDecimal quantidadeEfetiva;

    protected ItemSolicitacaoModel() {
    }

    /** A solicitação já precisa ter id (ter sido salva) para montar a chave. */
    public ItemSolicitacaoModel(SolicitacaoModel solicitacao, MaterialModel material, BigDecimal quantidadeEstimada) {
        this.id = new ItemSolicitacaoId(solicitacao.getId(), material.getId());
        this.solicitacao = solicitacao;
        this.material = material;
        this.unidade = material.getUnidade();
        this.quantidadeEstimada = quantidadeEstimada;
    }

    public ItemSolicitacaoId getId() {
        return id;
    }

    public MaterialModel getMaterial() {
        return material;
    }

    public UnidadeMaterial getUnidade() {
        return unidade;
    }

    public BigDecimal getQuantidadeEstimada() {
        return quantidadeEstimada;
    }

    public BigDecimal getQuantidadeEfetiva() {
        return quantidadeEfetiva;
    }

    public void setQuantidadeEfetiva(BigDecimal quantidadeEfetiva) {
        this.quantidadeEfetiva = quantidadeEfetiva;
    }
}
