package br.edu.ifs.web1.coletaseletiva.services.endereco;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import br.edu.ifs.web1.coletaseletiva.dto.common.ErroCampoResponse;
import br.edu.ifs.web1.coletaseletiva.dto.common.PaginaResponse;
import br.edu.ifs.web1.coletaseletiva.dto.common.PaginacaoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.endereco.EnderecoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.endereco.EnderecoResponse;
import br.edu.ifs.web1.coletaseletiva.exception.EntradaInvalidaException;
import br.edu.ifs.web1.coletaseletiva.exception.RecursoNaoEncontradoException;
import br.edu.ifs.web1.coletaseletiva.model.EnderecoModel;
import br.edu.ifs.web1.coletaseletiva.model.MoradorModel;
import br.edu.ifs.web1.coletaseletiva.repository.EnderecoRepository;
import br.edu.ifs.web1.coletaseletiva.repository.MoradorRepository;
import br.edu.ifs.web1.coletaseletiva.services.paginacao.PaginacaoService;

@ExtendWith(MockitoExtension.class)
class EnderecoServiceTest {

    @Mock
    private EnderecoRepository enderecoRepository;

    @Mock
    private MoradorRepository moradorRepository;

    private EnderecoService service;

    @BeforeEach
    void criarService() {
        service = new EnderecoService(enderecoRepository, moradorRepository, new PaginacaoService());
    }

    @Test
    void cadastraEnderecoComNumeroEUfEmMaiusculas() {
        when(moradorRepository.findById(1L)).thenReturn(Optional.of(morador(1L)));
        when(enderecoRepository.save(any(EnderecoModel.class)))
                .thenAnswer(invocacao -> comId(invocacao.getArgument(0), 10L));

        EnderecoResponse resposta = service.cadastrar(1L, request("120", false, "se"));

        assertThat(resposta.id()).isEqualTo(10L);
        assertThat(resposta.moradorId()).isEqualTo(1L);
        assertThat(resposta.numero()).isEqualTo("120");
        assertThat(resposta.semNumero()).isFalse();
        assertThat(resposta.uf()).isEqualTo("SE");
    }

    @Test
    void cadastraEnderecoSemNumero() {
        when(moradorRepository.findById(1L)).thenReturn(Optional.of(morador(1L)));
        when(enderecoRepository.save(any(EnderecoModel.class)))
                .thenAnswer(invocacao -> comId(invocacao.getArgument(0), 10L));

        EnderecoResponse resposta = service.cadastrar(1L, request(null, true, "SE"));

        assertThat(resposta.semNumero()).isTrue();
        assertThat(resposta.numero()).isNull();
    }

    @Test
    void rejeitaSemNumeroVerdadeiroComNumeroPreenchido() {
        assertThatThrownBy(() -> service.cadastrar(1L, request("120", true, "SE")))
                .isInstanceOfSatisfying(EntradaInvalidaException.class,
                        e -> assertThat(e.getErros()).extracting(ErroCampoResponse::campo).containsExactly("numero"));
        verify(enderecoRepository, never()).save(any());
    }

    @Test
    void rejeitaSemNumeroFalsoSemNumeroPreenchido() {
        assertThatThrownBy(() -> service.cadastrar(1L, request("  ", false, "SE")))
                .isInstanceOfSatisfying(EntradaInvalidaException.class,
                        e -> assertThat(e.getErros()).extracting(ErroCampoResponse::campo).containsExactly("numero"));
        verify(enderecoRepository, never()).save(any());
    }

    @Test
    void cadastrarParaMoradorInexistenteLancaNaoEncontrado() {
        when(moradorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cadastrar(99L, request("120", false, "SE")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(enderecoRepository, never()).save(any());
    }

    @Test
    void listaOsEnderecosDoMoradorEmPagina() {
        EnderecoModel endereco = endereco(10L, morador(1L));
        when(moradorRepository.findById(1L)).thenReturn(Optional.of(morador(1L)));
        when(enderecoRepository.findByMoradorId(eq(1L), any(Pageable.class)))
                .thenAnswer(invocacao -> new PageImpl<>(List.of(endereco), invocacao.getArgument(1), 1));

        PaginaResponse<EnderecoResponse> pagina = service.listar(1L, new PaginacaoRequest(0, 20));

        assertThat(pagina.content()).hasSize(1);
        assertThat(pagina.content().get(0).moradorId()).isEqualTo(1L);
        assertThat(pagina.page()).isZero();
        assertThat(pagina.size()).isEqualTo(20);
        assertThat(pagina.totalElements()).isEqualTo(1);
    }

    @Test
    void listarParaMoradorInexistenteLancaNaoEncontrado() {
        when(moradorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listar(99L, new PaginacaoRequest(0, 20)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void atualizaEnderecoDoProprioMorador() {
        EnderecoModel existente = endereco(10L, morador(1L));
        when(moradorRepository.findById(1L)).thenReturn(Optional.of(morador(1L)));
        when(enderecoRepository.findByIdAndMoradorId(10L, 1L)).thenReturn(Optional.of(existente));
        when(enderecoRepository.save(existente)).thenReturn(existente);

        EnderecoResponse resposta = service.atualizar(1L, 10L, request("200", false, "al"));

        assertThat(resposta.numero()).isEqualTo("200");
        assertThat(resposta.uf()).isEqualTo("AL");
    }

    @Test
    void rejeitaAtualizarEnderecoDeOutroMorador() {
        when(moradorRepository.findById(1L)).thenReturn(Optional.of(morador(1L)));
        when(enderecoRepository.findByIdAndMoradorId(20L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(1L, 20L, request("200", false, "SE")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(enderecoRepository, never()).save(any());
    }

    private EnderecoRequest request(String numero, boolean semNumero, String uf) {
        return new EnderecoRequest("Rua das Flores", numero, semNumero, "Centro", "Cidade Teste", uf, "49000000", null);
    }

    private MoradorModel morador(Long id) {
        MoradorModel morador = new MoradorModel();
        morador.setNome("Morador Teste");
        morador.setEmail("maria@exemplo.com");
        ReflectionTestUtils.setField(morador, "id", id);
        return morador;
    }

    private EnderecoModel endereco(Long id, MoradorModel dono) {
        EnderecoModel endereco = new EnderecoModel();
        endereco.setMorador(dono);
        endereco.setLogradouro("Rua das Flores");
        endereco.setNumero("120");
        endereco.setBairro("Centro");
        endereco.setMunicipio("Cidade Teste");
        endereco.setUf("SE");
        endereco.setCep("49000000");
        return comId(endereco, id);
    }

    private EnderecoModel comId(EnderecoModel endereco, Long id) {
        ReflectionTestUtils.setField(endereco, "id", id);
        return endereco;
    }
}
