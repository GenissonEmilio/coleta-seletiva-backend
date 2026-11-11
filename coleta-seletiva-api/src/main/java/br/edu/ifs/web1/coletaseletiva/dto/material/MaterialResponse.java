package br.edu.ifs.web1.coletaseletiva.dto.material;

import br.edu.ifs.web1.coletaseletiva.model.UnidadeMaterial;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Material cadastrado, incluindo sua situação atual")
public record MaterialResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Papel") String nome,
        @Schema(example = "Papel e papelão secos") String descricao,
        @Schema(example = "KG") UnidadeMaterial unidade,
        @Schema(example = "Separar limpo e seco") String orientacaoPreparo,
        @Schema(description = "Falso após a inativação; o registro e seus vínculos são preservados", example = "true")
        boolean ativo) {
}
