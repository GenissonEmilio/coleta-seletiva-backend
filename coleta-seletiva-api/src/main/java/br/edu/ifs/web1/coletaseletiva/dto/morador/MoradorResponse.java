package br.edu.ifs.web1.coletaseletiva.dto.morador;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Morador cadastrado")
public record MoradorResponse(
        @Schema(description = "Identificador do morador", example = "1") Long id,
        @Schema(description = "Nome do morador", example = "Maria Souza") String nome,
        @Schema(description = "E-mail do morador, já normalizado", example = "maria@exemplo.com") String email,
        @Schema(description = "Telefone, quando informado", example = "79999990000") String telefone,
        @Schema(description = "Indica se o morador está ativo; moradores são inativados, não excluídos",
                example = "true") boolean ativo) {
}
