package br.edu.ifs.web1.coletaseletiva.services.paginacao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import br.edu.ifs.web1.coletaseletiva.dto.common.ErroCampoResponse;
import br.edu.ifs.web1.coletaseletiva.dto.common.PaginacaoRequest;
import br.edu.ifs.web1.coletaseletiva.exception.EntradaInvalidaException;

/**
 * Cria o Pageable de todas as listagens, com as regras do contrato:
 * page >= 0, size de 1 a 100 (padrão 20) e ordenação estável (id como desempate).
 * Valores inválidos geram 400; nunca são corrigidos em silêncio.
 */
@Service
public class PaginacaoService {

    /**
     * Uso no Service do módulo:
     * <pre>
     * Pageable pagina = paginacaoService.criarPageable(entrada, Sort.by("nome"));
     * </pre>
     * A ordem é definida pelo módulo e recebe o id como desempate.
     */
    public Pageable criarPageable(PaginacaoRequest entrada, Sort ordem) {
        int page = entrada.page();
        int size = entrada.size();
        List<ErroCampoResponse> erros = new ArrayList<>();
        if (page < 0) {
            erros.add(new ErroCampoResponse("page", "deve ser maior ou igual a 0"));
        }
        if (size < 1 || size > PaginacaoRequest.SIZE_MAXIMO) {
            erros.add(new ErroCampoResponse("size", "deve estar entre 1 e " + PaginacaoRequest.SIZE_MAXIMO));
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
