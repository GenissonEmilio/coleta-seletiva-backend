package br.edu.ifs.web1.coletaseletiva.exception;

import java.util.List;

import br.edu.ifs.web1.coletaseletiva.dto.common.ErroCampoResponse;

/** Entrada inválida detectada no Service ou na paginação (400). */
public class EntradaInvalidaException extends RuntimeException {

    private final List<ErroCampoResponse> erros;

    public EntradaInvalidaException(List<ErroCampoResponse> erros) {
        super("Revise os campos informados.");
        this.erros = List.copyOf(erros);
    }

    public EntradaInvalidaException(String campo, String mensagem) {
        this(List.of(new ErroCampoResponse(campo, mensagem)));
    }

    public List<ErroCampoResponse> getErros() {
        return erros;
    }
}
