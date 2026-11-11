package br.edu.ifs.web1.coletaseletiva.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Chave composta de item_solicitacao: um material aparece no máximo uma vez em cada solicitação (RN01). */
@Embeddable
public class ItemSolicitacaoId implements Serializable {

    @Column(name = "solicitacao_id")
    private Long solicitacaoId;

    @Column(name = "material_id")
    private Long materialId;

    protected ItemSolicitacaoId() {
    }

    public ItemSolicitacaoId(Long solicitacaoId, Long materialId) {
        this.solicitacaoId = solicitacaoId;
        this.materialId = materialId;
    }

    public Long getSolicitacaoId() {
        return solicitacaoId;
    }

    public Long getMaterialId() {
        return materialId;
    }

    @Override
    public boolean equals(Object outro) {
        return outro instanceof ItemSolicitacaoId id
                && Objects.equals(solicitacaoId, id.solicitacaoId)
                && Objects.equals(materialId, id.materialId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(solicitacaoId, materialId);
    }
}
