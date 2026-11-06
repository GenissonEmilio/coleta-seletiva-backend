package br.edu.ifs.web1.coletaseletiva.shared.erro;

/**
 * Violação de regra de negócio, duplicidade, versão ou transição inválida.
 * O Service lança esta exceção com o código adequado; o status HTTP vem do código.
 */
public class ErroNegocioException extends RuntimeException {

    private final CodigoErro codigo;

    public ErroNegocioException(CodigoErro codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public CodigoErro getCodigo() {
        return codigo;
    }
}
