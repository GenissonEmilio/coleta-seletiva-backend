package br.edu.ifs.web1.coletaseletiva.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Chave composta de ponto_material: um material aparece no máximo uma vez em cada ponto. */
@Embeddable
public class PontoMaterialId implements Serializable {

    @Column(name = "ponto_id")
    private Long pontoId;

    @Column(name = "material_id")
    private Long materialId;

    protected PontoMaterialId() {
    }

    public PontoMaterialId(Long pontoId, Long materialId) {
        this.pontoId = pontoId;
        this.materialId = materialId;
    }

    public Long getPontoId() {
        return pontoId;
    }

    public Long getMaterialId() {
        return materialId;
    }

    @Override
    public boolean equals(Object outro) {
        return outro instanceof PontoMaterialId id
                && Objects.equals(pontoId, id.pontoId)
                && Objects.equals(materialId, id.materialId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pontoId, materialId);
    }
}
