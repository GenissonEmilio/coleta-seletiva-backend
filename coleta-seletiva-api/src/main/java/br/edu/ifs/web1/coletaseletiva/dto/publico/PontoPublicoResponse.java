package br.edu.ifs.web1.coletaseletiva.dto.publico;

import io.swagger.v3.oas.annotations.media.Schema;

/** Ponto ativo na consulta pública. Só traz dados do próprio ponto, nunca de moradores ou operadores. */
@Schema(description = "Ponto de coleta ativo, como aparece na consulta pública")
public record PontoPublicoResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Ecoponto Centro") String nome,
        @Schema(example = "Rua das Flores") String logradouro,
        @Schema(description = "Nulo quando semNumero é verdadeiro", example = "120") String numero,
        @Schema(example = "false") boolean semNumero,
        @Schema(example = "Centro") String bairro,
        @Schema(example = "Cidade Teste") String municipio,
        @Schema(example = "SE") String uf,
        @Schema(example = "49000000") String cep,
        @Schema(example = "Galpão 2") String complemento,
        @Schema(description = "Contato público do ponto", example = "ecoponto@exemplo.org") String contato,
        @Schema(example = "Seg a sex, 8h às 17h") String horarios,
        @Schema(description = "Falso: o ponto recebe materiais, mas não oferece coleta em casa", example = "true")
        boolean recolhimentoDomiciliar) {
}
