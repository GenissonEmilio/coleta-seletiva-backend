package br.edu.ifs.web1.coletaseletiva.dto.solicitacao;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.annotation.JsonDeserialize;

public record SolicitacaoConclusaoRequest(
        @NotNull @PositiveOrZero @JsonDeserialize(using = VersaoSolicitacaoDeserializer.class)
        @Schema(example = "2") Long versao,
        @NotEmpty @Valid @Schema(description = "Quantidade efetiva de cada material da solicitação")
        List<@NotNull ItemRequest> itens) {

    public record ItemRequest(
            @NotNull @Positive @Schema(example = "1") Long materialId,
            @NotNull @Schema(description = "Pode ser zero quando o item não foi recolhido", example = "2.5")
            BigDecimal quantidadeEfetiva) {
    }
}
