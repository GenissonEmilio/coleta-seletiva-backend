package br.edu.ifs.web1.coletaseletiva.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** Operadores, vínculos e contexto de demonstração (API-09) com o banco real; dados desfeitos ao fim. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OperadorIntegracaoTest {

    private static final String ANA = "{\"nome\":\"Ana Operadora\",\"email\":\"ana@exemplo.org\"}";
    private static final String BRUNO = "{\"nome\":\"Bruno Operador\",\"email\":\"bruno@exemplo.org\"}";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void administradorCadastraConsultaEAtualizaOperador() throws Exception {
        String criado = mvc.perform(comoAdmin(post("/api/v1/operadores")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana Operadora\",\"email\":\"  ANA@Exemplo.org \"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.email").value("ana@exemplo.org"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.pontos").isEmpty())
                .andReturn().getResponse().getContentAsString();
        long id = idDe(criado);

        mvc.perform(comoAdmin(put("/api/v1/operadores/" + id)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana Souza\",\"email\":\"ana@exemplo.org\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Souza"));
        mvc.perform(comoAdmin(get("/api/v1/operadores")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(comoAdmin(get("/api/v1/operadores/999999"))).andExpect(status().isNotFound());
    }

    @Test
    void emailRepetidoEEntradaInvalidaSaoRejeitados() throws Exception {
        cadastrarOperador(ANA);

        mvc.perform(comoAdmin(post("/api/v1/operadores")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Outra Ana\",\"email\":\"ANA@exemplo.org\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DUPLICIDADE"));
        mvc.perform(comoAdmin(post("/api/v1/operadores")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"email\":\"sem-arroba\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ENTRADA_INVALIDA"));
    }

    @Test
    void vinculoRepetidoNaoDuplicaEConsultaInformaOsPontos() throws Exception {
        long ana = cadastrarOperador(ANA);
        long pontoA = cadastrarPonto("Ecoponto A");
        long pontoB = cadastrarPonto("Ecoponto B");

        mvc.perform(comoAdmin(put("/api/v1/operadores/" + ana + "/pontos/" + pontoA)))
                .andExpect(status().isCreated());
        mvc.perform(comoAdmin(put("/api/v1/operadores/" + ana + "/pontos/" + pontoA)))
                .andExpect(status().isOk());
        mvc.perform(comoAdmin(put("/api/v1/operadores/" + ana + "/pontos/" + pontoB)))
                .andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM operador_ponto WHERE operador_id = ?", Long.class, ana))
                .isEqualTo(2);
        mvc.perform(comoAdmin(get("/api/v1/operadores/" + ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pontos.length()").value(2))
                .andExpect(jsonPath("$.pontos[0].id").value(pontoA))
                .andExpect(jsonPath("$.pontos[0].nome").value("Ecoponto A"))
                .andExpect(jsonPath("$.pontos[1].id").value(pontoB));
    }

    @Test
    void desvincularRemoveEVinculoOuPontoInexistenteResponde404() throws Exception {
        long ana = cadastrarOperador(ANA);
        long pontoA = cadastrarPonto("Ecoponto A");
        mvc.perform(comoAdmin(put("/api/v1/operadores/" + ana + "/pontos/" + pontoA)))
                .andExpect(status().isCreated());

        mvc.perform(comoAdmin(delete("/api/v1/operadores/" + ana + "/pontos/" + pontoA)))
                .andExpect(status().isNoContent());
        mvc.perform(comoAdmin(delete("/api/v1/operadores/" + ana + "/pontos/" + pontoA)))
                .andExpect(status().isNotFound());
        mvc.perform(comoAdmin(put("/api/v1/operadores/" + ana + "/pontos/999999")))
                .andExpect(status().isNotFound());
        mvc.perform(comoAdmin(get("/api/v1/operadores/" + ana))).andExpect(jsonPath("$.pontos").isEmpty());
    }

    @Test
    void atorAusenteDesconhecidoOuDeTipoInvalidoResponde403() throws Exception {
        mvc.perform(get("/api/v1/operadores"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        mvc.perform(get("/api/v1/contexto").header("X-Ator-Tipo", "OPERADOR").header("X-Ator-Id", "999999"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        mvc.perform(get("/api/v1/contexto").header("X-Ator-Tipo", "OPERADOR"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/contexto").header("X-Ator-Tipo", "GERENTE"))
                .andExpect(status().isForbidden());
    }

    @Test
    void operadorSoConsultaASiMesmoENaoAdministraCadastros() throws Exception {
        long ana = cadastrarOperador(ANA);
        long bruno = cadastrarOperador(BRUNO);

        mvc.perform(comoOperador(get("/api/v1/operadores/" + ana), ana)).andExpect(status().isOk());
        mvc.perform(comoOperador(get("/api/v1/operadores/" + bruno), ana))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        mvc.perform(comoOperador(get("/api/v1/operadores"), ana)).andExpect(status().isForbidden());
        mvc.perform(comoOperador(post("/api/v1/operadores"), ana).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Carla\",\"email\":\"carla@exemplo.org\"}")).andExpect(status().isForbidden());
        mvc.perform(comoOperador(put("/api/v1/operadores/" + ana + "/pontos/1"), ana))
                .andExpect(status().isForbidden());
    }

    @Test
    void contextoDistingueOsTiposDeAtorEInformaOsPontosDoOperador() throws Exception {
        long ana = cadastrarOperador(ANA);
        long pontoA = cadastrarPonto("Ecoponto A");
        mvc.perform(comoAdmin(put("/api/v1/operadores/" + ana + "/pontos/" + pontoA)))
                .andExpect(status().isCreated());
        long maria = idDe(mvc.perform(post("/api/v1/moradores").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria Ficticia\",\"email\":\"maria@exemplo.com\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());

        mvc.perform(comoAdmin(get("/api/v1/contexto")))
                .andExpect(jsonPath("$.tipo").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.id").doesNotExist());
        mvc.perform(comoOperador(get("/api/v1/contexto"), ana))
                .andExpect(jsonPath("$.tipo").value("OPERADOR"))
                .andExpect(jsonPath("$.nome").value("Ana Operadora"))
                .andExpect(jsonPath("$.pontoIds[0]").value(pontoA));
        mvc.perform(get("/api/v1/contexto").header("X-Ator-Tipo", "morador").header("X-Ator-Id", maria))
                .andExpect(jsonPath("$.tipo").value("MORADOR"))
                .andExpect(jsonPath("$.id").value(maria))
                .andExpect(jsonPath("$.pontoIds").isEmpty());
    }

    @Test
    void rotaDeAtoresDemoNaoExisteForaDoPerfilDemo() throws Exception {
        mvc.perform(get("/api/v1/demo/atores"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("ROTA_NAO_ENCONTRADA"));
    }

    private static MockHttpServletRequestBuilder comoAdmin(MockHttpServletRequestBuilder requisicao) {
        return requisicao.header("X-Ator-Tipo", "ADMINISTRADOR");
    }

    private static MockHttpServletRequestBuilder comoOperador(MockHttpServletRequestBuilder requisicao, long id) {
        return requisicao.header("X-Ator-Tipo", "OPERADOR").header("X-Ator-Id", id);
    }

    private long cadastrarOperador(String corpo) throws Exception {
        return idDe(mvc.perform(comoAdmin(post("/api/v1/operadores")).contentType(MediaType.APPLICATION_JSON)
                .content(corpo)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private long cadastrarPonto(String nome) throws Exception {
        String corpo = "{\"nome\":\"" + nome + "\",\"logradouro\":\"Rua das Flores\",\"numero\":\"120\","
                + "\"bairro\":\"Centro\",\"municipio\":\"Cidade Teste\",\"uf\":\"SE\",\"cep\":\"49000000\","
                + "\"recolhimentoDomiciliar\":true}";
        return idDe(mvc.perform(post("/api/v1/pontos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private static long idDe(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
