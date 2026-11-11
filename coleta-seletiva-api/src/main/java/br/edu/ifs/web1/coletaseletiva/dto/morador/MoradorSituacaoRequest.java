package br.edu.ifs.web1.coletaseletiva.dto.morador;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

public record MoradorSituacaoRequest(
        @NotNull @Schema(description = "Falso para inativar e verdadeiro para reativar", example = "false")
        Boolean ativo) {
}
