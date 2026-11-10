package br.edu.ifs.web1.coletaseletiva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MigracoesBancoTest {

    private static final String EMAIL = "ana@exemplo.com";

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void aplicaTodasAsMigracoesSemFalha() {
        assertThat(contar("SELECT count(*) FROM flyway_schema_history WHERE NOT success")).isZero();
        assertThat(contar("SELECT count(*) FROM flyway_schema_history WHERE success")).isGreaterThanOrEqualTo(6);
    }

    @Test
    void criaOsIndicesDoDer() {
        assertThat(contar("SELECT count(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname LIKE 'idx_%'"))
                .isGreaterThanOrEqualTo(9);
    }

    @Test
    void rejeitaEmailRepetidoDeMorador() {
        inserirMorador();

        assertRejeitado("INSERT INTO morador (nome, email) VALUES ('Outra Ana', ?)", EMAIL);
    }

    @Test
    void rejeitaApagarMoradorQueTemEndereco() {
        long moradorId = inserirMorador();
        jdbc.update("INSERT INTO endereco (morador_id, logradouro, numero, bairro, municipio, uf, cep) "
                + "VALUES (?, 'Rua A', '10', 'Centro', 'Cidade Teste', 'SE', '49000000')", moradorId);

        assertRejeitado("DELETE FROM morador WHERE id = ?", moradorId);
    }

    @Test
    void rejeitaEnderecoSemNumeroSemMarcarSemNumero() {
        long moradorId = inserirMorador();

        assertRejeitado("INSERT INTO endereco (morador_id, logradouro, bairro, municipio, uf, cep) "
                + "VALUES (?, 'Rua A', 'Centro', 'Cidade Teste', 'SE', '49000000')", moradorId);
    }

    @Test
    void rejeitaMaterialComUnidadeInvalida() {
        assertRejeitado("INSERT INTO material (nome, unidade) VALUES ('Vidro', 'XYZ')");
    }

    private long inserirMorador() {
        return jdbc.queryForObject(
                "INSERT INTO morador (nome, email) VALUES ('Ana Teste', ?) RETURNING id", Long.class, EMAIL);
    }

    private Integer contar(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    private void assertRejeitado(String sql, Object... argumentos) {
        assertThatThrownBy(() -> jdbc.update(sql, argumentos))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

}
