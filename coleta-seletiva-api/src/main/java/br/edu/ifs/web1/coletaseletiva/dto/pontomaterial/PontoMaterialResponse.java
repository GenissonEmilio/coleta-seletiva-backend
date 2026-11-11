package br.edu.ifs.web1.coletaseletiva.dto.pontomaterial;

import java.math.BigDecimal;

import br.edu.ifs.web1.coletaseletiva.model.UnidadeMaterial;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Material aceito por um ponto, com os dados que o formulário de solicitação precisa")
public record PontoMaterialResponse(
        @Schema(example = "1") Long pontoId,
        @Schema(example = "2") Long materialId,
        @Schema(example = "Vidro") String nomeMaterial,
        @Schema(example = "KG") UnidadeMaterial unidade,
        @Schema(example = "Lavar e separar por cor") String orientacaoPreparo,
        @Schema(example = "Somente garrafas") String observacoes,
        @Schema(description = "Nulo quando não há limite", example = "10.5") BigDecimal limitePorSolicitacao) {
}
