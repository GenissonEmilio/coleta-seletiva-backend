package br.edu.ifs.web1.coletaseletiva.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import jakarta.persistence.EntityManager;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** Criação e correção de solicitação (API-10) com o banco real; dados desfeitos ao fim de cada teste. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SolicitacaoIntegracaoTest {

    @TestBean
    Clock relogio;

    static Clock relogio() {
        return Clock.fixed(Instant.parse("2026-11-11T12:00:00Z"), ZoneOffset.UTC);
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManager entityManager;

    long maria;
    long enderecoDaMaria;
    long ponto;
    long vidro;
    long garrafa;

    @BeforeEach
    void cadastrarCenario() throws Exception {
        maria = cadastrarMorador("maria@exemplo.com");
        enderecoDaMaria = cadastrarEndereco(maria, "Rua das Flores");
        ponto = cadastrarPonto(true);
        vidro = cadastrarMaterial("Vidro", "KG");
        garrafa = cadastrarMaterial("Garrafa PET", "UNIDADE");
        vincular(ponto, vidro, "{\"limitePorSolicitacao\":10}");
        vincular(ponto, garrafa, "{}");
    }

    @Test
    void doisMateriaisValidosGeram201ComProtocoloItensEHistorico() throws Exception {
        String resposta = criar(maria, corpo(enderecoDaMaria, ponto, item(vidro, "2.5"), item(garrafa, "12")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.protocolo", matchesPattern("CS-\\d{8}-[A-Z2-9]{6}")))
                .andExpect(jsonPath("$.situacao").value("SOLICITADA"))
                .andExpect(jsonPath("$.versao").value(0))
                .andExpect(jsonPath("$.criadaEm", matchesPattern(".*([+-]\\d{2}:\\d{2}|Z)$")))
                .andExpect(jsonPath("$.moradorId").value(maria))
                .andExpect(jsonPath("$.endereco.logradouro").value("Rua das Flores"))
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].nomeMaterial").value("Garrafa PET"))
                .andExpect(jsonPath("$.itens[0].unidade").value("UNIDADE"))
                .andExpect(jsonPath("$.itens[0].quantidadeEstimada").value(12))
                .andExpect(jsonPath("$.itens[1].nomeMaterial").value("Vidro"))
                .andExpect(jsonPath("$.itens[1].quantidadeEstimada").value(2.5))
                .andExpect(jsonPath("$.historico.length()").value(1))
                .andExpect(jsonPath("$.historico[0].situacaoAnterior").doesNotExist())
                .andExpect(jsonPath("$.historico[0].situacaoNova").value("SOLICITADA"))
                .andExpect(jsonPath("$.historico[0].responsavelTipo").value("MORADOR"))
                .andExpect(jsonPath("$.historico[0].responsavelId").value(maria))
                .andReturn().getResponse().getContentAsString();
        long id = idDe(resposta);

        assertThat(contar("item_solicitacao WHERE solicitacao_id = " + id)).isEqualTo(2);
        assertThat(contar("historico_solicitacao WHERE solicitacao_id = " + id)).isEqualTo(1);

        String outra = criar(maria, corpo(enderecoDaMaria, ponto, item(vidro, "1"))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(outra, "$.protocolo")).isNotEqualTo(JsonPath.read(resposta, "$.protocolo"));
    }

    @Test
    void materialNaoAceitoInativoOuAcimaDoLimiteERejeitadoSemGravarNada() throws Exception {
        long papel = cadastrarMaterial("Papel", "KG");
        criar(maria, corpo(enderecoDaMaria, ponto, item(vidro, "2"), item(papel, "1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("MATERIAL_INCOMPATIVEL"));

        criar(maria, corpo(enderecoDaMaria, ponto, item(garrafa, "3"), item(vidro, "10.001")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("MATERIAL_INCOMPATIVEL"));
        criar(maria, corpo(enderecoDaMaria, ponto, item(vidro, "10"))).andExpect(status().isCreated());

        alterarSituacao("/api/v1/materiais/" + garrafa);
        criar(maria, corpo(enderecoDaMaria, ponto, item(vidro, "2"), item(garrafa, "3")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("MATERIAL_INCOMPATIVEL"));

        assertThat(contar("solicitacao")).isEqualTo(1);
        assertThat(contar("item_solicitacao")).isEqualTo(1);
        assertThat(contar("historico_solicitacao")).isEqualTo(1);
    }

    @Test
    void materialRepetidoEListaVaziaSaoRejeitados() throws Exception {
        criar(maria, corpo(enderecoDaMaria, ponto, item(vidro, "2"), item(vidro, "3")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[0].campo").value("itens[1].materialId"));
        criar(maria, "{\"enderecoId\":" + enderecoDaMaria + ",\"pontoId\":" + ponto + ",\"itens\":[]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[0].campo").value("itens"));
        assertThat(contar("solicitacao")).isZero();
    }

    @Test
    void pontoInativoOuSemRecolhimentoDomiciliarERejeitado() throws Exception {
        long semDomiciliar = cadastrarPonto(false);
        vincular(semDomiciliar, vidro, "{}");
        criar(maria, corpo(enderecoDaMaria, semDomiciliar, item(vidro, "2")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGRA_NEGOCIO"));

        alterarSituacao("/api/v1/pontos/" + ponto);
        criar(maria, corpo(enderecoDaMaria, ponto, item(vidro, "2")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGRA_NEGOCIO"));
        criar(maria, corpo(enderecoDaMaria, 999999, item(vidro, "2"))).andExpect(status().isNotFound());
        assertThat(contar("solicitacao")).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.0005"})
    void quantidadeZeroNegativaOuComCasasDemaisERejeitada(String quantidade) throws Exception {
        criar(maria, corpo(enderecoDaMaria, ponto, item(vidro, quantidade)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[0].campo").value("itens[0].quantidadeEstimada"));
        assertThat(contar("solicitacao")).isZero();
    }

    @Test
    void pecaFracionariaERejeitada() throws Exception {
        criar(maria, corpo(enderecoDaMaria, ponto, item(vidro, "2"), item(garrafa, "2.5")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[0].campo").value("itens[1].quantidadeEstimada"));
        assertThat(contar("solicitacao")).isZero();
    }

    @Test
    void enderecoDeveSerDoMoradorESoMoradorCria() throws Exception {
        long joao = cadastrarMorador("joao@exemplo.com");
        long enderecoDoJoao = cadastrarEndereco(joao, "Rua do Joao");
        String valido = corpo(enderecoDaMaria, ponto, item(vidro, "2"));

        criar(maria, corpo(enderecoDoJoao, ponto, item(vidro, "2")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("REGISTRO_NAO_ENCONTRADO"));
        mvc.perform(post("/api/v1/solicitacoes").contentType(MediaType.APPLICATION_JSON).content(valido))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/solicitacoes").header("X-Ator-Tipo", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON).content(valido))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        criar(999999, valido).andExpect(status().isForbidden());
        assertThat(contar("solicitacao")).isZero();
    }

    @Test
    void alterarCadastroDeEnderecoOuUnidadeNaoMudaAsCopias() throws Exception {
        long id = idDe(criar(maria, corpo(enderecoDaMaria, ponto, item(vidro, "2.5")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());

        mvc.perform(put("/api/v1/moradores/" + maria + "/enderecos/" + enderecoDaMaria)
                .contentType(MediaType.APPLICATION_JSON).content(endereco("Avenida Nova"))).andExpect(status().isOk());
        mvc.perform(put("/api/v1/materiais/" + vidro).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Vidro\",\"unidade\":\"LITRO\"}")).andExpect(status().isOk());
        entityManager.flush();

        assertThat(jdbc.queryForObject("SELECT logradouro FROM endereco WHERE id = ?", String.class, enderecoDaMaria))
                .isEqualTo("Avenida Nova");
        assertThat(jdbc.queryForObject("SELECT unidade FROM material WHERE id = ?", String.class, vidro))
                .isEqualTo("LITRO");
        assertThat(jdbc.queryForObject("SELECT end_logradouro FROM solicitacao WHERE id = ?", String.class, id))
                .isEqualTo("Rua das Flores");
        assertThat(jdbc.queryForObject("SELECT unidade FROM item_solicitacao WHERE solicitacao_id = ?",
                String.class, id)).isEqualTo("KG");
    }

    @Test
    void correcaoValidaTrocaItensEEnderecoSobeAVersaoEGuardaOsDadosAnteriores() throws Exception {
        long id = idDe(criar(maria, corpo(enderecoDaMaria, ponto, item(vidro, "2.5")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long outroEndereco = cadastrarEndereco(maria, "Travessa Dois");

        corrigir(maria, id, correcao(0, outroEndereco, item(garrafa, "4")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versao").value(1))
                .andExpect(jsonPath("$.situacao").value("SOLICITADA"))
                .andExpect(jsonPath("$.endereco.logradouro").value("Travessa Dois"))
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].nomeMaterial").value("Garrafa PET"))
                .andExpect(jsonPath("$.historico.length()").value(2))
                .andExpect(jsonPath("$.historico[1].situacaoAnterior").value("SOLICITADA"))
                .andExpect(jsonPath("$.historico[1].situacaoNova").value("SOLICITADA"))
                .andExpect(jsonPath("$.historico[1].motivo", containsString("Rua das Flores")))
                .andExpect(jsonPath("$.historico[1].motivo", containsString("Vidro 2.5 KG")));

        // Trocar só os itens (mesmo endereço) também devolve versão nova.
        corrigir(maria, id, correcao(1, outroEndereco, item(vidro, "3"), item(garrafa, "4")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versao").value(2))
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.historico.length()").value(3));
        assertThat(contar("item_solicitacao WHERE solicitacao_id = " + id)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT versao FROM solicitacao WHERE id = ?", Long.class, id)).isEqualTo(2);
    }

    @Test
    void correcaoInvalidaNaoAlteraNada() throws Exception {
        long id = idDe(criar(maria, corpo(enderecoDaMaria, ponto, item(vidro, "2.5")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long joao = cadastrarMorador("joao@exemplo.com");

        corrigir(maria, id, correcao(7, enderecoDaMaria, item(vidro, "3")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_VERSAO"));
        corrigir(maria, id, correcao(0, enderecoDaMaria, item(vidro, "11")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("MATERIAL_INCOMPATIVEL"));
        corrigir(maria, id, "{\"enderecoId\":" + enderecoDaMaria + ",\"itens\":[" + item(vidro, "3") + "]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[0].campo").value("versao"));
        corrigir(joao, id, correcao(0, enderecoDaMaria, item(vidro, "3"))).andExpect(status().isNotFound());

        assertThat(jdbc.queryForObject("SELECT versao FROM solicitacao WHERE id = ?", Long.class, id)).isZero();
        assertThat(jdbc.queryForObject("SELECT quantidade_estimada FROM item_solicitacao WHERE solicitacao_id = ?",
                java.math.BigDecimal.class, id)).isEqualByComparingTo("2.5");
        assertThat(contar("historico_solicitacao WHERE solicitacao_id = " + id)).isEqualTo(1);
    }

    @Test
    void correcaoAposAgendamentoConflita() throws Exception {
        long id = idDe(criar(maria, corpo(enderecoDaMaria, ponto, item(vidro, "2.5")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        // O agendamento é da API-12; aqui a situação é alterada direto no banco.
        jdbc.update("UPDATE solicitacao SET situacao = 'AGENDADA' WHERE id = ?", id);
        entityManager.clear();

        corrigir(maria, id, correcao(0, enderecoDaMaria, item(vidro, "3")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_INVALIDA"));
        assertThat(contar("historico_solicitacao WHERE solicitacao_id = " + id)).isEqualTo(1);
    }

    @Test
    void agendamentoFuturoRegistraEventoEPermiteCancelamentoPeloDono() throws Exception {
        long id = criarSolicitacaoParaAcao();
        long operador = operador(true);

        acao(id, "agendamento", "OPERADOR", operador,
                "{\"versao\":0,\"agendadaPara\":\"2026-11-11T09:30:00-03:00\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("AGENDADA"))
                .andExpect(jsonPath("$.versao").value(1))
                .andExpect(jsonPath("$.agendadaPara").value("2026-11-11T12:30:00Z"))
                .andExpect(jsonPath("$.historico[1].situacaoAnterior").value("SOLICITADA"))
                .andExpect(jsonPath("$.historico[1].situacaoNova").value("AGENDADA"))
                .andExpect(jsonPath("$.historico[1].ocorridoEm").value("2026-11-11T12:00:00Z"))
                .andExpect(jsonPath("$.historico[1].responsavelTipo").value("OPERADOR"))
                .andExpect(jsonPath("$.historico[1].responsavelId").value(operador))
                .andExpect(jsonPath("$.historico[1].agendamentoNovo").value("2026-11-11T12:30:00Z"));

        acao(id, "cancelamento", "MORADOR", maria, "{\"versao\":0,\"motivo\":\"Desisti\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_VERSAO"));
        acao(id, "cancelamento", "MORADOR", maria, "{\"versao\":1,\"motivo\":\"Desisti\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versao").value(2))
                .andExpect(jsonPath("$.historico[2].situacaoAnterior").value("AGENDADA"))
                .andExpect(jsonPath("$.historico[2].situacaoNova").value("CANCELADA"))
                .andExpect(jsonPath("$.historico[2].responsavelTipo").value("MORADOR"))
                .andExpect(jsonPath("$.historico[2].responsavelId").value(maria))
                .andExpect(jsonPath("$.historico[2].motivo").value("Desisti"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-11-11T08:59:59-03:00", "2026-11-11T09:00:00-03:00"})
    void agendamentoNoPassadoOuNoInstanteAtualNaoMudaSolicitacao(String data) throws Exception {
        long id = criarSolicitacaoParaAcao();
        acao(id, "agendamento", "OPERADOR", operador(true),
                "{\"versao\":0,\"agendadaPara\":\"" + data + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[0].campo").value("agendadaPara"));
        mvc.perform(get("/api/v1/solicitacoes/" + id).header("X-Ator-Tipo", "MORADOR").header("X-Ator-Id", maria))
                .andExpect(jsonPath("$.situacao").value("SOLICITADA"))
                .andExpect(jsonPath("$.versao").value(0))
                .andExpect(jsonPath("$.historico.length()").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"recusa", "cancelamento"})
    void encerramentoExigeMotivoEGravaAutorOperador(String acao) throws Exception {
        long id = criarSolicitacaoParaAcao();
        long operador = operador(true);
        acao(id, acao, "OPERADOR", operador, "{\"versao\":0,\"motivo\":\"  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[0].campo").value("motivo"));
        acao(id, acao, "OPERADOR", operador, "{\"versao\":0,\"motivo\":\" Atendimento indisponível \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value(acao.equals("recusa") ? "RECUSADA" : "CANCELADA"))
                .andExpect(jsonPath("$.versao").value(1))
                .andExpect(jsonPath("$.historico.length()").value(2))
                .andExpect(jsonPath("$.historico[1].motivo").value("Atendimento indisponível"))
                .andExpect(jsonPath("$.historico[1].responsavelId").value(operador));
    }

    @Test
    void acoesRespeitamTipoVinculoEPropriedade() throws Exception {
        long id = criarSolicitacaoParaAcao();
        String motivo = "{\"versao\":0,\"motivo\":\"Desisti\"}";
        acao(id, "recusa", "MORADOR", maria, motivo).andExpect(status().isForbidden());
        acao(id, "recusa", "OPERADOR", operador(false), motivo).andExpect(status().isNotFound());
        acao(id, "cancelamento", "MORADOR", cadastrarMorador("outra@exemplo.com"), motivo)
                .andExpect(status().isNotFound());
        acao(id, "cancelamento", "ADMINISTRADOR", 0, motivo).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"EM_RECOLHIMENTO", "CONCLUIDA", "RECUSADA", "CANCELADA"})
    void cancelamentoConflitaEmAtendimentoIniciadoOuFinal(String situacao) throws Exception {
        long id = criarSolicitacaoParaAcao();
        jdbc.update("UPDATE solicitacao SET situacao = ? WHERE id = ?", situacao, id);
        entityManager.clear();
        acao(id, "cancelamento", "MORADOR", maria, "{\"versao\":0,\"motivo\":\"Desisti\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_INVALIDA"));
        assertThat(contar("historico_solicitacao WHERE solicitacao_id = " + id)).isEqualTo(1);
    }

    @Test
    void acaoExigeVersaoNaoNegativa() throws Exception {
        long id = criarSolicitacaoParaAcao();
        acao(id, "cancelamento", "MORADOR", maria, "{\"motivo\":\"Desisti\"}")
                .andExpect(status().isBadRequest());
        acao(id, "cancelamento", "MORADOR", maria, "{\"versao\":-1,\"motivo\":\"Desisti\"}")
                .andExpect(status().isBadRequest());
        acao(id, "cancelamento", "MORADOR", maria, "{\"versao\":0.5,\"motivo\":\"Desisti\"}")
                .andExpect(status().isBadRequest());
        assertThat(contar("historico_solicitacao WHERE solicitacao_id = " + id)).isEqualTo(1);
    }

    private long criarSolicitacaoParaAcao() throws Exception {
        return idDe(criar(maria, corpo(enderecoDaMaria, ponto, item(vidro, "2")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private long operador(boolean vinculado) {
        long id = jdbc.queryForObject("INSERT INTO operador (nome, email) VALUES (?, ?) RETURNING id", Long.class,
                "Operador de teste", (vinculado ? "vinculado" : "sem.vinculo") + "@exemplo.com");
        if (vinculado) {
            jdbc.update("INSERT INTO operador_ponto (operador_id, ponto_id) VALUES (?, ?)", id, ponto);
        }
        return id;
    }

    private ResultActions acao(long id, String acao, String tipo, long ator, String corpo) throws Exception {
        return mvc.perform(post("/api/v1/solicitacoes/" + id + "/" + acao).header("X-Ator-Tipo", tipo)
                .header("X-Ator-Id", ator).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions criar(long morador, String corpo) throws Exception {
        return mvc.perform(post("/api/v1/solicitacoes").header("X-Ator-Tipo", "MORADOR")
                .header("X-Ator-Id", morador).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions corrigir(long morador, long id, String corpo) throws Exception {
        return mvc.perform(put("/api/v1/solicitacoes/" + id + "/dados").header("X-Ator-Tipo", "MORADOR")
                .header("X-Ator-Id", morador).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private static String corpo(long endereco, long ponto, String... itens) {
        return "{\"enderecoId\":" + endereco + ",\"pontoId\":" + ponto + ",\"observacoes\":\"Portão azul\","
                + "\"itens\":[" + String.join(",", itens) + "]}";
    }

    private static String correcao(long versao, long endereco, String... itens) {
        return "{\"versao\":" + versao + ",\"enderecoId\":" + endereco + ",\"itens\":[" + String.join(",", itens)
                + "]}";
    }

    private static String item(long material, String quantidade) {
        return "{\"materialId\":" + material + ",\"quantidadeEstimada\":" + quantidade + "}";
    }

    private static String endereco(String logradouro) {
        return "{\"logradouro\":\"" + logradouro + "\",\"numero\":\"200\",\"bairro\":\"Centro\","
                + "\"municipio\":\"Cidade Teste\",\"uf\":\"SE\",\"cep\":\"49000000\"}";
    }

    private long contar(String tabelaEFiltro) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabelaEFiltro, Long.class);
    }

    private long cadastrarMorador(String email) throws Exception {
        return idDe(mvc.perform(post("/api/v1/moradores").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Morador Ficticio\",\"email\":\"" + email + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private long cadastrarEndereco(long morador, String logradouro) throws Exception {
        return idDe(mvc.perform(post("/api/v1/moradores/" + morador + "/enderecos")
                        .contentType(MediaType.APPLICATION_JSON).content(endereco(logradouro)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private long cadastrarPonto(boolean domiciliar) throws Exception {
        String corpo = "{\"nome\":\"Ecoponto Centro\",\"logradouro\":\"Rua A\",\"numero\":\"1\","
                + "\"bairro\":\"Centro\",\"municipio\":\"Cidade Teste\",\"uf\":\"SE\",\"cep\":\"49000000\","
                + "\"recolhimentoDomiciliar\":" + domiciliar + "}";
        return idDe(mvc.perform(post("/api/v1/pontos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private long cadastrarMaterial(String nome, String unidade) throws Exception {
        return idDe(mvc.perform(post("/api/v1/materiais").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\",\"unidade\":\"" + unidade + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private void vincular(long ponto, long material, String corpo) throws Exception {
        mvc.perform(put("/api/v1/pontos/" + ponto + "/materiais/" + material)
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isCreated());
    }

    private void alterarSituacao(String caminho) throws Exception {
        mvc.perform(patch(caminho + "/situacao").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ativo\":false}")).andExpect(status().isOk());
    }

    private static long idDe(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
