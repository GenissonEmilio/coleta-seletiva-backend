package br.edu.ifs.web1.coletaseletiva.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifs.web1.coletaseletiva.dto.common.PaginaResponse;
import br.edu.ifs.web1.coletaseletiva.dto.common.PaginacaoRequest;
import br.edu.ifs.web1.coletaseletiva.services.paginacao.PaginacaoService;

/** Testa o formato de erro sem banco de dados, com um controller que existe só nos testes. */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.TesteController.class)
@Import({ GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TesteController.class, PaginacaoService.class })
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void entradaInvalidaRetorna400ComErrosPorCampo() throws Exception {
        String corpo = "{\"nome\":\"\",\"itens\":[{\"quantidadeEstimada\":-1}]}";
        mvc.perform(post("/teste/entrada").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ENTRADA_INVALIDA"))
                .andExpect(jsonPath("$.mensagem").value("Revise os campos informados."))
                .andExpect(jsonPath("$.errosCampos", hasSize(2)))
                .andExpect(jsonPath("$.errosCampos[?(@.campo=='nome')]").exists())
                .andExpect(jsonPath("$.errosCampos[?(@.campo=='itens[0].quantidadeEstimada')]").exists());
    }

    @Test
    void mensagensDeValidacaoSaemEmPortugues() throws Exception {
        mvc.perform(post("/teste/entrada").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"itens\":[{\"quantidadeEstimada\":1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[0].mensagem").value("não deve estar em branco"));
    }

    @Test
    void jsonMalformadoRetorna400SemDetalhesDoParser() throws Exception {
        mvc.perform(post("/teste/entrada").contentType(MediaType.APPLICATION_JSON).content("{nome:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ENTRADA_INVALIDA"))
                .andExpect(jsonPath("$.errosCampos", hasSize(0)))
                .andExpect(content().string(not(containsString("Jackson"))));
    }

    @Test
    void registroAusenteRetorna404() throws Exception {
        mvc.perform(get("/teste/ausente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("REGISTRO_NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.errosCampos", hasSize(0)));
    }

    @Test
    void rotaInexistenteRetorna404NoMesmoFormato() throws Exception {
        mvc.perform(get("/teste/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("ROTA_NAO_ENCONTRADA"));
    }

    @Test
    void metodoNaoPermitidoRetorna405NoMesmoFormato() throws Exception {
        mvc.perform(post("/teste/ausente"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.codigo").value("METODO_NAO_PERMITIDO"));
    }

    @Test
    void duplicidadeRetorna409() throws Exception {
        mvc.perform(get("/teste/duplicidade"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DUPLICIDADE"));
    }

    @Test
    void conflitoDeVersaoRetorna409() throws Exception {
        mvc.perform(get("/teste/versao"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_VERSAO"));
    }

    @Test
    void violacaoDeIntegridadeRetorna409SemExporSql() throws Exception {
        mvc.perform(get("/teste/integridade"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGRA_NEGOCIO"))
                .andExpect(content().string(not(containsString("insert into"))))
                .andExpect(content().string(not(containsString("uk_morador_email"))));
    }

    @Test
    void erroInesperadoRetorna500GenericoSemDetalhesInternos() throws Exception {
        mvc.perform(get("/teste/erro"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"))
                .andExpect(content().string(not(containsString("senha"))))
                .andExpect(content().string(not(containsString("SELECT"))))
                .andExpect(content().string(not(containsString("RuntimeException"))))
                .andExpect(content().string(not(containsString("at br.edu"))));
    }

    @Test
    void parametroComTipoInvalidoRetorna400() throws Exception {
        mvc.perform(get("/teste/pagina").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ENTRADA_INVALIDA"))
                .andExpect(jsonPath("$.errosCampos[0].campo").value("page"));
    }

    @Test
    void paginacaoUsaPadroesEMetadados() throws Exception {
        mvc.perform(get("/teste/pagina"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(20)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(45))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void ultimaPaginaTrazOResto() throws Exception {
        mvc.perform(get("/teste/pagina").param("page", "2").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)));
    }

    @Test
    void paginaAlemDoFimMantemTotaisComConteudoVazio() throws Exception {
        mvc.perform(get("/teste/pagina").param("page", "3").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page").value(3))
                .andExpect(jsonPath("$.totalElements").value(45))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void sizeMaximoAceitoE100() throws Exception {
        mvc.perform(get("/teste/pagina").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(45)))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void sizeForaDoIntervaloRetorna400() throws Exception {
        for (String size : new String[] { "0", "101", "-5" }) {
            mvc.perform(get("/teste/pagina").param("size", size))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("ENTRADA_INVALIDA"))
                    .andExpect(jsonPath("$.errosCampos[0].campo").value("size"));
        }
    }

    @Test
    void pageNegativoRetorna400() throws Exception {
        mvc.perform(get("/teste/pagina").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[0].campo").value("page"));
    }

    @Test
    void pageESizeInvalidosJuntosGeramDoisErros() throws Exception {
        mvc.perform(get("/teste/pagina").param("page", "-1").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos", hasSize(2)));
    }

    @RestController
    @RequestMapping("/teste")
    static class TesteController {

        private final PaginacaoService paginacaoService;

        TesteController(PaginacaoService paginacaoService) {
            this.paginacaoService = paginacaoService;
        }

        record ItemTesteRequest(@NotNull @Positive BigDecimal quantidadeEstimada) {
        }

        record EntradaTesteRequest(@NotBlank String nome, @NotEmpty List<@Valid ItemTesteRequest> itens) {
        }

        @PostMapping("/entrada")
        public String entrada(@Valid @RequestBody EntradaTesteRequest entrada) {
            return "ok";
        }

        @GetMapping("/ausente")
        public String ausente() {
            throw new RecursoNaoEncontradoException("Material", 99);
        }

        @GetMapping("/duplicidade")
        public String duplicidade() {
            throw new RegraNegocioException(CodigoErro.DUPLICIDADE, "E-mail já cadastrado.");
        }

        @GetMapping("/versao")
        public String versao() {
            throw new ObjectOptimisticLockingFailureException(Object.class, 1L);
        }

        @GetMapping("/integridade")
        public String integridade() {
            throw new DataIntegrityViolationException(
                    "could not execute statement [insert into morador (email) values (?)]; constraint [uk_morador_email]");
        }

        @GetMapping("/erro")
        public String erro() {
            throw new RuntimeException("senha=123 SELECT * FROM usuario");
        }

        @GetMapping("/pagina")
        public PaginaResponse<Integer> pagina(
                @org.springframework.web.bind.annotation.RequestParam(
                        defaultValue = PaginacaoRequest.PAGE_PADRAO) int page,
                @org.springframework.web.bind.annotation.RequestParam(
                        defaultValue = PaginacaoRequest.SIZE_PADRAO) int size) {
            var pageable = paginacaoService.criarPageable(new PaginacaoRequest(page, size),
                    org.springframework.data.domain.Sort.by("id"));
            List<Integer> todos = java.util.stream.IntStream.range(0, 45).boxed().toList();
            int de = (int) Math.min(pageable.getOffset(), todos.size());
            int ate = Math.min(de + pageable.getPageSize(), todos.size());
            return PaginaResponse
                    .de(new org.springframework.data.domain.PageImpl<>(todos.subList(de, ate), pageable, todos.size()));
        }
    }
}
