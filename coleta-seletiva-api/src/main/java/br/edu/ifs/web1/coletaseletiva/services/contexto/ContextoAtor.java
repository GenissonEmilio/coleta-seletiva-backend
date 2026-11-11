package br.edu.ifs.web1.coletaseletiva.services.contexto;

import java.util.Set;

/**
 * Ator da requisição, já conferido no banco pelo ContextoService.
 * id é nulo para o administrador; pontoIds só é preenchido para o operador.
 */
public record ContextoAtor(TipoAtor tipo, Long id, String nome, Set<Long> pontoIds) {

    public boolean is(TipoAtor outro) {
        return tipo == outro;
    }
}
