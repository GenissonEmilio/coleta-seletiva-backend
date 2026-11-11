package br.edu.ifs.web1.coletaseletiva.dto.solicitacao;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.annotation.JsonDeserialize;

public record SolicitacaoInicioRequest(
        @NotNull @PositiveOrZero @JsonDeserialize(using = VersaoSolicitacaoDeserializer.class)
        @Schema(example = "1") Long versao) {
}
