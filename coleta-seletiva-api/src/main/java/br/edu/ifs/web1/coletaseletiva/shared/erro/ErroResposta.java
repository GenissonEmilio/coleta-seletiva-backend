package br.edu.ifs.web1.coletaseletiva.shared.erro;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** Formato único de erro da API. Nunca carrega stack trace, SQL ou credenciais. */
@Schema(description = "Formato padrão de erro")
public record ErroResposta(
        @Schema(example = "ENTRADA_INVALIDA") String codigo,
        @Schema(example = "Revise os campos informados.") String mensagem,
        List<ErroCampo> errosCampos) {

    public static ErroResposta de(CodigoErro codigo, String mensagem) {
        return new ErroResposta(codigo.name(), mensagem, List.of());
    }

    public static ErroResposta de(CodigoErro codigo, String mensagem, List<ErroCampo> erros) {
        return new ErroResposta(codigo.name(), mensagem, erros);
    }
}
