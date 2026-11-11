package br.edu.ifs.web1.coletaseletiva.dto.solicitacao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.annotation.JsonDeserialize;

/** Corpo comum de recusa e cancelamento. */
public record SolicitacaoEncerramentoRequest(
        @NotNull @PositiveOrZero @JsonDeserialize(using = VersaoSolicitacaoDeserializer.class)
        @Schema(example = "0") Long versao,
        @NotBlank @Schema(example = "Solicitação duplicada") String motivo) {
}
