package br.edu.ifs.web1.coletaseletiva.dto.solicitacao;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.annotation.JsonDeserialize;

/** Corpo comum do reagendamento e do registro de tentativa sem sucesso. */
public record SolicitacaoReagendamentoRequest(
        @NotNull @PositiveOrZero @JsonDeserialize(using = VersaoSolicitacaoDeserializer.class)
        @Schema(example = "2") Long versao,
        @NotNull @Schema(example = "2026-12-02T14:30:00-03:00") OffsetDateTime agendadaPara,
        @NotBlank @Schema(example = "Portão fechado na tentativa de recolhimento") String motivo) {
}
