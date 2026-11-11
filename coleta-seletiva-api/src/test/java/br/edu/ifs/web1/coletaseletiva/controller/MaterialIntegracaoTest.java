package br.edu.ifs.web1.coletaseletiva.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.stream.Stream;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifs.web1.coletaseletiva.exception.CodigoErro;
import br.edu.ifs.web1.coletaseletiva.exception.RecursoNaoEncontradoException;
import br.edu.ifs.web1.coletaseletiva.exception.RegraNegocioException;
import br.edu.ifs.web1.coletaseletiva.model.UnidadeMaterial;
import br.edu.ifs.web1.coletaseletiva.services.material.MaterialService;

/** HTTP, validação, Service, JPA e migrações reais; cada teste reverte seus próprios dados. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MaterialIntegracaoTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MaterialService materialService;

    @Test
    void cadastraEDetalhaUnidadeDescricaoEPreparo() throws Exception {
        long id = cadastrar(" Papel ", "KG");

        mvc.perform(get("/api/v1/materiais/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Papel"))
                .andExpect(jsonPath("$.descricao").value("Material reciclável"))
                .andExpect(jsonPath("$.unidade").value("KG"))
                .andExpect(jsonPath("$.orientacaoPreparo").value("Separar limpo e seco"))
                .andExpect(jsonPath("$.ativo").value(true));
        assertThat(materialService.buscarAtivoParaSolicitacao(id).getUnidade()).isEqualTo(UnidadeMaterial.KG);
    }

    @Test
    void aceitaCamposOpcionaisAusentes() throws Exception {
        mvc.perform(post("/api/v1/materiais").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Garrafas\",\"unidade\":\"UNIDADE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unidade").value("UNIDADE"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void rejeitaNomeRepetidoMesmoInativoComCaixaEEspacosDiferentes() throws Exception {
        long id = cadastrar("Papel", "KG");
        alterarSituacao(id, false);

        mvc.perform(post("/api/v1/materiais").contentType(MediaType.APPLICATION_JSON)
                        .content(dados("  PAPEL  ", "LITRO")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DUPLICIDADE"));
    }

    @Test
    void atualizaOMesmoNomeMasRejeitaNomeDeOutroMaterialSemGravacaoParcial() throws Exception {
        long papel = cadastrar("Papel", "KG");
        cadastrar("Vidro", "UNIDADE");

        mvc.perform(put("/api/v1/materiais/" + papel).contentType(MediaType.APPLICATION_JSON)
                        .content(dados("PAPEL", "LITRO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unidade").value("LITRO"));

        mvc.perform(put("/api/v1/materiais/" + papel).contentType(MediaType.APPLICATION_JSON)
                        .content(dados(" vidro ", "UNIDADE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DUPLICIDADE"));

        mvc.perform(get("/api/v1/materiais/" + papel))
                .andExpect(jsonPath("$.nome").value("PAPEL"))
                .andExpect(jsonPath("$.unidade").value("LITRO"));
    }

    @ParameterizedTest
    @MethodSource("cadastrosInvalidos")
    void rejeitaCamposObrigatoriosNomeLongoEUnidadeForaDoCatalogo(String corpo) throws Exception {
        mvc.perform(post("/api/v1/materiais").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ENTRADA_INVALIDA"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM material", Long.class)).isZero();
    }

    static Stream<String> cadastrosInvalidos() {
        return Stream.of(
                "{\"unidade\":\"KG\"}",
                dados("   ", "KG"),
                dados("x".repeat(81), "KG"),
                "{\"nome\":\"Papel\"}",
                "{\"nome\":\"Papel\",\"unidade\":null}",
                dados("Papel", "TONELADA"),
                dados("Papel", "kg"));
    }

    @Test
    void filtraAntesDePaginarEOrdenaPorNomeSemDistinguirCaixa() throws Exception {
        cadastrar("Zinco", "KG");
        long aluminio = cadastrar("alumínio", "KG");
        cadastrar("Papel", "KG");
        alterarSituacao(aluminio, false);

        mvc.perform(get("/api/v1/materiais").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nome").value("alumínio"))
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/v1/materiais").param("ativo", "true").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nome").value("Zinco"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/v1/materiais").param("ativo", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(aluminio))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/materiais").param("page", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void rejeitaParametrosInvalidosComOCampoNoErro() throws Exception {
        mvc.perform(get("/api/v1/materiais").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[0].campo").value("size"));
        mvc.perform(get("/api/v1/materiais").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[0].campo").value("page"));
        mvc.perform(get("/api/v1/materiais").param("ativo", "talvez"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[0].campo").value("ativo"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"ativo\":null}"})
    void exigeSituacaoExplicitaSemInativarPorAcidente(String corpo) throws Exception {
        long id = cadastrar("Papel", "KG");
        mvc.perform(patch("/api/v1/materiais/" + id + "/situacao").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errosCampos[0].campo").value("ativo"));
        assertThat(materialService.buscar(id).ativo()).isTrue();
    }

    @Test
    void inativacaoEReativacaoSaoIdempotentesEValidacaoDeNovaSolicitacaoRespeitaSituacao() throws Exception {
        long id = cadastrar("Papel", "KG");
        alterarSituacao(id, false);
        alterarSituacao(id, false);

        assertThatThrownBy(() -> materialService.buscarAtivoParaSolicitacao(id))
                .isInstanceOfSatisfying(RegraNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoErro.MATERIAL_INCOMPATIVEL));

        alterarSituacao(id, true);
        alterarSituacao(id, true);
        assertThat(materialService.buscarAtivoParaSolicitacao(id).isAtivo()).isTrue();
    }

    @Test
    void registroInexistenteResponde404() throws Exception {
        mvc.perform(get("/api/v1/materiais/99999999")).andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/materiais/99999999").contentType(MediaType.APPLICATION_JSON)
                        .content(dados("Papel", "KG")))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/materiais/99999999/situacao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\":false}"))
                .andExpect(status().isNotFound());
        assertThatThrownBy(() -> materialService.buscarAtivoParaSolicitacao(99999999L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void bancoImpedeDuplicataNormalizadaSemPassarPeloService() {
        jdbc.update("INSERT INTO material (nome, unidade) VALUES ('Papel', 'KG')");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO material (nome, unidade) VALUES (' PAPEL ', 'LITRO')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void inativacaoEEdicaoPreservamVinculosEUnidadeCopiadaNaSolicitacao() throws Exception {
        long materialId = cadastrar("Papel", "KG");
        long solicitacaoId = criarSolicitacaoComMaterial(materialId);
        alterarSituacao(materialId, false);

        mvc.perform(put("/api/v1/materiais/" + materialId).contentType(MediaType.APPLICATION_JSON)
                        .content(dados("Papel atualizado", "UNIDADE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false))
                .andExpect(jsonPath("$.unidade").value("UNIDADE"));
        assertThat(jdbc.queryForObject("SELECT unidade FROM item_solicitacao WHERE solicitacao_id = ?",
                String.class, solicitacaoId)).isEqualTo("KG");
        assertThat(jdbc.queryForObject("SELECT quantidade_estimada FROM item_solicitacao WHERE solicitacao_id = ?",
                BigDecimal.class, solicitacaoId)).isEqualByComparingTo("2.5");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ponto_material WHERE material_id = ?",
                Long.class, materialId)).isEqualTo(1);

        mvc.perform(delete("/api/v1/materiais/" + materialId)).andExpect(status().isMethodNotAllowed());
        assertThat(materialService.buscar(materialId).ativo()).isFalse();
    }

    private long criarSolicitacaoComMaterial(long materialId) {
        long morador = jdbc.queryForObject("""
                INSERT INTO morador (nome, email) VALUES ('Morador Teste', 'material@exemplo.com') RETURNING id
                """, Long.class);
        long endereco = jdbc.queryForObject("""
                INSERT INTO endereco (morador_id, logradouro, numero, bairro, municipio, uf, cep)
                VALUES (?, 'Rua Teste', '10', 'Centro', 'Cidade Teste', 'SE', '49000000') RETURNING id
                """, Long.class, morador);
        long ponto = jdbc.queryForObject("""
                INSERT INTO ponto_coleta (nome, logradouro, numero, bairro, municipio, uf, cep, recolhimento_domiciliar)
                VALUES ('Ponto Teste', 'Rua Teste', '20', 'Centro', 'Cidade Teste', 'SE', '49000000', true) RETURNING id
                """, Long.class);
        jdbc.update("INSERT INTO ponto_material (ponto_id, material_id) VALUES (?, ?)", ponto, materialId);
        long solicitacao = jdbc.queryForObject("""
                INSERT INTO solicitacao (protocolo, morador_id, ponto_id, endereco_id, end_logradouro, end_numero,
                    end_bairro, end_municipio, end_uf, end_cep, situacao, criada_em)
                VALUES (?, ?, ?, ?, 'Rua Teste', '10', 'Centro', 'Cidade Teste', 'SE', '49000000',
                    'SOLICITADA', CURRENT_TIMESTAMP) RETURNING id
                """, Long.class, "MAT-" + materialId, morador, ponto, endereco);
        jdbc.update("""
                INSERT INTO item_solicitacao (solicitacao_id, material_id, unidade, quantidade_estimada)
                VALUES (?, ?, 'KG', 2.5)
                """, solicitacao, materialId);
        return solicitacao;
    }

    private long cadastrar(String nome, String unidade) throws Exception {
        String resposta = mvc.perform(post("/api/v1/materiais").contentType(MediaType.APPLICATION_JSON)
                        .content(dados(nome, unidade)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }

    private void alterarSituacao(long id, boolean ativo) throws Exception {
        mvc.perform(patch("/api/v1/materiais/" + id + "/situacao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\":" + ativo + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(ativo));
    }

    private static String dados(String nome, String unidade) {
        return "{\"nome\":\"" + nome + "\",\"descricao\":\"Material reciclável\",\"unidade\":\"" + unidade
                + "\",\"orientacaoPreparo\":\"Separar limpo e seco\"}";
    }
}
