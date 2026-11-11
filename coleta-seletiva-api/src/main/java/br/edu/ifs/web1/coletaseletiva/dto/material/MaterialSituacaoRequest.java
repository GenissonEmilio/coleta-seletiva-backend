package br.edu.ifs.web1.coletaseletiva.dto.material;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Inativa ou reativa um material, sem excluir registros ou vínculos")
public record MaterialSituacaoRequest(
        @NotNull @Schema(description = "Falso para inativar; verdadeiro para reativar", example = "false") Boolean ativo) {
}
