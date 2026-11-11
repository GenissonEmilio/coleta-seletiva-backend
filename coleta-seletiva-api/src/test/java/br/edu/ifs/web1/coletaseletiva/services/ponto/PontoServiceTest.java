package br.edu.ifs.web1.coletaseletiva.services.ponto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ifs.web1.coletaseletiva.dto.ponto.PontoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.ponto.PontoResponse;
import br.edu.ifs.web1.coletaseletiva.dto.ponto.PontoSituacaoRequest;
import br.edu.ifs.web1.coletaseletiva.exception.EntradaInvalidaException;
import br.edu.ifs.web1.coletaseletiva.exception.RecursoNaoEncontradoException;
import br.edu.ifs.web1.coletaseletiva.exception.RegraNegocioException;
import br.edu.ifs.web1.coletaseletiva.model.PontoModel;
import br.edu.ifs.web1.coletaseletiva.repository.PontoRepository;
import br.edu.ifs.web1.coletaseletiva.repository.SolicitacaoRepository;
import br.edu.ifs.web1.coletaseletiva.services.paginacao.PaginacaoService;

@ExtendWith(MockitoExtension.class)
class PontoServiceTest {

    @Mock
    private PontoRepository pontoRepository;

    @Mock
    private SolicitacaoRepository solicitacaoRepository;

    @Mock
    private PaginacaoService paginacaoService;

    private PontoService service;

    @BeforeEach
    void criarService() {
        service = new PontoService(pontoRepository, solicitacaoRepository, paginacaoService);
    }

    @Test
    void cadastraPontoAtivoComUfEmMaiusculasENumeroSemEspacos() {
        when(pontoRepository.save(any(PontoModel.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        PontoResponse resposta = service.cadastrar(request(" 120 ", false, "se"));

        assertThat(resposta.uf()).isEqualTo("SE");
        assertThat(resposta.numero()).isEqualTo("120");
        assertThat(resposta.ativo()).isTrue();
    }

    @Test
    void semNumeroGravaNumeroNulo() {
        when(pontoRepository.save(any(PontoModel.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        PontoResponse resposta = service.cadastrar(request(null, true, "SE"));

        assertThat(resposta.semNumero()).isTrue();
        assertThat(resposta.numero()).isNull();
    }

    @Test
    void rejeitaNumeroPreenchidoQuandoSemNumero() {
        assertThatThrownBy(() -> service.cadastrar(request("120", true, "SE")))
                .isInstanceOf(EntradaInvalidaException.class);
        verify(pontoRepository, never()).save(any());
    }

    @Test
    void rejeitaNumeroVazioQuandoTemNumero() {
        assertThatThrownBy(() -> service.cadastrar(request(" ", false, "SE")))
                .isInstanceOf(EntradaInvalidaException.class);
        verify(pontoRepository, never()).save(any());
    }

    @Test
    void buscarPontoInexistenteLancaNaoEncontrado() {
        when(pontoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void alterarSituacaoInativaEReativa() {
        PontoModel ponto = new PontoModel();
        when(pontoRepository.travarParaAlteracao(1L)).thenReturn(Optional.of(ponto));
        when(pontoRepository.save(ponto)).thenReturn(ponto);

        assertThat(service.alterarSituacao(1L, new PontoSituacaoRequest(false)).ativo()).isFalse();
        assertThat(service.alterarSituacao(1L, new PontoSituacaoRequest(true)).ativo()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"false, true", "true, false"})
    void pontoInativoOuSemRecolhimentoDomiciliarNaoRecebeSolicitacao(boolean ativo, boolean domiciliar) {
        PontoModel ponto = ponto(ativo, domiciliar);
        when(pontoRepository.findById(1L)).thenReturn(Optional.of(ponto));

        assertThatThrownBy(() -> service.buscarDisponivelParaSolicitacao(1L))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void pontoAtivoComRecolhimentoDomiciliarRecebeSolicitacao() {
        PontoModel ponto = ponto(true, true);
        when(pontoRepository.findById(1L)).thenReturn(Optional.of(ponto));

        assertThat(service.buscarDisponivelParaSolicitacao(1L)).isSameAs(ponto);
    }

    private PontoModel ponto(boolean ativo, boolean domiciliar) {
        PontoModel ponto = new PontoModel();
        ponto.setAtivo(ativo);
        ponto.setRecolhimentoDomiciliar(domiciliar);
        return ponto;
    }

    private PontoRequest request(String numero, boolean semNumero, String uf) {
        return new PontoRequest("Ecoponto Centro", "Rua das Flores", numero, semNumero, "Centro", "Cidade Teste",
                uf, "49000000", null, null, null, true);
    }
}
