package br.edu.ifs.web1.coletaseletiva.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifs.web1.coletaseletiva.model.SituacaoSolicitacao;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IndicadorIntegracaoTest {

    private static final OffsetDateTime INICIO = OffsetDateTime.parse("2026-11-01T03:00:00Z");
    private static final OffsetDateTime FIM = OffsetDateTime.parse("2026-12-01T03:00:00Z");
    private static final String INICIO_COM_OFFSET = "2026-11-01T00:00:00-03:00";
    private static final String FIM_COM_OFFSET = "2026-12-01T00:00:00-03:00";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    private int numeroProtocolo;

    @Test
    void contaSituacoesAtuaisPelaCriacaoComLimitesOpcionais() throws Exception {
        Cenario cenario = cadastrarCenario();
        for (SituacaoSolicitacao situacao : SituacaoSolicitacao.values()) {
            solicitacao(cenario, situacao.name(), INICIO, FIM);
        }
        solicitacao(cenario, "SOLICITADA", INICIO.plusHours(1), null);
        solicitacao(cenario, "AGENDADA", FIM, null);
        solicitacao(cenario, "CONCLUIDA", INICIO.minusDays(1), INICIO);

        mvc.perform(noPeriodo(admin("solicitacoes")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].situacao").value("SOLICITADA"))
                .andExpect(jsonPath("$[0].total").value(2))
                .andExpect(jsonPath("$[1].total").value(1))
                .andExpect(jsonPath("$[2].total").value(1))
                .andExpect(jsonPath("$[3].total").value(1))
                .andExpect(jsonPath("$[4].total").value(1))
                .andExpect(jsonPath("$[5].situacao").value("CONCLUIDA"))
                .andExpect(jsonPath("$[5].total").value(1));
        mvc.perform(admin("solicitacoes").param("inicio", INICIO_COM_OFFSET))
                .andExpect(status().isOk()).andExpect(jsonPath("$[1].total").value(2));
        mvc.perform(admin("solicitacoes").param("fim", FIM_COM_OFFSET))
                .andExpect(status().isOk()).andExpect(jsonPath("$[5].total").value(2));
    }

    @Test
    void somaSomenteEfetivosConcluidosNoPeriodoSeparandoUnidadesCopiadas() throws Exception {
        Cenario cenario = cadastrarCenario();
        long vidro = material("Vidro", "UNIDADE");
        long papel = material("Papel", "UNIDADE");
        long metal = material("Metal", "KG");
        long semEfetivo = material("Outro material", "KG");

        long primeira = solicitacao(cenario, "CONCLUIDA", INICIO.minusDays(1), INICIO);
        item(primeira, vidro, "KG", "2.250");
        item(primeira, papel, "UNIDADE", "3");
        long segunda = solicitacao(cenario, "CONCLUIDA", INICIO, INICIO.plusDays(1));
        item(segunda, vidro, "KG", "3.500");
        item(segunda, metal, "KG", "0");
        item(segunda, semEfetivo, "KG", null);
        item(solicitacao(cenario, "CONCLUIDA", INICIO, INICIO.plusDays(2)), vidro, "LITRO", "1.125");
        item(solicitacao(cenario, "CONCLUIDA", INICIO, FIM), vidro, "KG", "50");
        for (SituacaoSolicitacao situacao : SituacaoSolicitacao.values()) {
            if (situacao != SituacaoSolicitacao.CONCLUIDA) {
                item(solicitacao(cenario, situacao.name(), INICIO, INICIO.plusDays(1)), vidro, "KG", "99");
            }
        }
        jdbc.update("UPDATE material SET ativo = false WHERE id = ?", papel);

        mvc.perform(noPeriodo(admin("recolhimentos")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[0].nomeMaterial").value("Metal"))
                .andExpect(jsonPath("$.content[0].quantidadeEfetiva").value(0))
                .andExpect(jsonPath("$.content[1].nomeMaterial").value("Papel"))
                .andExpect(jsonPath("$.content[1].unidade").value("UNIDADE"))
                .andExpect(jsonPath("$.content[1].quantidadeEfetiva").value(3))
                .andExpect(jsonPath("$.content[2].materialId").value(vidro))
                .andExpect(jsonPath("$.content[2].unidade").value("KG"))
                .andExpect(jsonPath("$.content[2].quantidadeEfetiva").value(5.75))
                .andExpect(jsonPath("$.content[3].materialId").value(vidro))
                .andExpect(jsonPath("$.content[3].unidade").value("LITRO"))
                .andExpect(jsonPath("$.content[3].quantidadeEfetiva").value(1.125))
                .andExpect(jsonPath("$.content[2].quantidadeEstimada").doesNotExist())
                .andExpect(jsonPath("$.content[2].moradorId").doesNotExist());
    }

    @Test
    void paginaContaGruposEmVezDeItensEIgnoraConclusaoSemData() throws Exception {
        Cenario cenario = cadastrarCenario();
        long vidro = material("Vidro", "KG");
        item(solicitacao(cenario, "CONCLUIDA", INICIO, INICIO), vidro, "KG", "2");
        item(solicitacao(cenario, "CONCLUIDA", INICIO, INICIO), vidro, "KG", "3");
        item(solicitacao(cenario, "CONCLUIDA", INICIO, null), vidro, "UNIDADE", "7");

        mvc.perform(admin("recolhimentos").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].quantidadeEfetiva").value(5));

        item(solicitacao(cenario, "CONCLUIDA", INICIO, INICIO), vidro, "LITRO", "4");
        mvc.perform(admin("recolhimentos").param("size", "1").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].unidade").value("LITRO"))
                .andExpect(jsonPath("$.content[0].quantidadeEfetiva").value(4));
    }

    @Test
    void retornaSituacoesZeradasEPaginaVaziaSemDados() throws Exception {
        mvc.perform(admin("solicitacoes")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[*].total", everyItem(is(0))));
        mvc.perform(admin("recolhimentos")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"solicitacoes", "recolhimentos"})
    void exigeAdministradorEValidaPeriodo(String relatorio) throws Exception {
        Cenario cenario = cadastrarCenario();
        long operador = jdbc.queryForObject(
                "INSERT INTO operador (nome, email) VALUES ('Operador de teste', 'operador.indicador@exemplo.com') RETURNING id",
                Long.class);
        String caminho = "/api/v1/indicadores/" + relatorio;
        mvc.perform(get(caminho)).andExpect(status().isForbidden());
        mvc.perform(get(caminho).header("X-Ator-Tipo", "MORADOR").header("X-Ator-Id", cenario.morador()))
                .andExpect(status().isForbidden());
        mvc.perform(get(caminho).header("X-Ator-Tipo", "OPERADOR").header("X-Ator-Id", operador))
                .andExpect(status().isForbidden());
        mvc.perform(admin(relatorio).param("inicio", INICIO_COM_OFFSET).param("fim", INICIO.toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errosCampos[0].campo").value("fim"));
        mvc.perform(admin(relatorio).param("inicio", FIM_COM_OFFSET).param("fim", INICIO_COM_OFFSET))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errosCampos[0].campo").value("fim"));
        mvc.perform(admin(relatorio).param("inicio", "2026-11-01T00:00:00"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errosCampos[0].campo").value("inicio"));
    }

    @Test
    void aplicaLimiteDaPaginacaoDeRecolhimentos() throws Exception {
        mvc.perform(admin("recolhimentos").param("size", "101"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errosCampos[0].campo").value("size"));
    }

    private MockHttpServletRequestBuilder admin(String relatorio) {
        return get("/api/v1/indicadores/" + relatorio).header("X-Ator-Tipo", "ADMINISTRADOR");
    }

    private MockHttpServletRequestBuilder noPeriodo(MockHttpServletRequestBuilder requisicao) {
        return requisicao.param("inicio", INICIO_COM_OFFSET).param("fim", FIM_COM_OFFSET);
    }

    private Cenario cadastrarCenario() {
        long morador = jdbc.queryForObject(
                "INSERT INTO morador (nome, email) VALUES ('Morador de teste', 'indicador@exemplo.com') RETURNING id",
                Long.class);
        long endereco = jdbc.queryForObject("""
                INSERT INTO endereco (morador_id, logradouro, numero, bairro, municipio, uf, cep)
                VALUES (?, 'Rua A', '1', 'Centro', 'Cidade Teste', 'SE', '49000000') RETURNING id
                """, Long.class, morador);
        long ponto = jdbc.queryForObject("""
                INSERT INTO ponto_coleta (nome, logradouro, numero, bairro, municipio, uf, cep, recolhimento_domiciliar)
                VALUES ('Ponto de teste', 'Rua B', '2', 'Centro', 'Cidade Teste', 'SE', '49000000', true) RETURNING id
                """, Long.class);
        return new Cenario(morador, endereco, ponto);
    }

    private long material(String nome, String unidade) {
        return jdbc.queryForObject("INSERT INTO material (nome, unidade) VALUES (?, ?) RETURNING id",
                Long.class, nome, unidade);
    }

    private long solicitacao(Cenario cenario, String situacao, OffsetDateTime criada, OffsetDateTime concluida) {
        return jdbc.queryForObject("""
                INSERT INTO solicitacao (protocolo, morador_id, endereco_id, ponto_id, end_logradouro,
                    end_numero, end_bairro, end_municipio, end_uf, end_cep, situacao, criada_em, concluida_em)
                VALUES (?, ?, ?, ?, 'Rua A', '1', 'Centro', 'Cidade Teste', 'SE', '49000000', ?, ?, ?) RETURNING id
                """, Long.class, "IND-TESTE-" + ++numeroProtocolo, cenario.morador(), cenario.endereco(),
                cenario.ponto(), situacao, criada, concluida);
    }

    private void item(long solicitacao, long material, String unidade, String efetiva) {
        jdbc.update("""
                INSERT INTO item_solicitacao (solicitacao_id, material_id, unidade, quantidade_estimada, quantidade_efetiva)
                VALUES (?, ?, ?, 80, ?)
                """, solicitacao, material, unidade, efetiva == null ? null : new BigDecimal(efetiva));
    }

    private record Cenario(long morador, long endereco, long ponto) {
    }
}
