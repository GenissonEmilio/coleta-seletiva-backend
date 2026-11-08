package br.edu.ifs.web1.coletaseletiva.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifs.web1.coletaseletiva.model.MoradorModel;
import br.edu.ifs.web1.coletaseletiva.repository.MoradorRepository;

/** Percorre a API inteira (HTTP, Service, JPA e banco real), desfazendo os dados ao fim de cada teste. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MoradorEnderecoIntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    MoradorRepository moradorRepository;

    @Test
    void cadastroValidoPersisteEPodeSerConsultado() throws Exception {
        long id = cadastrarMorador("maria@exemplo.com");

        mvc.perform(get("/api/v1/moradores/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("maria@exemplo.com"));
    }

    @Test
    void emailRepetidoComCaixaEEspacosDiferentesConflita() throws Exception {
        cadastrarMorador("maria@exemplo.com");

        mvc.perform(post("/api/v1/moradores").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Outra Maria\",\"email\":\"  MARIA@Exemplo.com \"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DUPLICIDADE"));
    }

    @Test
    void bancoRejeitaEmailRepetidoMesmoSemPassarPeloService() {
        moradorRepository.saveAndFlush(morador("maria@exemplo.com"));

        assertThatThrownBy(() -> moradorRepository.saveAndFlush(morador("maria@exemplo.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enderecoDeOutroMoradorNaoPodeSerAtualizado() throws Exception {
        long mariaId = cadastrarMorador("maria@exemplo.com");
        long joaoId = cadastrarMorador("joao@exemplo.com");
        String resposta = mvc.perform(post("/api/v1/moradores/" + mariaId + "/enderecos")
                        .contentType(MediaType.APPLICATION_JSON).content(ENDERECO))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long enderecoId = ((Number) JsonPath.read(resposta, "$.id")).longValue();

        mvc.perform(put("/api/v1/moradores/" + joaoId + "/enderecos/" + enderecoId)
                        .contentType(MediaType.APPLICATION_JSON).content(ENDERECO))
                .andExpect(status().isNotFound());
    }

    private static final String ENDERECO = "{\"logradouro\":\"Rua das Flores\",\"numero\":\"10\",\"bairro\":\"Centro\","
            + "\"municipio\":\"Cidade Teste\",\"uf\":\"SE\",\"cep\":\"49000000\"}";

    private long cadastrarMorador(String email) throws Exception {
        String resposta = mvc.perform(post("/api/v1/moradores").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Morador Teste\",\"email\":\"" + email + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }

    private MoradorModel morador(String email) {
        MoradorModel morador = new MoradorModel();
        morador.setNome("Morador Teste");
        morador.setEmail(email);
        
        return morador;
    }
}
