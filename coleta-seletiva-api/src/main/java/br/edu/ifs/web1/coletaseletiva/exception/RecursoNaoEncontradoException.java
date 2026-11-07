package br.edu.ifs.web1.coletaseletiva.exception;

/** Registro inexistente (404), inclusive quando o ID vem dentro do corpo da requisição. */
public class RecursoNaoEncontradoException extends RegraNegocioException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(CodigoErro.REGISTRO_NAO_ENCONTRADO, mensagem);
    }

    public RecursoNaoEncontradoException(String recurso, Object id) {
        this(recurso + " não encontrado(a): " + id);
    }
}
