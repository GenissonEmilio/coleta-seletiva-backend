package br.edu.ifs.web1.coletaseletiva.services.contexto;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

import br.edu.ifs.web1.coletaseletiva.exception.AcessoNegadoException;

/** Regras de escopo que as issues de solicitação (API-10 a API-13) vão chamar. Não precisa de banco. */
class ContextoServiceTest {

    private static final long PONTO_A = 1L;
    private static final long PONTO_B = 2L;

    private final ContextoService service = new ContextoService(null, null);

    private final ContextoAtor admin = new ContextoAtor(TipoAtor.ADMINISTRADOR, null, "Administrador", Set.of());
    private final ContextoAtor operadorDoPontoA = new ContextoAtor(TipoAtor.OPERADOR, 10L, "Ana", Set.of(PONTO_A));
    private final ContextoAtor moradora = new ContextoAtor(TipoAtor.MORADOR, 20L, "Maria", Set.of());

    @Test
    void operadorDoPontoANaoAgeNoPontoB() {
        assertThatCode(() -> service.exigirOperadorDoPonto(operadorDoPontoA, PONTO_A)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.exigirOperadorDoPonto(operadorDoPontoA, PONTO_B))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void moradorEAdministradorNaoFazemAcaoDeOperador() {
        assertThatThrownBy(() -> service.exigirOperadorDoPonto(moradora, PONTO_A))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> service.exigirOperadorDoPonto(admin, PONTO_A))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void moradorSoAcessaOsPropriosRegistros() {
        assertThatCode(() -> service.exigirMoradorDono(moradora, 20L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.exigirMoradorDono(moradora, 21L)).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> service.exigirMoradorDono(operadorDoPontoA, 10L))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void soAdministradorPassaNaExigenciaDeAdministrador() {
        assertThatCode(() -> service.exigirAdministrador(admin)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.exigirAdministrador(operadorDoPontoA))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> service.exigirAdministrador(moradora)).isInstanceOf(AcessoNegadoException.class);
    }
}
