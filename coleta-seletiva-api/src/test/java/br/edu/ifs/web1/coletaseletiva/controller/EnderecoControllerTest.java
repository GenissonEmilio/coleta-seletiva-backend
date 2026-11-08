package br.edu.ifs.web1.coletaseletiva.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.edu.ifs.web1.coletaseletiva.dto.common.PaginaResponse;
import br.edu.ifs.web1.coletaseletiva.dto.common.PaginacaoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.endereco.EnderecoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.endereco.EnderecoResponse;
import br.edu.ifs.web1.coletaseletiva.exception.GlobalExceptionHandler;
import br.edu.ifs.web1.coletaseletiva.services.endereco.EnderecoService;

@WebMvcTest(controllers = EnderecoController.class)
@Import(GlobalExceptionHandler.class)
class EnderecoControllerTest {

    private static final EnderecoResponse RESPONSE =
            new EnderecoResponse(10L, 1L, "Rua das Flores", "120", false, "Centro", "Cidade Teste", "SE", "49000000", null, true);

    @Autowired
    MockMvc mvc;

    @MockitoBean
    EnderecoService enderecoService;

    @Test
    void cadastroValidoRetorna201ComLocationEAceitaSemNumeroAusente() throws Exception {
        EnderecoRequest request =
                new EnderecoRequest("Rua das Flores", "120", false, "Centro", "Cidade Teste", "SE", "49000000", null);
        when(enderecoService.cadastrar(1L, request)).thenReturn(RESPONSE);

        mvc.perform(post("/api/v1/moradores/1/enderecos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logradouro\":\"Rua das Flores\",\"numero\":\"120\",\"bairro\":\"Centro\","
                                + "\"municipio\":\"Cidade Teste\",\"uf\":\"SE\",\"cep\":\"49000000\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/moradores/1/enderecos/10"));
    }

    @Test
    void enderecoIncompletoRetorna400ComOsCamposInvalidos() throws Exception {
        mvc.perform(post("/api/v1/moradores/1/enderecos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":\"120\",\"bairro\":\"Centro\",\"uf\":\"SEE\",\"cep\":\"4900\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[?(@.campo=='logradouro')]").exists())
                .andExpect(jsonPath("$.errosCampos[?(@.campo=='uf')]").exists())
                .andExpect(jsonPath("$.errosCampos[?(@.campo=='cep')]").exists());
    }

    @Test
    void listagemUsaPaginacaoPadraoERetornaOEnvelope() throws Exception {
        when(enderecoService.listar(1L, new PaginacaoRequest(0, 20)))
                .thenReturn(new PaginaResponse<>(List.of(RESPONSE), 0, 20, 1, 1));

        mvc.perform(get("/api/v1/moradores/1/enderecos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
