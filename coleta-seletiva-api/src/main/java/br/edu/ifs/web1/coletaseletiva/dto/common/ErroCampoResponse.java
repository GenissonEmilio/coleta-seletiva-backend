package br.edu.ifs.web1.coletaseletiva.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Erro associado a um campo da entrada")
public record ErroCampoResponse(
        @Schema(example = "itens[0].quantidadeEstimada") String campo,
        @Schema(example = "deve ser maior que zero") String mensagem) {
}
