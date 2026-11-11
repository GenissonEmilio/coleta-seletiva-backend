package br.edu.ifs.web1.coletaseletiva.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultas de solicitação por contexto (API-11) com o banco real; dados desfeitos ao fim de cada teste.
 * Cenário: Maria pediu no ponto A (janeiro) e no ponto B (fevereiro); João pediu no ponto A (janeiro, agendada).
 * Ana opera o ponto A, Bruno opera o ponto B.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SolicitacaoConsultaIntegracaoTest {

    private static final String LISTA = "/api/v1/solicitacoes";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManager entityManager;

    long maria;
    long joao;
    long enderecoDaMaria;
    long pontoA;
    long pontoB;
    long vidro;
    long ana;
    long bruno;
    long mariaNoA;
    long mariaNoB;
    long joaoNoA;

    @BeforeEach
    void cadastrarCenario() throws Exception {
        maria = cadastrar("/api/v1/moradores", "{\"nome\":\"Maria Ficticia\",\"email\":\"maria@exemplo.com\"}");
        joao = cadastrar("/api/v1/moradores", "{\"nome\":\"Joao Ficticio\",\"email\":\"joao@exemplo.com\"}");
        enderecoDaMaria = cadastrar("/api/v1/moradores/" + maria + "/enderecos", endereco("Rua das Flores"));
        long enderecoDoJoao = cadastrar("/api/v1/moradores/" + joao + "/enderecos", endereco("Rua do Joao"));
        pontoA = cadastrar("/api/v1/pontos", ponto("Ecoponto A"));
        pontoB = cadastrar("/api/v1/pontos", ponto("Ecoponto B"));
        vidro = cadastrar("/api/v1/materiais", "{\"nome\":\"Vidro\",\"unidade\":\"KG\"}");
        for (long ponto : new long[] {pontoA, pontoB}) {
            mvc.perform(put("/api/v1/pontos/" + ponto + "/materiais/" + vidro)
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isCreated());
        }
        ana = cadastrarOperador("ana@exemplo.org", pontoA);
        bruno = cadastrarOperador("bruno@exemplo.org", pontoB);

        mariaNoA = solicitar(maria, enderecoDaMaria, pontoA);
        mariaNoB = solicitar(maria, enderecoDaMaria, pontoB);
        joaoNoA = solicitar(joao, enderecoDoJoao, pontoA);
        jdbc.update("UPDATE solicitacao SET criada_em = '2026-01-10T12:00:00Z' WHERE id = ?", mariaNoA);
        jdbc.update("UPDATE solicitacao SET criada_em = '2026-02-10T12:00:00Z' WHERE id = ?", mariaNoB);
        jdbc.update("UPDATE solicitacao SET criada_em = '2026-01-20T12:00:00Z', situacao = 'AGENDADA', "
                + "agendada_para = '2026-01-25T13:00:00Z' WHERE id = ?", joaoNoA);
        entityManager.clear();
    }

    @Test
    void moradorListaApenasAsProprias() throws Exception {
        mvc.perform(como("MORADOR", maria, get(LISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(mariaNoB))
                .andExpect(jsonPath("$.content[1].id").value(mariaNoA));
        mvc.perform(como("MORADOR", joao, get(LISTA)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(joaoNoA));
    }

    @Test
    void operadorListaApenasAsDosSeusPontos() throws Exception {
        mvc.perform(como("OPERADOR", ana, get(LISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(joaoNoA))
                .andExpect(jsonPath("$.content[0].nomePonto").value("Ecoponto A"))
                .andExpect(jsonPath("$.content[1].id").value(mariaNoA));
        mvc.perform(como("OPERADOR", bruno, get(LISTA)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(mariaNoB));
        // Filtrar por um ponto que não é dela não abre o acesso: lista vazia.
        mvc.perform(como("OPERADOR", ana, get(LISTA).param("pontoId", String.valueOf(pontoB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        long semPonto = cadastrarOperador("carla@exemplo.org", null);
        mvc.perform(como("OPERADOR", semPonto, get(LISTA))).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void administradorListaTodasDaMaisRecenteParaAMaisAntigaComPaginacao() throws Exception {
        mvc.perform(comoAdmin(get(LISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].id").value(mariaNoB))
                .andExpect(jsonPath("$.content[1].id").value(joaoNoA))
                .andExpect(jsonPath("$.content[1].situacao").value("AGENDADA"))
                .andExpect(jsonPath("$.content[1].agendadaPara").value("2026-01-25T13:00:00Z"))
                .andExpect(jsonPath("$.content[2].id").value(mariaNoA));
        mvc.perform(comoAdmin(get(LISTA).param("page", "1").param("size", "2")))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(mariaNoA))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(comoAdmin(get(LISTA).param("size", "100"))).andExpect(status().isOk());
        mvc.perform(comoAdmin(get(LISTA).param("size", "101")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[0].campo").value("size"));
    }

    @Test
    void filtrosDePeriodoSituacaoEPontoSeCombinam() throws Exception {
        // Janeiro inteiro: início inclusivo, fim exclusivo.
        mvc.perform(comoAdmin(get(LISTA).param("inicio", "2026-01-01T00:00:00Z").param("fim", "2026-02-01T00:00:00Z")))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(joaoNoA))
                .andExpect(jsonPath("$.content[1].id").value(mariaNoA));
        mvc.perform(comoAdmin(get(LISTA).param("inicio", "2026-01-01T00:00:00Z").param("fim", "2026-02-01T00:00:00Z")
                        .param("situacao", "AGENDADA").param("pontoId", String.valueOf(pontoA))))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(joaoNoA));
        mvc.perform(comoAdmin(get(LISTA).param("situacao", "SOLICITADA").param("pontoId", String.valueOf(pontoA))))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(mariaNoA));
        // A criação exatamente no início entra; exatamente no fim fica de fora. O offset é respeitado.
        mvc.perform(comoAdmin(get(LISTA).param("inicio", "2026-01-10T09:00:00-03:00")
                        .param("fim", "2026-01-20T12:00:00Z")))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(mariaNoA));
        // O filtro não amplia o contexto: Maria filtrando AGENDADA não vê a do João.
        mvc.perform(como("MORADOR", maria, get(LISTA).param("situacao", "AGENDADA")))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void filtroInvalidoResponde400ApontandoOCampo() throws Exception {
        mvc.perform(comoAdmin(get(LISTA).param("inicio", "2026-01-01T00:00:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[0].campo").value("inicio"));
        mvc.perform(comoAdmin(get(LISTA).param("inicio", "2026-02-01T00:00:00Z").param("fim", "2026-01-01T00:00:00Z")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[0].campo").value("fim"));
        mvc.perform(comoAdmin(get(LISTA).param("situacao", "PENDENTE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[0].campo").value("situacao"));
        mvc.perform(get(LISTA)).andExpect(status().isForbidden());
    }

    @Test
    void detalheMostraItensEnderecoCopiadoAgendamentoEHistoricoNoContexto() throws Exception {
        mvc.perform(como("MORADOR", joao, get(LISTA + "/" + joaoNoA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.protocolo").exists())
                .andExpect(jsonPath("$.situacao").value("AGENDADA"))
                .andExpect(jsonPath("$.agendadaPara").value("2026-01-25T13:00:00Z"))
                .andExpect(jsonPath("$.concluidaEm").doesNotExist())
                .andExpect(jsonPath("$.endereco.logradouro").value("Rua do Joao"))
                .andExpect(jsonPath("$.itens[0].nomeMaterial").value("Vidro"))
                .andExpect(jsonPath("$.itens[0].unidade").value("KG"))
                .andExpect(jsonPath("$.itens[0].quantidadeEstimada").value(2))
                .andExpect(jsonPath("$.historico.length()").value(1));
        mvc.perform(como("OPERADOR", ana, get(LISTA + "/" + joaoNoA))).andExpect(status().isOk());
        mvc.perform(comoAdmin(get(LISTA + "/" + joaoNoA))).andExpect(status().isOk());
    }

    @Test
    void detalheEHistoricoForaDoContextoRespondem404SemVazarDados() throws Exception {
        for (String caminho : new String[] {LISTA + "/" + joaoNoA, LISTA + "/" + joaoNoA + "/historico"}) {
            mvc.perform(como("MORADOR", maria, get(caminho)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value("REGISTRO_NAO_ENCONTRADO"))
                    .andExpect(content().string(not(containsString("Rua do Joao"))))
                    .andExpect(content().string(not(containsString("CS-"))));
            mvc.perform(como("OPERADOR", bruno, get(caminho))).andExpect(status().isNotFound());
            mvc.perform(get(caminho)).andExpect(status().isForbidden());
        }
        // Mesma resposta de uma solicitação que realmente não existe.
        mvc.perform(como("MORADOR", maria, get(LISTA + "/999999"))).andExpect(status().isNotFound());
    }

    @Test
    void historicoVemEmOrdemComDataResponsavelEMotivo() throws Exception {
        mvc.perform(como("MORADOR", maria, put(LISTA + "/" + mariaNoA + "/dados"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"versao\":0,\"enderecoId\":"
                                + enderecoDaMaria + ",\"itens\":[{\"materialId\":" + vidro
                                + ",\"quantidadeEstimada\":5}]}"))
                .andExpect(status().isOk());

        mvc.perform(como("OPERADOR", ana, get(LISTA + "/" + mariaNoA + "/historico")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].situacaoAnterior").doesNotExist())
                .andExpect(jsonPath("$[0].situacaoNova").value("SOLICITADA"))
                .andExpect(jsonPath("$[0].ocorridoEm").exists())
                .andExpect(jsonPath("$[0].responsavelTipo").value("MORADOR"))
                .andExpect(jsonPath("$[0].responsavelId").value(maria))
                .andExpect(jsonPath("$[1].situacaoAnterior").value("SOLICITADA"))
                .andExpect(jsonPath("$[1].motivo", containsString("Vidro 2 KG")));
    }

    @Test
    void detalheContinuaMostrandoACopiaDepoisDeMudarOCadastro() throws Exception {
        mvc.perform(put("/api/v1/moradores/" + maria + "/enderecos/" + enderecoDaMaria)
                .contentType(MediaType.APPLICATION_JSON).content(endereco("Avenida Nova"))).andExpect(status().isOk());
        mvc.perform(put("/api/v1/materiais/" + vidro).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Vidro\",\"unidade\":\"LITRO\"}")).andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        mvc.perform(como("MORADOR", maria, get(LISTA + "/" + mariaNoA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endereco.logradouro").value("Rua das Flores"))
                .andExpect(jsonPath("$.itens[0].unidade").value("KG"));
    }

    private static MockHttpServletRequestBuilder como(String tipo, long id, MockHttpServletRequestBuilder requisicao) {
        return requisicao.header("X-Ator-Tipo", tipo).header("X-Ator-Id", id);
    }

    private static MockHttpServletRequestBuilder comoAdmin(MockHttpServletRequestBuilder requisicao) {
        return requisicao.header("X-Ator-Tipo", "ADMINISTRADOR");
    }

    private static String endereco(String logradouro) {
        return "{\"logradouro\":\"" + logradouro + "\",\"numero\":\"200\",\"bairro\":\"Centro\","
                + "\"municipio\":\"Cidade Teste\",\"uf\":\"SE\",\"cep\":\"49000000\"}";
    }

    private static String ponto(String nome) {
        return "{\"nome\":\"" + nome + "\",\"logradouro\":\"Rua A\",\"numero\":\"1\",\"bairro\":\"Centro\","
                + "\"municipio\":\"Cidade Teste\",\"uf\":\"SE\",\"cep\":\"49000000\",\"recolhimentoDomiciliar\":true}";
    }

    private long cadastrar(String caminho, String corpo) throws Exception {
        return idDe(mvc.perform(post(caminho).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private long cadastrarOperador(String email, Long ponto) throws Exception {
        long operador = idDe(mvc.perform(comoAdmin(post("/api/v1/operadores")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Operador Ficticio\",\"email\":\"" + email + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        if (ponto != null) {
            mvc.perform(comoAdmin(put("/api/v1/operadores/" + operador + "/pontos/" + ponto)))
                    .andExpect(status().isCreated());
        }
        return operador;
    }

    private long solicitar(long morador, long endereco, long ponto) throws Exception {
        return idDe(mvc.perform(como("MORADOR", morador, post(LISTA)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enderecoId\":" + endereco + ",\"pontoId\":" + ponto + ",\"itens\":[{\"materialId\":"
                                + vidro + ",\"quantidadeEstimada\":2}]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private static long idDe(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
