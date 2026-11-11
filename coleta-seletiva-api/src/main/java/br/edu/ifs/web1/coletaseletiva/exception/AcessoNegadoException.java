package br.edu.ifs.web1.coletaseletiva.exception;

/** Ator de demonstração ausente, desconhecido ou sem permissão para a operação (403). */
public class AcessoNegadoException extends RegraNegocioException {

    public AcessoNegadoException(String mensagem) {
        super(CodigoErro.ACESSO_NEGADO, mensagem);
    }
}
