package br.edu.ifs.web1.coletaseletiva.shared.paginacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import br.edu.ifs.web1.coletaseletiva.shared.erro.EntradaInvalidaException;

class PaginacaoTest {

    @Test
    void aceitaLimitesValidos() {
        Pageable p = Paginacao.pageable(0, 1, Sort.by("nome"));
        assertThat(p.getPageSize()).isEqualTo(1);
        assertThat(Paginacao.pageable(3, 100, Sort.by("nome")).getPageSize()).isEqualTo(100);
    }

    @Test
    void rejeitaValoresForaDoIntervalo() {
        assertThatThrownBy(() -> Paginacao.pageable(-1, 20, Sort.unsorted()))
                .isInstanceOf(EntradaInvalidaException.class);
        assertThatThrownBy(() -> Paginacao.pageable(0, 0, Sort.unsorted()))
                .isInstanceOf(EntradaInvalidaException.class);
        assertThatThrownBy(() -> Paginacao.pageable(0, 101, Sort.unsorted()))
                .isInstanceOf(EntradaInvalidaException.class);
    }

    @Test
    void sempreAdicionaIdComoDesempate() {
        Sort s = Paginacao.pageable(0, 20, Sort.by("nome")).getSort();
        assertThat(s.getOrderFor("nome")).isNotNull();
        assertThat(s.getOrderFor("id")).isNotNull();
        assertThat(Paginacao.pageable(0, 20, null).getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    void naoDuplicaIdQuandoJaOrdenaPorEle() {
        Sort s = Paginacao.pageable(0, 20, Sort.by("id")).getSort();
        assertThat(s.stream().count()).isEqualTo(1);
    }

    @Test
    void envelopeTemTodosOsMetadados() {
        var pagina = new PageImpl<>(List.of("a", "b"), Paginacao.pageable(1, 2, Sort.unsorted()), 5);
        var resposta = PaginaResposta.de(pagina);
        assertThat(resposta.content()).containsExactly("a", "b");
        assertThat(resposta.page()).isEqualTo(1);
        assertThat(resposta.size()).isEqualTo(2);
        assertThat(resposta.totalElements()).isEqualTo(5);
        assertThat(resposta.totalPages()).isEqualTo(3);
    }
}
