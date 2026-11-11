package br.edu.ifs.web1.coletaseletiva.dto.solicitacao;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para criar uma solicitação. O morador vem do contexto (X-Ator-Tipo e X-Ator-Id)")
public record SolicitacaoRequest(
        @NotNull @Schema(description = "Endereço do próprio morador", example = "1") Long enderecoId,
        @NotNull @Schema(description = "Ponto ativo com recolhimento domiciliar", example = "1") Long pontoId,
        @Schema(example = "Portão azul, tocar a campainha") String observacoes,
        @NotEmpty @Valid @Schema(description = "Pelo menos um item; cada material aparece uma vez só")
        List<@NotNull ItemSolicitacaoRequest> itens) {
}
