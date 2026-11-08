package br.edu.ifs.web1.coletaseletiva.dto.morador;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class MoradorRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void aceitaCadastroValido() {
        assertThat(validator.validate(new MoradorRequest("Maria Souza", "maria@exemplo.com", "79999990000"))).isEmpty();
    }

    @Test
    void aceitaTelefoneAusente() {
        assertThat(validator.validate(new MoradorRequest("Maria Souza", "maria@exemplo.com", null))).isEmpty();
    }

    @Test
    void rejeitaNomeAusenteOuEmBranco() {
        assertThat(camposInvalidos(new MoradorRequest(null, "maria@exemplo.com", null))).containsExactly("nome");
        assertThat(camposInvalidos(new MoradorRequest("   ", "maria@exemplo.com", null))).containsExactly("nome");
    }

    @Test
    void rejeitaEmailAusenteOuInvalido() {
        assertThat(camposInvalidos(new MoradorRequest("Maria Souza", null, null))).containsExactly("email");
        assertThat(camposInvalidos(new MoradorRequest("Maria Souza", "sem-arroba", null))).containsExactly("email");
    }

    @Test
    void rejeitaCamposMaioresQueOBanco() {
        assertThat(camposInvalidos(new MoradorRequest("n".repeat(121), "maria@exemplo.com", null))).containsExactly("nome");
        assertThat(camposInvalidos(new MoradorRequest("Maria Souza", "maria@exemplo.com", "9".repeat(21))))
                .containsExactly("telefone");
    }

    @Test
    void normalizaOEmailAntesDaValidacao() {
        MoradorRequest request = new MoradorRequest("Maria Souza", "  Maria@Exemplo.COM ", null);

        assertThat(request.email()).isEqualTo("maria@exemplo.com");
        assertThat(validator.validate(request)).isEmpty();
    }

    private Set<String> camposInvalidos(MoradorRequest request) {
        Set<ConstraintViolation<MoradorRequest>> violacoes = validator.validate(request);
        return violacoes.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }
}
