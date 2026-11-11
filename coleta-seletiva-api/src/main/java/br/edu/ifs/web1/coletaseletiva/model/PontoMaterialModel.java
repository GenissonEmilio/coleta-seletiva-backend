package br.edu.ifs.web1.coletaseletiva.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "ponto_material")
public class PontoMaterialModel {

    @EmbeddedId
    private PontoMaterialId id;

    @MapsId("pontoId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ponto_id", nullable = false)
    private PontoModel ponto;

    @MapsId("materialId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private MaterialModel material;

    @Column(columnDefinition = "text")
    private String observacoes;

    @Column(precision = 12, scale = 3)
    private BigDecimal limitePorSolicitacao;

    protected PontoMaterialModel() {
    }

    public PontoMaterialModel(PontoModel ponto, MaterialModel material) {
        this.id = new PontoMaterialId(ponto.getId(), material.getId());
        this.ponto = ponto;
        this.material = material;
    }

    public PontoMaterialId getId() {
        return id;
    }

    public MaterialModel getMaterial() {
        return material;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public BigDecimal getLimitePorSolicitacao() {
        return limitePorSolicitacao;
    }

    public void setLimitePorSolicitacao(BigDecimal limitePorSolicitacao) {
        this.limitePorSolicitacao = limitePorSolicitacao;
    }
}
