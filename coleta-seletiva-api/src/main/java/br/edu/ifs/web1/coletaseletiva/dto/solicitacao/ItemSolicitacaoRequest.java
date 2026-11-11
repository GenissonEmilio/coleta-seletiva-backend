package br.edu.ifs.web1.coletaseletiva.dto.solicitacao;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Material e quantidade estimada de um item da solicitação")
public record ItemSolicitacaoRequest(
        @NotNull @Schema(example = "2") Long materialId,
        @NotNull @Schema(description = "Maior que zero, na unidade do material; inteira para UNIDADE",
                example = "2.5") BigDecimal quantidadeEstimada) {
}
