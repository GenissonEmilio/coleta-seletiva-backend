package br.edu.ifs.web1.coletaseletiva.dto.solicitacao;

import java.time.OffsetDateTime;

import br.edu.ifs.web1.coletaseletiva.model.SituacaoSolicitacao;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Linha da lista de solicitações; itens, endereço e histórico ficam no detalhe")
public record SolicitacaoResumoResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "CS-20261111-K7P3QX") String protocolo,
        @Schema(example = "AGENDADA") SituacaoSolicitacao situacao,
        @Schema(example = "2026-11-11T17:30:00Z") OffsetDateTime criadaEm,
        @Schema(description = "Nulo até o agendamento", example = "2026-11-15T12:00:00Z") OffsetDateTime agendadaPara,
        @Schema(description = "Nulo até a conclusão") OffsetDateTime concluidaEm,
        @Schema(example = "1") Long moradorId,
        @Schema(example = "1") Long pontoId,
        @Schema(example = "Ecoponto Centro") String nomePonto) {
}
