package br.edu.ifs.web1.coletaseletiva.dto.indicador;

import br.edu.ifs.web1.coletaseletiva.model.SituacaoSolicitacao;

import io.swagger.v3.oas.annotations.media.Schema;

public record SolicitacoesPorSituacaoResponse(
        @Schema(example = "CONCLUIDA") SituacaoSolicitacao situacao,
        @Schema(description = "Número de solicitações na situação atual", example = "4") long total) {
}
