package br.edu.ifs.web1.coletaseletiva.dto.solicitacao;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.annotation.JsonDeserialize;

public record SolicitacaoAgendamentoRequest(
        @NotNull @PositiveOrZero @JsonDeserialize(using = VersaoSolicitacaoDeserializer.class)
        @Schema(example = "0") Long versao,
        @NotNull @Schema(example = "2026-12-01T14:30:00-03:00") OffsetDateTime agendadaPara) {
}
