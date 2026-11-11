package br.edu.ifs.web1.coletaseletiva.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import br.edu.ifs.web1.coletaseletiva.model.HistoricoSolicitacaoModel;
import br.edu.ifs.web1.coletaseletiva.repository.HistoricoSolicitacaoRepository;
import br.edu.ifs.web1.coletaseletiva.repository.PontoRepository;
import br.edu.ifs.web1.coletaseletiva.repository.SolicitacaoRepository;

/**
 * Atomicidade (RN06) e trava do ponto com transações reais: este teste NÃO é @Transactional,
 * então cada requisição confirma ou desfaz de verdade. Os dados próprios são apagados ao fim.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SolicitacaoTransacaoIntegracaoTest {

    private static final String MARCA = "Transacao API-10";
    /** SQLSTATE do PostgreSQL para "lock_not_available" (NOWAIT sem conseguir a trava). */
    private static final String TRAVA_INDISPONIVEL = "55P03";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    DataSource dataSource;

    @Autowired
    TransactionTemplate transacao;

    @Autowired
    PontoRepository pontoRepository;

    @Autowired
    SolicitacaoRepository solicitacaoRepository;

    @MockitoSpyBean
    HistoricoSolicitacaoRepository historicoRepository;

    long morador;
    long endereco;
    long ponto;
    long vidro;
    long papel;

    @BeforeEach
    void cadastrarCenario() throws Exception {
        apagarDadosDoTeste();
        morador = idDe(enviar(post("/api/v1/moradores"),
                "{\"nome\":\"" + MARCA + "\",\"email\":\"transacao.api10@exemplo.com\"}"));
        endereco = idDe(enviar(post("/api/v1/moradores/" + morador + "/enderecos"),
                "{\"logradouro\":\"Rua das Flores\",\"numero\":\"200\",\"bairro\":\"Centro\","
                        + "\"municipio\":\"Cidade Teste\",\"uf\":\"SE\",\"cep\":\"49000000\"}"));
        ponto = idDe(enviar(post("/api/v1/pontos"),
                "{\"nome\":\"" + MARCA + "\",\"logradouro\":\"Rua A\",\"numero\":\"1\",\"bairro\":\"Centro\","
                        + "\"municipio\":\"Cidade Teste\",\"uf\":\"SE\",\"cep\":\"49000000\","
                        + "\"recolhimentoDomiciliar\":true}"));
        vidro = idDe(enviar(post("/api/v1/materiais"), "{\"nome\":\"" + MARCA + " vidro\",\"unidade\":\"KG\"}"));
        papel = idDe(enviar(post("/api/v1/materiais"), "{\"nome\":\"" + MARCA + " papel\",\"unidade\":\"KG\"}"));
        enviar(put("/api/v1/pontos/" + ponto + "/materiais/" + vidro), "{}");
    }

    @AfterEach
    void apagarDadosDoTeste() {
        String solicitacoes = "(SELECT s.id FROM solicitacao s JOIN morador m ON m.id = s.morador_id "
                + "WHERE m.nome = '" + MARCA + "')";
        jdbc.update("DELETE FROM historico_solicitacao WHERE solicitacao_id IN " + solicitacoes);
        jdbc.update("DELETE FROM item_solicitacao WHERE solicitacao_id IN " + solicitacoes);
        jdbc.update("DELETE FROM solicitacao WHERE morador_id IN (SELECT id FROM morador WHERE nome = ?)", MARCA);
        jdbc.update("DELETE FROM ponto_material WHERE ponto_id IN (SELECT id FROM ponto_coleta WHERE nome = ?)",
                MARCA);
        jdbc.update("DELETE FROM endereco WHERE morador_id IN (SELECT id FROM morador WHERE nome = ?)", MARCA);
        jdbc.update("DELETE FROM morador WHERE nome = ?", MARCA);
        jdbc.update("DELETE FROM material WHERE nome LIKE ?", MARCA + " %");
        jdbc.update("DELETE FROM ponto_coleta WHERE nome = ?", MARCA);
    }

    @Test
    void criacaoConfirmadaPersisteSolicitacaoItemEHistorico() throws Exception {
        criar(item(vidro, "2")).andExpect(status().isCreated());

        assertThat(linhasDoMorador()).containsExactly(1L, 1L, 1L);
    }

    @Test
    void segundoItemIncompativelNaoDeixaLinhasParciais() throws Exception {
        criar(item(vidro, "2") + "," + item(papel, "1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("MATERIAL_INCOMPATIVEL"));

        assertThat(linhasDoMorador()).containsExactly(0L, 0L, 0L);
    }

    @Test
    void falhaAoGravarHistoricoDesfazSolicitacaoEItens() throws Exception {
        doThrow(new IllegalStateException("falha simulada no histórico"))
                .when(historicoRepository).save(any(HistoricoSolicitacaoModel.class));

        criar(item(vidro, "2"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"));

        assertThat(linhasDoMorador()).containsExactly(0L, 0L, 0L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void duasCorrecoesQueLeramAMesmaVersaoTemUmaVencedora(boolean trocarEndereco) throws Exception {
        long id = idDe(criar(item(vidro, "2")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        long destino = trocarEndereco ? idDe(enviar(post("/api/v1/moradores/" + morador + "/enderecos"),
                "{\"logradouro\":\"Outra rua\",\"numero\":\"201\",\"bairro\":\"Centro\","
                        + "\"municipio\":\"Cidade Teste\",\"uf\":\"SE\",\"cep\":\"49000000\"}")) : endereco;
        CyclicBarrier leituras = new CyclicBarrier(2);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<MockHttpServletResponse> primeira = executor.submit(() -> corrigirNaTransacao(id, destino, "3", leituras));
            Future<MockHttpServletResponse> segunda = executor.submit(() -> corrigirNaTransacao(id, destino, "4", leituras));
            MockHttpServletResponse a = primeira.get(15, TimeUnit.SECONDS);
            MockHttpServletResponse b = segunda.get(15, TimeUnit.SECONDS);
            assertThat(List.of(a.getStatus(), b.getStatus())).containsExactlyInAnyOrder(200, 409);
            MockHttpServletResponse vencedora = a.getStatus() == 200 ? a : b;
            MockHttpServletResponse perdedora = a.getStatus() == 409 ? a : b;
            assertThat((String) JsonPath.read(perdedora.getContentAsString(), "$.codigo")).isEqualTo("CONFLITO_VERSAO");
            assertThat((Integer) JsonPath.read(vencedora.getContentAsString(), "$.versao")).isEqualTo(1);
            Number quantidade = JsonPath.read(vencedora.getContentAsString(), "$.itens[0].quantidadeEstimada");
            assertThat(jdbc.queryForList("SELECT quantidade_estimada FROM item_solicitacao WHERE solicitacao_id = ?",
                    Integer.class, id)).containsExactly(quantidade.intValue());
        } finally {
            executor.shutdownNow();
        }

        assertThat(jdbc.queryForObject("SELECT versao FROM solicitacao WHERE id = ?", Long.class, id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM historico_solicitacao WHERE solicitacao_id = ?",
                Long.class, id)).isEqualTo(2);
    }

    private MockHttpServletResponse corrigirNaTransacao(long id, long destino, String quantidade,
            CyclicBarrier leituras) {
        return transacao.execute(statusTransacao -> {
            assertThat(solicitacaoRepository.findById(id).orElseThrow().getVersao()).isZero();
            try {
                leituras.await(8, TimeUnit.SECONDS);
                MockHttpServletResponse resposta = mvc.perform(put("/api/v1/solicitacoes/" + id + "/dados")
                        .header("X-Ator-Tipo", "MORADOR").header("X-Ator-Id", morador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correcao(destino, quantidade, "0"))).andReturn().getResponse();
                if (resposta.getStatus() >= 400) {
                    statusTransacao.setRollbackOnly();
                }
                return resposta;
            } catch (Exception e) {
                throw new IllegalStateException("Falha ao coordenar as duas correções", e);
            }
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "0.5", "\"0\"", "9223372036854775808"})
    void correcaoComVersaoInvalidaRetorna400SemGravar(String versao) throws Exception {
        long id = idDe(criar(item(vidro, "2")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        mvc.perform(put("/api/v1/solicitacoes/" + id + "/dados").header("X-Ator-Tipo", "MORADOR")
                .header("X-Ator-Id", morador).contentType(MediaType.APPLICATION_JSON)
                .content(correcao(endereco, "3", versao)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ENTRADA_INVALIDA"));
        assertThat(jdbc.queryForObject("SELECT versao FROM solicitacao WHERE id = ?", Long.class, id)).isZero();
        assertThat(linhasDoMorador()).containsExactly(1L, 1L, 1L);
    }

    private String correcao(long destino, String quantidade, String versao) {
        return "{\"versao\":" + versao + ",\"enderecoId\":" + destino + ",\"itens\":["
                + item(vidro, quantidade) + "]}";
    }

    /** Política combinada com a API-14: criar solicitação usa FOR SHARE; inativar ponto deve usar FOR UPDATE. */
    @Test
    void travaDoPontoPermiteOutrasSolicitacoesMasBloqueiaQuemQuerAlteraLo() {
        transacao.executeWithoutResult(status -> {
            pontoRepository.travarParaSolicitacao(ponto).orElseThrow();

            assertThat(consegueTravar("FOR SHARE NOWAIT")).isTrue();
            assertThat(consegueTravar("FOR UPDATE NOWAIT")).isFalse();
        });
        assertThat(consegueTravar("FOR UPDATE NOWAIT")).isTrue();
    }

    /** Tenta a trava em outra conexão, como faria outra requisição ao mesmo tempo. */
    private boolean consegueTravar(String modo) {
        try (Connection outra = dataSource.getConnection()) {
            outra.setAutoCommit(false);
            try (Statement comando = outra.createStatement()) {
                comando.execute("SELECT id FROM ponto_coleta WHERE id = " + ponto + " " + modo);
                return true;
            } finally {
                outra.rollback();
            }
        } catch (SQLException e) {
            if (TRAVA_INDISPONIVEL.equals(e.getSQLState())) {
                return false;
            }
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /** Quantas linhas de solicitação, item e histórico o morador do teste tem no banco. */
    private List<Long> linhasDoMorador() {
        String solicitacoes = "(SELECT id FROM solicitacao WHERE morador_id = " + morador + ")";
        return List.of(
                jdbc.queryForObject("SELECT count(*) FROM solicitacao WHERE morador_id = ?", Long.class, morador),
                jdbc.queryForObject("SELECT count(*) FROM item_solicitacao WHERE solicitacao_id IN " + solicitacoes,
                        Long.class),
                jdbc.queryForObject("SELECT count(*) FROM historico_solicitacao WHERE solicitacao_id IN "
                        + solicitacoes, Long.class));
    }

    private ResultActions criar(String itens) throws Exception {
        return mvc.perform(post("/api/v1/solicitacoes").header("X-Ator-Tipo", "MORADOR")
                .header("X-Ator-Id", morador).contentType(MediaType.APPLICATION_JSON)
                .content("{\"enderecoId\":" + endereco + ",\"pontoId\":" + ponto + ",\"itens\":[" + itens + "]}"));
    }

    private static String item(long material, String quantidade) {
        return "{\"materialId\":" + material + ",\"quantidadeEstimada\":" + quantidade + "}";
    }

    private String enviar(MockHttpServletRequestBuilder requisicao, String corpo) throws Exception {
        return mvc.perform(requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
    }

    private static long idDe(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
