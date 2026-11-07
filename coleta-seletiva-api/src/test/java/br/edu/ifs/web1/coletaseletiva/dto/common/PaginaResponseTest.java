package br.edu.ifs.web1.coletaseletiva.dto.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PaginaResponseTest {

    @Test
    void envelopeTemTodosOsMetadados() {
        var pagina = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 5);
        var resposta = PaginaResponse.de(pagina);

        assertThat(resposta.content()).containsExactly("a", "b");
        assertThat(resposta.page()).isEqualTo(1);
        assertThat(resposta.size()).isEqualTo(2);
        assertThat(resposta.totalElements()).isEqualTo(5);
        assertThat(resposta.totalPages()).isEqualTo(3);
    }
}
