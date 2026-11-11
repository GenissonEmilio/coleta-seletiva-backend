package br.edu.ifs.web1.coletaseletiva.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
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
import org.springframework.dao.DataIntegrityViolationException;
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
    void falhaNoHistoricoDoCancelamentoReverteSituacaoEVersao() throws Exception {
        long id = idDe(criar(item(vidro, "2")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        doThrow(new IllegalStateException("falha simulada no histórico"))
                .when(historicoRepository).save(any(HistoricoSolicitacaoModel.class));

        mvc.perform(post("/api/v1/solicitacoes/" + id + "/cancelamento").header("X-Ator-Tipo", "MORADOR")
                .header("X-Ator-Id", morador).contentType(MediaType.APPLICATION_JSON)
                .content("{\"versao\":0,\"motivo\":\"Desisti\"}"))
                .andExpect(status().isInternalServerError());

        assertThat(jdbc.queryForObject("SELECT situacao FROM solicitacao WHERE id = ?", String.class, id))
                .isEqualTo("SOLICITADA");
        assertThat(jdbc.queryForObject("SELECT versao FROM solicitacao WHERE id = ?", Long.class, id)).isZero();
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

    @Test
    void duasCorrecoesSimultaneasComAMesmaVersaoUmaConfirmaEAOutraRecebe409() throws Exception {
        long id = idDe(criar(item(vidro, "2")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        String correcao = "{\"versao\":0,\"enderecoId\":" + endereco + ",\"itens\":[" + item(vidro, "3") + "]}";
        CountDownLatch largada = new CountDownLatch(1);
        Callable<Integer> corrigir = () -> {
            largada.await();
            return mvc.perform(put("/api/v1/solicitacoes/" + id + "/dados").header("X-Ator-Tipo", "MORADOR")
                            .header("X-Ator-Id", morador).contentType(MediaType.APPLICATION_JSON).content(correcao))
                    .andReturn().getResponse().getStatus();
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> primeira = executor.submit(corrigir);
            Future<Integer> segunda = executor.submit(corrigir);
            largada.countDown();
            assertThat(List.of(primeira.get(), segunda.get())).containsExactlyInAnyOrder(200, 409);
        } finally {
            executor.shutdownNow();
        }

        assertThat(jdbc.queryForObject("SELECT versao FROM solicitacao WHERE id = ?", Long.class, id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM historico_solicitacao WHERE solicitacao_id = ?",
                Long.class, id)).isEqualTo(2);
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

    @ParameterizedTest
    @ValueSource(strings = {"SOLICITADA", "AGENDADA", "EM_RECOLHIMENTO"})
    void abertasImpedemInativacaoERemocaoSomenteDosMateriaisAfetados(String situacao) throws Exception {
        long id = criarEInformarSituacao(situacao);
        enviar(put("/api/v1/pontos/" + ponto + "/materiais/" + papel), "{}");

        mvc.perform(inativacaoDoPonto()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PONTO_COM_SOLICITACOES_ABERTAS"));
        mvc.perform(remocaoDoVidro()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PONTO_COM_SOLICITACOES_ABERTAS"));
        mvc.perform(delete("/api/v1/pontos/" + ponto + "/materiais/" + papel)).andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("SELECT ativo FROM ponto_coleta WHERE id = ?", Boolean.class, ponto)).isTrue();
        assertThat(jdbc.queryForObject("SELECT material_id FROM ponto_material WHERE ponto_id = ?", Long.class, ponto))
                .isEqualTo(vidro);
        assertThat(jdbc.queryForObject("SELECT situacao FROM solicitacao WHERE id = ?", String.class, id))
                .isEqualTo(situacao);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONCLUIDA", "RECUSADA", "CANCELADA"})
    void finaisPermitemAlteracaoEPreservamItensEHistorico(String situacao) throws Exception {
        long id = criarEInformarSituacao(situacao);

        mvc.perform(inativacaoDoPonto()).andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(false));
        mvc.perform(remocaoDoVidro()).andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("SELECT situacao FROM solicitacao WHERE id = ?", String.class, id))
                .isEqualTo(situacao);
        assertThat(jdbc.queryForObject("SELECT unidade FROM item_solicitacao WHERE solicitacao_id = ?",
                String.class, id)).isEqualTo("KG");
        assertThat(linhasDoMorador()).containsExactly(1L, 1L, 1L);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM ponto_coleta WHERE id = ?", ponto))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM material WHERE id = ?", vidro))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM morador WHERE id = ?", morador))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void moradorInativoMantemRegistrosEDeixaDeOperarNoContexto() throws Exception {
        criar(item(vidro, "2")).andExpect(status().isCreated());
        mvc.perform(patch("/api/v1/moradores/" + morador + "/situacao")
                .contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(false));

        criar(item(vidro, "2")).andExpect(status().isForbidden());
        assertThat(linhasDoMorador()).containsExactly(1L, 1L, 1L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void criacaoQueConfirmaPrimeiroBloqueiaAlteracaoDoPonto(boolean removerVinculo) throws Exception {
        MockHttpServletRequestBuilder alteracao = removerVinculo ? remocaoDoVidro() : inativacaoDoPonto();
        MockHttpServletResponse resposta = disputar(requisicaoCriacao(item(vidro, "2")), 201, alteracao);

        assertThat(resposta.getStatus()).isEqualTo(409);
        assertThat((String) JsonPath.read(resposta.getContentAsString(), "$.codigo"))
                .isEqualTo("PONTO_COM_SOLICITACOES_ABERTAS");
        assertThat(linhasDoMorador()).containsExactly(1L, 1L, 1L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void alteracaoQueConfirmaPrimeiroImpedeCriacao(boolean removerVinculo) throws Exception {
        MockHttpServletRequestBuilder alteracao = removerVinculo ? remocaoDoVidro() : inativacaoDoPonto();
        MockHttpServletResponse resposta = disputar(alteracao, removerVinculo ? 204 : 200,
                requisicaoCriacao(item(vidro, "2")));

        assertThat(resposta.getStatus()).isEqualTo(409);
        assertThat((String) JsonPath.read(resposta.getContentAsString(), "$.codigo"))
                .isEqualTo(removerVinculo ? "MATERIAL_INCOMPATIVEL" : "REGRA_NEGOCIO");
        assertThat(linhasDoMorador()).containsExactly(0L, 0L, 0L);
    }

    /** Confirma a primeira operação só depois que o PostgreSQL mostra a segunda esperando sua trava. */
    private MockHttpServletResponse disputar(MockHttpServletRequestBuilder primeira, int sucesso,
            MockHttpServletRequestBuilder segunda) throws Exception {
        var pidSegunda = new java.util.concurrent.atomic.AtomicInteger();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<MockHttpServletResponse> aguardando = transacao.execute(statusPrimeira -> {
                assertThat(executar(primeira).getStatus()).isEqualTo(sucesso);
                int pidPrimeira = jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
                Future<MockHttpServletResponse> pendente = executor.submit(() -> transacao.execute(statusSegunda -> {
                    pidSegunda.set(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
                    MockHttpServletResponse resposta = executar(segunda);
                    if (resposta.getStatus() >= 400) {
                        statusSegunda.setRollbackOnly();
                    }
                    return resposta;
                }));
                await().pollInSameThread().atMost(Duration.ofSeconds(8)).until(() -> pidSegunda.get() > 0
                        && Boolean.TRUE.equals(jdbc.queryForObject("SELECT ? = ANY(pg_blocking_pids(?))",
                                Boolean.class, pidPrimeira, pidSegunda.get())));
                return pendente;
            });
            return aguardando.get(12, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    private MockHttpServletResponse executar(MockHttpServletRequestBuilder requisicao) {
        try {
            return mvc.perform(requisicao).andReturn().getResponse();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao executar a requisição concorrente", e);
        }
    }

    private long criarEInformarSituacao(String situacao) throws Exception {
        long id = idDe(criar(item(vidro, "2")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        jdbc.update("UPDATE solicitacao SET situacao = ? WHERE id = ?", situacao, id);
        return id;
    }

    private MockHttpServletRequestBuilder inativacaoDoPonto() {
        return patch("/api/v1/pontos/" + ponto + "/situacao").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ativo\":false}");
    }

    private MockHttpServletRequestBuilder remocaoDoVidro() {
        return delete("/api/v1/pontos/" + ponto + "/materiais/" + vidro);
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
        return mvc.perform(requisicaoCriacao(itens));
    }

    private MockHttpServletRequestBuilder requisicaoCriacao(String itens) {
        return post("/api/v1/solicitacoes").header("X-Ator-Tipo", "MORADOR")
                .header("X-Ator-Id", morador).contentType(MediaType.APPLICATION_JSON)
                .content("{\"enderecoId\":" + endereco + ",\"pontoId\":" + ponto + ",\"itens\":[" + itens + "]}");
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
