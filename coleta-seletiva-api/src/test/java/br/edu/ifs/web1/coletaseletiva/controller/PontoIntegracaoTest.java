package br.edu.ifs.web1.coletaseletiva.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Percorre a API inteira (HTTP, Service, JPA e banco real), desfazendo os dados ao fim de cada teste. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PontoIntegracaoTest {

    private static final String BASE = "{\"nome\":\"Ecoponto Centro\",\"logradouro\":\"Rua das Flores\","
            + "\"numero\":\"120\",\"bairro\":\"Centro\",\"municipio\":\"Cidade Teste\",\"uf\":\"se\","
            + "\"cep\":\"49000000\",\"horarios\":\"Seg a sex, 8h-17h\"";
    private static final String PONTO = BASE + ",\"recolhimentoDomiciliar\":true}";
    private static final String SEM_RECOLHIMENTO = BASE + "}";

    @Autowired
    MockMvc mvc;

    @Test
    void cadastroValidoPersisteEPodeSerConsultado() throws Exception {
        String resposta = mvc.perform(post("/api/v1/pontos").contentType(MediaType.APPLICATION_JSON).content(PONTO))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(resposta, "$.id")).longValue();

        mvc.perform(get("/api/v1/pontos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ecoponto Centro"))
                .andExpect(jsonPath("$.uf").value("SE"))
                .andExpect(jsonPath("$.horarios").value("Seg a sex, 8h-17h"))
                .andExpect(jsonPath("$.recolhimentoDomiciliar").value(true))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void pontoSemNomeNemEnderecoERejeitado() throws Exception {
        mvc.perform(post("/api/v1/pontos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recolhimentoDomiciliar\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ENTRADA_INVALIDA"))
                .andExpect(jsonPath("$.errosCampos[*].campo")
                        .value(hasItems("nome", "logradouro", "bairro", "municipio", "uf", "cep")));
    }

    @Test
    void recolhimentoDomiciliarAusenteERejeitado() throws Exception {
        mvc.perform(post("/api/v1/pontos").contentType(MediaType.APPLICATION_JSON).content(SEM_RECOLHIMENTO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[0].campo").value("recolhimentoDomiciliar"));
    }

    @Test
    void inativarEReativarPreservaOPonto() throws Exception {
        long id = cadastrarPonto();

        mvc.perform(patch("/api/v1/pontos/" + id + "/situacao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
        mvc.perform(get("/api/v1/pontos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
        mvc.perform(patch("/api/v1/pontos/" + id + "/situacao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\":true}"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void atualizarAlteraOsDadosSemMudarASituacao() throws Exception {
        long id = cadastrarPonto();
        mvc.perform(patch("/api/v1/pontos/" + id + "/situacao").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ativo\":false}"));

        mvc.perform(put("/api/v1/pontos/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(PONTO.replace("Ecoponto Centro", "Ecoponto Norte")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ecoponto Norte"))
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void situacaoSemAtivoERejeitadaSemInativarPorAcidente() throws Exception {
        long id = cadastrarPonto();

        mvc.perform(patch("/api/v1/pontos/" + id + "/situacao").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[0].campo").value("ativo"));
        mvc.perform(get("/api/v1/pontos/" + id))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void listaPontosPorNomeComFiltroDeSituacaoEPaginacao() throws Exception {
        cadastrarPonto("Ecoponto Sul");
        long norte = cadastrarPonto("Ecoponto Norte");
        cadastrarPonto("Ecoponto Centro");
        mvc.perform(patch("/api/v1/pontos/" + norte + "/situacao").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ativo\":false}"));

        mvc.perform(get("/api/v1/pontos").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nome").value("Ecoponto Centro"))
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/v1/pontos").param("ativo", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(norte))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/pontos").param("ativo", "true").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nome").value("Ecoponto Sul"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void pontoInexistenteResponde404() throws Exception {
        mvc.perform(get("/api/v1/pontos/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("REGISTRO_NAO_ENCONTRADO"));
        mvc.perform(patch("/api/v1/pontos/999999/situacao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\":false}"))
                .andExpect(status().isNotFound());
    }

    private long cadastrarPonto() throws Exception {
        return cadastrarPonto("Ecoponto Centro");
    }

    private long cadastrarPonto(String nome) throws Exception {
        String resposta = mvc.perform(post("/api/v1/pontos").contentType(MediaType.APPLICATION_JSON)
                        .content(PONTO.replace("Ecoponto Centro", nome)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }
}
