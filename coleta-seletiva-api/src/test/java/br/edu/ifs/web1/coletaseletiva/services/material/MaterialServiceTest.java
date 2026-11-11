package br.edu.ifs.web1.coletaseletiva.services.material;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import br.edu.ifs.web1.coletaseletiva.dto.material.MaterialRequest;
import br.edu.ifs.web1.coletaseletiva.exception.CodigoErro;
import br.edu.ifs.web1.coletaseletiva.exception.RegraNegocioException;
import br.edu.ifs.web1.coletaseletiva.model.UnidadeMaterial;
import br.edu.ifs.web1.coletaseletiva.repository.MaterialRepository;
import br.edu.ifs.web1.coletaseletiva.services.paginacao.PaginacaoService;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {

    @Mock
    MaterialRepository materialRepository;

    @Mock
    PaginacaoService paginacaoService;

    @InjectMocks
    MaterialService service;

    @ParameterizedTest
    @ValueSource(strings = {"material_nome_key", "idx_material_nome_normalizado"})
    void traduzDuplicataDetectadaPeloBancoAposAChecagemDoNome(String restricao) {
        when(materialRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("Banco",
                new ConstraintViolationException("Nome duplicado", new SQLException("UNIQUE", "23505"), restricao)));

        assertThatThrownBy(() -> service.cadastrar(new MaterialRequest("Papel", null, UnidadeMaterial.KG, null)))
                .isInstanceOfSatisfying(RegraNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoErro.DUPLICIDADE));
    }

    @Test
    void naoConfundeOutraFalhaDeIntegridadeComNomeDuplicado() {
        DataIntegrityViolationException falha = new DataIntegrityViolationException("Outra restrição");
        when(materialRepository.saveAndFlush(any())).thenThrow(falha);

        assertThatThrownBy(() -> service.cadastrar(new MaterialRequest("Papel", null, UnidadeMaterial.KG, null)))
                .isSameAs(falha);
    }
}
