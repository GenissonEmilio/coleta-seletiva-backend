package br.edu.ifs.web1.coletaseletiva.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** Percorre os vínculos ponto-material com o banco real, desfazendo os dados ao fim de cada teste. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PontoMaterialIntegracaoTest {

    private static final String PONTO = "{\"nome\":\"Ecoponto Centro\",\"logradouro\":\"Rua das Flores\","
            + "\"numero\":\"120\",\"bairro\":\"Centro\",\"municipio\":\"Cidade Teste\",\"uf\":\"SE\","
            + "\"cep\":\"49000000\",\"recolhimentoDomiciliar\":true}";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void repetirVinculoAtualizaSemCriarSegundaLinha() throws Exception {
        long ponto = cadastrarPonto();
        long vidro = cadastrarMaterial("Vidro", "KG", "Lavar e separar por cor");

        vincular(ponto, vidro, "{\"observacoes\":\"Somente garrafas\",\"limitePorSolicitacao\":10.5}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unidade").value("KG"))
                .andExpect(jsonPath("$.limitePorSolicitacao").value(10.5));
        vincular(ponto, vidro, "{\"observacoes\":\"Garrafas e potes\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observacoes").value("Garrafas e potes"))
                .andExpect(jsonPath("$.limitePorSolicitacao").doesNotExist());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM ponto_material WHERE ponto_id = ?", Long.class, ponto))
                .isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "2.5"})
    void limiteZeroNegativoOuFracionarioParaPecaERejeitado(String limite) throws Exception {
        long ponto = cadastrarPonto();
        long garrafa = cadastrarMaterial("Garrafa PET", "UNIDADE", null);

        vincular(ponto, garrafa, "{\"limitePorSolicitacao\":" + limite + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[0].campo").value("limitePorSolicitacao"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ponto_material", Long.class)).isZero();
    }

    @Test
    void consultaTrazUnidadeOrientacaoObservacoesELimiteOrdenadosPorNome() throws Exception {
        long ponto = cadastrarPonto();
        long vidro = cadastrarMaterial("Vidro", "KG", "Lavar e separar por cor");
        long garrafa = cadastrarMaterial("Garrafa PET", "UNIDADE", "Amassar");
        vincular(ponto, vidro, "{}").andExpect(status().isCreated());
        vincular(ponto, garrafa, "{\"observacoes\":\"Sem tampa\",\"limitePorSolicitacao\":20}")
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/pontos/" + ponto + "/materiais"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].nomeMaterial").value("Garrafa PET"))
                .andExpect(jsonPath("$.content[0].unidade").value("UNIDADE"))
                .andExpect(jsonPath("$.content[0].orientacaoPreparo").value("Amassar"))
                .andExpect(jsonPath("$.content[0].observacoes").value("Sem tampa"))
                .andExpect(jsonPath("$.content[0].limitePorSolicitacao").value(20))
                .andExpect(jsonPath("$.content[1].nomeMaterial").value("Vidro"))
                .andExpect(jsonPath("$.content[1].limitePorSolicitacao").doesNotExist())
                .andExpect(content().string(containsString("\"limitePorSolicitacao\":20}")));
    }

    @Test
    void materialInativoNaoPodeSerVinculado() throws Exception {
        long ponto = cadastrarPonto();
        long vidro = cadastrarMaterial("Vidro", "KG", null);
        mvc.perform(patch("/api/v1/materiais/" + vidro + "/situacao").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ativo\":false}")).andExpect(status().isOk());

        vincular(ponto, vidro, "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("MATERIAL_INCOMPATIVEL"));
    }

    @Test
    void removerApagaOVinculoERepetirResponde404() throws Exception {
        long ponto = cadastrarPonto();
        long vidro = cadastrarMaterial("Vidro", "KG", null);
        vincular(ponto, vidro, "{}").andExpect(status().isCreated());

        mvc.perform(delete("/api/v1/pontos/" + ponto + "/materiais/" + vidro)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/pontos/" + ponto + "/materiais")).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(delete("/api/v1/pontos/" + ponto + "/materiais/" + vidro))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("REGISTRO_NAO_ENCONTRADO"));
    }

    @Test
    void pontoOuMaterialInexistenteResponde404() throws Exception {
        long ponto = cadastrarPonto();
        long vidro = cadastrarMaterial("Vidro", "KG", null);

        vincular(999999, vidro, "{}").andExpect(status().isNotFound());
        vincular(ponto, 999999, "{}").andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/pontos/999999/materiais")).andExpect(status().isNotFound());
    }

    @Test
    void bancoRejeitaVinculoRepetidoSemPassarPeloService() throws Exception {
        long ponto = cadastrarPonto();
        long vidro = cadastrarMaterial("Vidro", "KG", null);
        jdbc.update("INSERT INTO ponto_material (ponto_id, material_id) VALUES (?, ?)", ponto, vidro);

        assertThatThrownBy(() -> jdbc.update("INSERT INTO ponto_material (ponto_id, material_id) VALUES (?, ?)",
                ponto, vidro)).isInstanceOf(DataIntegrityViolationException.class);
    }

    private ResultActions vincular(long ponto, long material, String corpo)
            throws Exception {
        return mvc.perform(put("/api/v1/pontos/" + ponto + "/materiais/" + material)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private long cadastrarPonto() throws Exception {
        return idDe(mvc.perform(post("/api/v1/pontos").contentType(MediaType.APPLICATION_JSON).content(PONTO))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private long cadastrarMaterial(String nome, String unidade, String orientacao) throws Exception {
        String corpo = "{\"nome\":\"" + nome + "\",\"unidade\":\"" + unidade + "\""
                + (orientacao == null ? "" : ",\"orientacaoPreparo\":\"" + orientacao + "\"") + "}";
        return idDe(mvc.perform(post("/api/v1/materiais").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private static long idDe(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
