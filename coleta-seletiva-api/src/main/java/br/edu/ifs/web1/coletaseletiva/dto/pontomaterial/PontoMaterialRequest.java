package br.edu.ifs.web1.coletaseletiva.dto.pontomaterial;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Observações e limite opcional de um material aceito pelo ponto")
public record PontoMaterialRequest(
        @Schema(description = "Observações opcionais para quem vai separar o material", example = "Somente garrafas")
        String observacoes,
        @Schema(description = "Limite opcional por solicitação, na unidade do material; maior que zero e inteiro "
                + "para UNIDADE. Ausente significa sem limite", example = "10.5")
        BigDecimal limitePorSolicitacao) {
}
