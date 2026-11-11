package br.edu.ifs.web1.coletaseletiva.exception;

import org.springframework.http.HttpStatus;

/**
 * Códigos estáveis de erro devolvidos pela API.
 * O frontend usa o código para decidir o que mostrar.
 */
public enum CodigoErro {

    ENTRADA_INVALIDA(HttpStatus.BAD_REQUEST),
    REGISTRO_NAO_ENCONTRADO(HttpStatus.NOT_FOUND),
    ROTA_NAO_ENCONTRADA(HttpStatus.NOT_FOUND),
    METODO_NAO_PERMITIDO(HttpStatus.METHOD_NOT_ALLOWED),
    TIPO_NAO_SUPORTADO(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    ACESSO_NEGADO(HttpStatus.FORBIDDEN),

    // Conflitos (409)
    DUPLICIDADE(HttpStatus.CONFLICT),
    CONFLITO_VERSAO(HttpStatus.CONFLICT),
    REGRA_NEGOCIO(HttpStatus.CONFLICT),
    TRANSICAO_INVALIDA(HttpStatus.CONFLICT),
    MATERIAL_INCOMPATIVEL(HttpStatus.CONFLICT),
    PONTO_COM_SOLICITACOES_ABERTAS(HttpStatus.CONFLICT),

    ERRO_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    CodigoErro(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
