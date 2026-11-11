package br.edu.ifs.web1.coletaseletiva.dto.indicador;

import java.math.BigDecimal;

import br.edu.ifs.web1.coletaseletiva.model.UnidadeMaterial;

import io.swagger.v3.oas.annotations.media.Schema;

public record RecolhimentoPorMaterialResponse(
        @Schema(example = "1") Long materialId,
        @Schema(example = "Vidro") String nomeMaterial,
        @Schema(description = "Unidade copiada no item da solicitação", example = "KG") UnidadeMaterial unidade,
        @Schema(description = "Soma das quantidades efetivas concluídas no período", example = "5.75")
        BigDecimal quantidadeEfetiva) {
}
