package br.edu.ifs.web1.coletaseletiva.services.material;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import br.edu.ifs.web1.coletaseletiva.exception.EntradaInvalidaException;
import br.edu.ifs.web1.coletaseletiva.model.UnidadeMaterial;

class QuantidadeMaterialServiceTest {

    private final QuantidadeMaterialService service = new QuantidadeMaterialService();

    @ParameterizedTest
    @CsvSource({"KG, 2.500", "LITRO, 0.125", "UNIDADE, 2.000", "UNIDADE, 1.0000", "KG, 999999999.999"})
    void aceitaQuantidadesCompativeisComAUnidadeEOArmazenamento(UnidadeMaterial unidade, BigDecimal valor) {
        assertThatCode(() -> service.validarPositiva(valor, unidade, "quantidadeEstimada")).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({"UNIDADE, 1.5", "KG, 0", "LITRO, -0.1", "KG, 0.0001", "KG, 1000000000"})
    void rejeitaFracaoEmPecaZeroNegativoOuPerdaDePrecisao(UnidadeMaterial unidade, BigDecimal valor) {
        assertThatThrownBy(() -> service.validarPositiva(valor, unidade, "itens[0].quantidadeEstimada"))
                .isInstanceOfSatisfying(EntradaInvalidaException.class,
                        e -> assertThat(e.getErros().getFirst().campo()).isEqualTo("itens[0].quantidadeEstimada"));
    }

    @ParameterizedTest
    @EnumSource(UnidadeMaterial.class)
    void efetivoAceitaZeroEmQualquerUnidade(UnidadeMaterial unidade) {
        assertThatCode(() -> service.validarNaoNegativa(BigDecimal.ZERO, unidade, "quantidadeEfetiva"))
                .doesNotThrowAnyException();
    }

    @Test
    void efetivoRejeitaNegativoEFracaoEmPeca() {
        assertThatThrownBy(() -> service.validarNaoNegativa(new BigDecimal("-1"), UnidadeMaterial.KG, "efetiva"))
                .isInstanceOf(EntradaInvalidaException.class);
        assertThatThrownBy(() -> service.validarNaoNegativa(new BigDecimal("0.5"), UnidadeMaterial.UNIDADE, "efetiva"))
                .isInstanceOf(EntradaInvalidaException.class);
    }

    @Test
    void quantidadeAusenteApontaCampoObrigatorio() {
        assertThatThrownBy(() -> service.validarPositiva(null, UnidadeMaterial.KG, "quantidadeEstimada"))
                .isInstanceOfSatisfying(EntradaInvalidaException.class,
                        e -> assertThat(e.getErros().getFirst().campo()).isEqualTo("quantidadeEstimada"));
    }
}
