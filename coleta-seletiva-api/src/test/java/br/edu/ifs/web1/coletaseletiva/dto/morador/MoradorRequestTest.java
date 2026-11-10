package br.edu.ifs.web1.coletaseletiva.dto.morador;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class MoradorRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void aceitaCadastroValidoComTelefoneAusente() {
        assertThat(validator.validate(new MoradorRequest("Maria Souza", "maria@exemplo.com", null))).isEmpty();
    }

    @Test
    void rejeitaNomeEmBrancoEEmailInvalido() {
        assertThat(camposInvalidos(new MoradorRequest("   ", "maria@exemplo.com", null))).containsExactly("nome");
        assertThat(camposInvalidos(new MoradorRequest("Maria Souza", "sem-arroba", null))).containsExactly("email");
    }

    @Test
    void normalizaOEmailAntesDaValidacao() {
        MoradorRequest request = new MoradorRequest("Maria Souza", "  Maria@Exemplo.COM ", null);

        assertThat(request.email()).isEqualTo("maria@exemplo.com");
        assertThat(validator.validate(request)).isEmpty();
    }

    private Set<String> camposInvalidos(MoradorRequest request) {
        return validator.validate(request).stream()
                .map(violacao -> violacao.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
