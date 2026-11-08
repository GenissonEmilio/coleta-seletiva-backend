package br.edu.ifs.web1.coletaseletiva.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import br.edu.ifs.web1.coletaseletiva.exception.EntradaInvalidaException;
import br.edu.ifs.web1.coletaseletiva.exception.GlobalExceptionHandler;
import br.edu.ifs.web1.coletaseletiva.exception.RecursoNaoEncontradoException;
import br.edu.ifs.web1.coletaseletiva.services.endereco.EnderecoService;

@WebMvcTest(controllers = EnderecoController.class)
@Import(GlobalExceptionHandler.class)
class EnderecoControllerTest {

    private static final String ENDERECO_VALIDO = "{\"logradouro\":\"Rua das Flores\",\"numero\":\"120\",\"semNumero\":false,"
            + "\"bairro\":\"Centro\",\"municipio\":\"Cidade Teste\",\"uf\":\"SE\",\"cep\":\"49000000\"}";

    private static final EnderecoRequest REQUEST_VALIDO =
            new EnderecoRequest("Rua das Flores", "120", false, "Centro", "Cidade Teste", "SE", "49000000", null);

    private static final EnderecoResponse RESPONSE_VALIDO =
            new EnderecoResponse(10L, 1L, "Rua das Flores", "120", false, "Centro", "Cidade Teste", "SE", "49000000", null, true);

    @Autowired
    MockMvc mvc;

    @MockitoBean
    EnderecoService enderecoService;

    @Test
    void cadastroValidoRetorna201ComLocation() throws Exception {
        when(enderecoService.cadastrar(1L, REQUEST_VALIDO)).thenReturn(RESPONSE_VALIDO);

        mvc.perform(post("/api/v1/moradores/1/enderecos").contentType(MediaType.APPLICATION_JSON).content(ENDERECO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/moradores/1/enderecos/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.moradorId").value(1));
    }

    @Test
    void semNumeroAusenteNoJsonValeFalso() throws Exception {
        when(enderecoService.cadastrar(1L, REQUEST_VALIDO)).thenReturn(RESPONSE_VALIDO);

        mvc.perform(post("/api/v1/moradores/1/enderecos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logradouro\":\"Rua das Flores\",\"numero\":\"120\",\"bairro\":\"Centro\","
                                + "\"municipio\":\"Cidade Teste\",\"uf\":\"SE\",\"cep\":\"49000000\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.semNumero").value(false));
    }

    @Test
    void enderecoIncompletoRetorna400ComTodosOsCamposInvalidos() throws Exception {
        mvc.perform(post("/api/v1/moradores/1/enderecos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":\"120\",\"bairro\":\"Centro\",\"uf\":\"SEE\",\"cep\":\"4900\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ENTRADA_INVALIDA"))
                .andExpect(jsonPath("$.errosCampos[?(@.campo=='logradouro')]").exists())
                .andExpect(jsonPath("$.errosCampos[?(@.campo=='municipio')]").exists())
                .andExpect(jsonPath("$.errosCampos[?(@.campo=='uf')]").exists())
                .andExpect(jsonPath("$.errosCampos[?(@.campo=='cep')]").exists());
    }

    @Test
    void regraDoNumeroViolada400ApontaOCampoNumero() throws Exception {
        when(enderecoService.cadastrar(any(), any()))
                .thenThrow(new EntradaInvalidaException("numero", "é obrigatório quando semNumero é falso"));

        mvc.perform(post("/api/v1/moradores/1/enderecos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logradouro\":\"Rua das Flores\",\"semNumero\":false,\"bairro\":\"Centro\","
                                + "\"municipio\":\"Cidade Teste\",\"uf\":\"SE\",\"cep\":\"49000000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos", hasSize(1)))
                .andExpect(jsonPath("$.errosCampos[0].campo").value("numero"));
    }

    @Test
    void cadastroParaMoradorInexistenteRetorna404() throws Exception {
        when(enderecoService.cadastrar(99L, REQUEST_VALIDO)).thenThrow(new RecursoNaoEncontradoException("Morador", 99L));

        mvc.perform(post("/api/v1/moradores/99/enderecos").contentType(MediaType.APPLICATION_JSON).content(ENDERECO_VALIDO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("REGISTRO_NAO_ENCONTRADO"));
    }

    @Test
    void listagemUsaPaginacaoPadraoERetornaOEnvelope() throws Exception {
        when(enderecoService.listar(1L, new PaginacaoRequest(0, 20)))
                .thenReturn(new PaginaResponse<>(List.of(RESPONSE_VALIDO), 0, 20, 1, 1));

        mvc.perform(get("/api/v1/moradores/1/enderecos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].moradorId").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void paginaNaoNumericaRetorna400ApontandoOParametro() throws Exception {
        mvc.perform(get("/api/v1/moradores/1/enderecos").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[0].campo").value("page"));
    }

    @Test
    void atualizacaoValidaRetorna200() throws Exception {
        when(enderecoService.atualizar(1L, 10L, REQUEST_VALIDO)).thenReturn(RESPONSE_VALIDO);

        mvc.perform(put("/api/v1/moradores/1/enderecos/10").contentType(MediaType.APPLICATION_JSON).content(ENDERECO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void atualizarEnderecoDeOutroMoradorRetorna404() throws Exception {
        when(enderecoService.atualizar(2L, 10L, REQUEST_VALIDO)).thenThrow(new RecursoNaoEncontradoException("Endereço", 10L));

        mvc.perform(put("/api/v1/moradores/2/enderecos/10").contentType(MediaType.APPLICATION_JSON).content(ENDERECO_VALIDO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("REGISTRO_NAO_ENCONTRADO"));
    }
}
