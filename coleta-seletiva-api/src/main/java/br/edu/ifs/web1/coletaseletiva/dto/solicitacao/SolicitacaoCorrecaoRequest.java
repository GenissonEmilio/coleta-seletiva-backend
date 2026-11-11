package br.edu.ifs.web1.coletaseletiva.dto.solicitacao;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Correção de endereço e itens, permitida só em SOLICITADA. A lista substitui todos os itens")
public record SolicitacaoCorrecaoRequest(
        @NotNull @Schema(description = "Versão que o cliente leu; se estiver desatualizada, 409 CONFLITO_VERSAO",
                example = "0") Long versao,
        @NotNull @Schema(description = "Endereço do próprio morador", example = "1") Long enderecoId,
        @NotEmpty @Valid @Schema(description = "Nova lista completa de itens")
        List<@NotNull ItemSolicitacaoRequest> itens) {
}
