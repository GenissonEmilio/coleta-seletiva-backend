package br.edu.ifs.web1.coletaseletiva.services.morador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import br.edu.ifs.web1.coletaseletiva.dto.morador.MoradorRequest;
import br.edu.ifs.web1.coletaseletiva.dto.morador.MoradorResponse;
import br.edu.ifs.web1.coletaseletiva.exception.CodigoErro;
import br.edu.ifs.web1.coletaseletiva.exception.RegraNegocioException;
import br.edu.ifs.web1.coletaseletiva.model.MoradorModel;
import br.edu.ifs.web1.coletaseletiva.repository.MoradorRepository;

@ExtendWith(MockitoExtension.class)
class MoradorServiceTest {

    @Mock
    private MoradorRepository moradorRepository;

    @InjectMocks
    private MoradorService service;

    @Test
    void cadastraMoradorValido() {
        when(moradorRepository.existsByEmail("maria@exemplo.com")).thenReturn(false);
        when(moradorRepository.saveAndFlush(any(MoradorModel.class)))
                .thenAnswer(invocacao -> comId(invocacao.getArgument(0), 1L));

        MoradorResponse resposta = service.cadastrar(new MoradorRequest("Maria Souza", "maria@exemplo.com", null));

        assertThat(resposta.id()).isEqualTo(1L);
        assertThat(resposta.email()).isEqualTo("maria@exemplo.com");
        assertThat(resposta.ativo()).isTrue();
    }

    @Test
    void rejeitaEmailRepetidoMesmoComCaixaEEspacosDiferentes() {
        when(moradorRepository.existsByEmail("maria@exemplo.com")).thenReturn(true);

        MoradorRequest request = new MoradorRequest("Outra Maria", "  Maria@Exemplo.COM ", null);

        assertThatThrownBy(() -> service.cadastrar(request))
                .isInstanceOfSatisfying(RegraNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoErro.DUPLICIDADE));
        verify(moradorRepository, never()).saveAndFlush(any());
    }

    @Test
    void atualizarSemTrocarOEmailNaoContaOProprioMoradorComoDuplicado() {
        MoradorModel existente = morador(1L, "maria@exemplo.com");
        when(moradorRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(moradorRepository.findByEmail("maria@exemplo.com")).thenReturn(Optional.of(existente));
        when(moradorRepository.saveAndFlush(existente)).thenReturn(existente);

        MoradorResponse resposta = service.atualizar(1L, new MoradorRequest("Maria Nova", "maria@exemplo.com", null));

        assertThat(resposta.nome()).isEqualTo("Maria Nova");
    }

    @Test
    void rejeitaAtualizarComEmailDeOutroMorador() {
        when(moradorRepository.findById(1L)).thenReturn(Optional.of(morador(1L, "maria@exemplo.com")));
        when(moradorRepository.findByEmail("joao@exemplo.com")).thenReturn(Optional.of(morador(2L, "joao@exemplo.com")));

        MoradorRequest request = new MoradorRequest("Maria Souza", "joao@exemplo.com", null);

        assertThatThrownBy(() -> service.atualizar(1L, request))
                .isInstanceOfSatisfying(RegraNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo(CodigoErro.DUPLICIDADE));
        verify(moradorRepository, never()).saveAndFlush(any());
    }

    private MoradorModel morador(Long id, String email) {
        MoradorModel morador = new MoradorModel();
        morador.setNome("Morador Teste");
        morador.setEmail(email);
        return comId(morador, id);
    }

    private MoradorModel comId(MoradorModel morador, Long id) {
        ReflectionTestUtils.setField(morador, "id", id);
        return morador;
    }
}
