package br.edu.ifs.web1.coletaseletiva.shared.paginacao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import br.edu.ifs.web1.coletaseletiva.shared.erro.EntradaInvalidaException;
import br.edu.ifs.web1.coletaseletiva.shared.erro.ErroCampo;

/**
 * Cria o Pageable de todas as listagens, com as regras do contrato:
 * page >= 0, size de 1 a 100 (padrão 20) e ordenação estável (id como desempate).
 * Valores inválidos geram 400; nunca são corrigidos em silêncio.
 */
public final class Paginacao {

    public static final String PAGE_PADRAO = "0";
    public static final String SIZE_PADRAO = "20";
    public static final int SIZE_MAXIMO = 100;

    private Paginacao() {
    }

    /**
     * Uso no controller:
     * <pre>
     * &#64;RequestParam(defaultValue = Paginacao.PAGE_PADRAO) int page,
     * &#64;RequestParam(defaultValue = Paginacao.SIZE_PADRAO) int size
     * ...
     * Pageable p = Paginacao.pageable(page, size, Sort.by("nome"));
     * </pre>
     * A ordem é definida pelo módulo (não pelo cliente) e recebe o id como desempate.
     */
    public static Pageable pageable(int page, int size, Sort ordem) {
        List<ErroCampo> erros = new ArrayList<>();
        if (page < 0) {
            erros.add(new ErroCampo("page", "deve ser maior ou igual a 0"));
        }
        if (size < 1 || size > SIZE_MAXIMO) {
            erros.add(new ErroCampo("size", "deve estar entre 1 e " + SIZE_MAXIMO));
        }
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
        return PageRequest.of(page, size, comDesempate(ordem));
    }

    private static Sort comDesempate(Sort ordem) {
        Sort base = ordem == null ? Sort.unsorted() : ordem;
        return base.getOrderFor("id") == null ? base.and(Sort.by(Sort.Direction.ASC, "id")) : base;
    }
}
