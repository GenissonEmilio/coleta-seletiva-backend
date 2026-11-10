package br.edu.ifs.web1.coletaseletiva.services.paginacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import br.edu.ifs.web1.coletaseletiva.dto.common.PaginacaoRequest;
import br.edu.ifs.web1.coletaseletiva.exception.EntradaInvalidaException;

class PaginacaoServiceTest {

    private final PaginacaoService service = new PaginacaoService();

    @Test
    void aceitaLimitesValidos() {
        Pageable p = service.criarPageable(new PaginacaoRequest(0, 1), Sort.by("nome"));
        assertThat(p.getPageSize()).isEqualTo(1);
        assertThat(service.criarPageable(new PaginacaoRequest(3, 100), Sort.by("nome")).getPageSize()).isEqualTo(100);
    }

    @Test
    void rejeitaValoresForaDoIntervalo() {
        assertThatThrownBy(() -> service.criarPageable(new PaginacaoRequest(-1, 20), Sort.unsorted()))
                .isInstanceOf(EntradaInvalidaException.class);
        assertThatThrownBy(() -> service.criarPageable(new PaginacaoRequest(0, 0), Sort.unsorted()))
                .isInstanceOf(EntradaInvalidaException.class);
        assertThatThrownBy(() -> service.criarPageable(new PaginacaoRequest(0, 101), Sort.unsorted()))
                .isInstanceOf(EntradaInvalidaException.class);
    }

    @Test
    void sempreAdicionaIdComoDesempate() {
        Sort s = service.criarPageable(new PaginacaoRequest(0, 20), Sort.by("nome")).getSort();
        assertThat(s.getOrderFor("nome")).isNotNull();
        assertThat(s.getOrderFor("id")).isNotNull();
        assertThat(service.criarPageable(new PaginacaoRequest(0, 20), null).getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    void naoDuplicaIdQuandoJaOrdenaPorEle() {
        Sort s = service.criarPageable(new PaginacaoRequest(0, 20), Sort.by("id")).getSort();
        assertThat(s.stream().count()).isEqualTo(1);
    }

}
