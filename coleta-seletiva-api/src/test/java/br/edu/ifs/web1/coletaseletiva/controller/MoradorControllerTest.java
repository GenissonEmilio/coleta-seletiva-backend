package br.edu.ifs.web1.coletaseletiva.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.edu.ifs.web1.coletaseletiva.dto.morador.MoradorRequest;
import br.edu.ifs.web1.coletaseletiva.dto.morador.MoradorResponse;
import br.edu.ifs.web1.coletaseletiva.exception.CodigoErro;
import br.edu.ifs.web1.coletaseletiva.exception.GlobalExceptionHandler;
import br.edu.ifs.web1.coletaseletiva.exception.RecursoNaoEncontradoException;
import br.edu.ifs.web1.coletaseletiva.exception.RegraNegocioException;
import br.edu.ifs.web1.coletaseletiva.services.morador.MoradorService;

@WebMvcTest(controllers = MoradorController.class)
@Import(GlobalExceptionHandler.class)
class MoradorControllerTest {

    private static final String CADASTRO_VALIDO = "{\"nome\":\"Maria Souza\",\"email\":\"maria@exemplo.com\",\"telefone\":\"79999990000\"}";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    MoradorService moradorService;

    @Test
    void cadastroValidoRetorna201ComLocation() throws Exception {
        when(moradorService.cadastrar(new MoradorRequest("Maria Souza", "maria@exemplo.com", "79999990000")))
                .thenReturn(new MoradorResponse(1L, "Maria Souza", "maria@exemplo.com", "79999990000", true));

        mvc.perform(post("/api/v1/moradores").contentType(MediaType.APPLICATION_JSON).content(CADASTRO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/moradores/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("maria@exemplo.com"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void emailComCaixaEEspacosChegaNormalizadoAoService() throws Exception {
        when(moradorService.cadastrar(new MoradorRequest("Maria Souza", "maria@exemplo.com", null)))
                .thenReturn(new MoradorResponse(1L, "Maria Souza", "maria@exemplo.com", null, true));

        mvc.perform(post("/api/v1/moradores").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria Souza\",\"email\":\"  Maria@Exemplo.COM \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("maria@exemplo.com"));
    }

    @Test
    void nomeAusenteRetorna400ApontandoOCampo() throws Exception {
        mvc.perform(post("/api/v1/moradores").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria@exemplo.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ENTRADA_INVALIDA"))
                .andExpect(jsonPath("$.errosCampos", hasSize(1)))
                .andExpect(jsonPath("$.errosCampos[0].campo").value("nome"));
    }

    @Test
    void emailInvalidoRetorna400ApontandoOCampo() throws Exception {
        mvc.perform(post("/api/v1/moradores").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria Souza\",\"email\":\"sem-arroba\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[0].campo").value("email"));
    }

    @Test
    void emailDuplicadoRetorna409() throws Exception {
        when(moradorService.cadastrar(new MoradorRequest("Maria Souza", "maria@exemplo.com", "79999990000")))
                .thenThrow(new RegraNegocioException(CodigoErro.DUPLICIDADE, "Já existe um morador com este e-mail."));

        mvc.perform(post("/api/v1/moradores").contentType(MediaType.APPLICATION_JSON).content(CADASTRO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DUPLICIDADE"));
    }

    @Test
    void consultaMoradorExistenteRetorna200() throws Exception {
        when(moradorService.buscar(1L))
                .thenReturn(new MoradorResponse(1L, "Maria Souza", "maria@exemplo.com", null, true));

        mvc.perform(get("/api/v1/moradores/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Souza"));
    }

    @Test
    void consultaMoradorInexistenteRetorna404() throws Exception {
        when(moradorService.buscar(99L)).thenThrow(new RecursoNaoEncontradoException("Morador", 99L));

        mvc.perform(get("/api/v1/moradores/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("REGISTRO_NAO_ENCONTRADO"));
    }

    @Test
    void atualizacaoValidaRetorna200() throws Exception {
        when(moradorService.atualizar(1L, new MoradorRequest("Maria Souza", "maria@exemplo.com", "79999990000")))
                .thenReturn(new MoradorResponse(1L, "Maria Souza", "maria@exemplo.com", "79999990000", true));

        mvc.perform(put("/api/v1/moradores/1").contentType(MediaType.APPLICATION_JSON).content(CADASTRO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefone").value("79999990000"));
    }

    @Test
    void idNaoNumericoRetorna400() throws Exception {
        mvc.perform(get("/api/v1/moradores/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ENTRADA_INVALIDA"));
    }
}
