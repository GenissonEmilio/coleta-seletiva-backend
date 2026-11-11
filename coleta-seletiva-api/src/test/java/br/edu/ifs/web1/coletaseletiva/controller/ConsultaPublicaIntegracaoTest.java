package br.edu.ifs.web1.coletaseletiva.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Consulta pública (API-08) com o banco real, desfazendo os dados ao fim de cada teste. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConsultaPublicaIntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Test
    void filtroPorVidroRetornaSomentePontosAtivosQueOAceitam() throws Exception {
        long vidro = cadastrarMaterial("Vidro");
        long papel = cadastrarMaterial("Papel");
        long comVidro = cadastrarPonto("Ecoponto Centro", "Cidade Teste", "Centro", true);
        long inativoComVidro = cadastrarPonto("Ecoponto Fechado", "Cidade Teste", "Centro", true);
        long soPapel = cadastrarPonto("Ecoponto Papel", "Cidade Teste", "Centro", true);
        vincular(comVidro, vidro);
        vincular(inativoComVidro, vidro);
        vincular(soPapel, papel);
        inativarPonto(inativoComVidro);

        mvc.perform(get("/api/v1/publico/pontos").param("materialId", String.valueOf(vidro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(comVidro));
    }

    @Test
    void filtrosCombinadosSaoRespeitadosSemDiferenciarMaiusculas() throws Exception {
        long vidro = cadastrarMaterial("Vidro");
        long certo = cadastrarPonto("Ecoponto Centro", "Cidade Teste", "Centro", true);
        long outroBairro = cadastrarPonto("Ecoponto Norte", "Cidade Teste", "Norte", true);
        long outraCidade = cadastrarPonto("Ecoponto Vizinho", "Outra Cidade", "Centro", true);
        vincular(certo, vidro);
        vincular(outroBairro, vidro);
        vincular(outraCidade, vidro);

        mvc.perform(get("/api/v1/publico/pontos").param("municipio", "cidade teste").param("bairro", " CENTRO ")
                        .param("materialId", String.valueOf(vidro)).param("nome", "eco"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(certo));
    }

    @Test
    void paginacaoNaoDuplicaNemOmitePontosComVariosMateriais() throws Exception {
        long vidro = cadastrarMaterial("Vidro");
        long papel = cadastrarMaterial("Papel");
        List<Long> cadastrados = new ArrayList<>();
        for (String nome : List.of("Ponto A", "Ponto B", "Ponto C")) {
            long ponto = cadastrarPonto(nome, "Cidade Teste", "Centro", true);
            vincular(ponto, vidro);
            vincular(ponto, papel);
            cadastrados.add(ponto);
        }

        List<Long> vistos = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            String json = mvc.perform(get("/api/v1/publico/pontos").param("page", String.valueOf(page))
                            .param("size", "1"))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andReturn().getResponse().getContentAsString();
            vistos.add(((Number) JsonPath.read(json, "$.content[0].id")).longValue());
        }
        assertThat(vistos).containsExactlyElementsOf(cadastrados);

        mvc.perform(get("/api/v1/publico/pontos").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3));
        mvc.perform(get("/api/v1/publico/pontos").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[0].campo").value("size"));
    }

    @Test
    void detalheMostraMateriaisAtivosEIdentificaPontoSemDomiciliar() throws Exception {
        long vidro = cadastrarMaterial("Vidro");
        long papel = cadastrarMaterial("Papel");
        long ponto = cadastrarPonto("Ecoponto Centro", "Cidade Teste", "Centro", false);
        vincular(ponto, vidro);
        vincular(ponto, papel);
        mvc.perform(patch("/api/v1/materiais/" + papel + "/situacao").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ativo\":false}")).andExpect(status().isOk());

        mvc.perform(get("/api/v1/publico/pontos/" + ponto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ponto.recolhimentoDomiciliar").value(false))
                .andExpect(jsonPath("$.ponto.contato").value("ecoponto@exemplo.org"))
                .andExpect(jsonPath("$.ponto.horarios").value("Seg a sex, 8h às 17h"))
                .andExpect(jsonPath("$.materiais.length()").value(1))
                .andExpect(jsonPath("$.materiais[0].nomeMaterial").value("Vidro"))
                .andExpect(jsonPath("$.materiais[0].unidade").value("KG"))
                .andExpect(jsonPath("$.materiais[0].orientacaoPreparo").value("Lavar"));
    }

    @Test
    void pontoInativoNaoApareceNoDetalhePublico() throws Exception {
        long ponto = cadastrarPonto("Ecoponto Fechado", "Cidade Teste", "Centro", true);
        inativarPonto(ponto);

        mvc.perform(get("/api/v1/publico/pontos/" + ponto))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("REGISTRO_NAO_ENCONTRADO"));
    }

    @Test
    void respostasPublicasNaoTrazemDadosDeMoradores() throws Exception {
        long vidro = cadastrarMaterial("Vidro");
        long ponto = cadastrarPonto("Ecoponto Centro", "Cidade Teste", "Centro", true);
        vincular(ponto, vidro);
        mvc.perform(post("/api/v1/moradores").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria Ficticia\",\"email\":\"maria@exemplo.com\"}"))
                .andExpect(status().isCreated());

        for (String caminho : List.of("/api/v1/publico/pontos", "/api/v1/publico/pontos/" + ponto)) {
            mvc.perform(get(caminho))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("maria@exemplo.com"))))
                    .andExpect(content().string(not(containsString("Maria Ficticia"))))
                    .andExpect(content().string(not(containsString("\"ativo\""))));
        }
    }

    @Test
    void listaPublicaDeMateriaisTrazSoOsAtivos() throws Exception {
        cadastrarMaterial("Vidro");
        long papel = cadastrarMaterial("Papel");
        mvc.perform(patch("/api/v1/materiais/" + papel + "/situacao").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ativo\":false}")).andExpect(status().isOk());

        mvc.perform(get("/api/v1/publico/materiais"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].nome", containsInAnyOrder("Vidro")));
    }

    private long cadastrarPonto(String nome, String municipio, String bairro, boolean domiciliar)
            throws Exception {
        String corpo = "{\"nome\":\"" + nome + "\",\"logradouro\":\"Rua das Flores\",\"numero\":\"120\","
                + "\"bairro\":\"" + bairro + "\",\"municipio\":\"" + municipio + "\",\"uf\":\"SE\","
                + "\"cep\":\"49000000\",\"contato\":\"ecoponto@exemplo.org\","
                + "\"horarios\":\"Seg a sex, 8h às 17h\",\"recolhimentoDomiciliar\":" + domiciliar + "}";
        return idDe(mvc.perform(post("/api/v1/pontos").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private long cadastrarMaterial(String nome) throws Exception {
        String corpo = "{\"nome\":\"" + nome + "\",\"unidade\":\"KG\",\"orientacaoPreparo\":\"Lavar\"}";
        return idDe(mvc.perform(post("/api/v1/materiais").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private void vincular(long ponto, long material) throws Exception {
        mvc.perform(put("/api/v1/pontos/" + ponto + "/materiais/" + material)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isCreated());
    }

    private void inativarPonto(long ponto) throws Exception {
        mvc.perform(patch("/api/v1/pontos/" + ponto + "/situacao").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ativo\":false}")).andExpect(status().isOk());
    }

    private static long idDe(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
