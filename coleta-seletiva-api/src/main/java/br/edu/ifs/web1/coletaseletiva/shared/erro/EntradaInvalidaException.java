package br.edu.ifs.web1.coletaseletiva.shared.erro;

import java.util.List;

/** Entrada inválida detectada no Service ou na paginação (400). */
public class EntradaInvalidaException extends RuntimeException {

    private final List<ErroCampo> erros;

    public EntradaInvalidaException(List<ErroCampo> erros) {
        super("Revise os campos informados.");
        this.erros = List.copyOf(erros);
    }

    public EntradaInvalidaException(String campo, String mensagem) {
        this(List.of(new ErroCampo(campo, mensagem)));
    }

    public List<ErroCampo> getErros() {
        return erros;
    }
}
