package br.edu.ifs.web1.coletaseletiva.dto.common;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import br.edu.ifs.web1.coletaseletiva.exception.CodigoErro;

/** Formato único de erro da API. Nunca carrega stack trace, SQL ou credenciais. */
@Schema(description = "Formato padrão de erro")
public record ErroResponse(
        @Schema(example = "ENTRADA_INVALIDA") String codigo,
        @Schema(example = "Revise os campos informados.") String mensagem,
        List<ErroCampoResponse> errosCampos) {

    public static ErroResponse de(CodigoErro codigo, String mensagem) {
        return new ErroResponse(codigo.name(), mensagem, List.of());
    }

    public static ErroResponse de(CodigoErro codigo, String mensagem, List<ErroCampoResponse> erros) {
        return new ErroResponse(codigo.name(), mensagem, erros);
    }
}
