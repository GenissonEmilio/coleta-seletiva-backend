package br.edu.ifs.web1.coletaseletiva.dto.ponto;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Inativa ou reativa um ponto de coleta, sem excluir registros")
public record PontoSituacaoRequest(
        @NotNull @Schema(description = "Falso para inativar; verdadeiro para reativar", example = "false") Boolean ativo) {
}
