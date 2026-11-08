package br.edu.ifs.web1.coletaseletiva.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import br.edu.ifs.web1.coletaseletiva.exception.RegraNegocioException;
import br.edu.ifs.web1.coletaseletiva.services.morador.MoradorService;

@WebMvcTest(controllers = MoradorController.class)
@Import(GlobalExceptionHandler.class)
class MoradorControllerTest {

    private static final String CADASTRO_VALIDO = "{\"nome\":\"Maria Souza\",\"email\":\"maria@exemplo.com\"}";

    private static final MoradorRequest REQUEST_VALIDO = new MoradorRequest("Maria Souza", "maria@exemplo.com", null);

    @Autowired
    MockMvc mvc;

    @MockitoBean
    MoradorService moradorService;

    @Test
    void cadastroValidoRetorna201ComLocation() throws Exception {
        when(moradorService.cadastrar(REQUEST_VALIDO))
                .thenReturn(new MoradorResponse(1L, "Maria Souza", "maria@exemplo.com", null, true));

        mvc.perform(post("/api/v1/moradores").contentType(MediaType.APPLICATION_JSON).content(CADASTRO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/moradores/1"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void nomeAusenteRetorna400ApontandoOCampo() throws Exception {
        mvc.perform(post("/api/v1/moradores").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria@exemplo.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[0].campo").value("nome"));
    }

    @Test
    void emailDuplicadoRetorna409() throws Exception {
        when(moradorService.cadastrar(REQUEST_VALIDO))
                .thenThrow(new RegraNegocioException(CodigoErro.DUPLICIDADE, "Já existe um morador com este e-mail."));

        mvc.perform(post("/api/v1/moradores").contentType(MediaType.APPLICATION_JSON).content(CADASTRO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DUPLICIDADE"));
    }
}
