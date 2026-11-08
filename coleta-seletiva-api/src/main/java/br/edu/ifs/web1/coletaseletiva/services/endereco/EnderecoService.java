package br.edu.ifs.web1.coletaseletiva.services.endereco;

import java.util.Locale;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;
    private final MoradorRepository moradorRepository;
    private final PaginacaoService paginacaoService;

    public EnderecoService(EnderecoRepository enderecoRepository, MoradorRepository moradorRepository,
            PaginacaoService paginacaoService) {
        this.enderecoRepository = enderecoRepository;
        this.moradorRepository = moradorRepository;
        this.paginacaoService = paginacaoService;
    }

    @Transactional
    public EnderecoResponse cadastrar(Long moradorId, EnderecoRequest request) {
        validarNumero(request);
        EnderecoModel endereco = new EnderecoModel();
        endereco.setMorador(buscarMorador(moradorId));
        preencher(endereco, request);
        return converter(enderecoRepository.save(endereco));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<EnderecoResponse> listar(Long moradorId, PaginacaoRequest paginacao) {
        buscarMorador(moradorId);
        Pageable pagina = paginacaoService.criarPageable(paginacao, Sort.unsorted());
        return PaginaResponse.de(enderecoRepository.findByMoradorId(moradorId, pagina), this::converter);
    }

    @Transactional
    public EnderecoResponse atualizar(Long moradorId, Long enderecoId, EnderecoRequest request) {
        validarNumero(request);
        buscarMorador(moradorId);
        EnderecoModel endereco = enderecoRepository.findByIdAndMoradorId(enderecoId, moradorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Endereço", enderecoId));
        preencher(endereco, request);
        return converter(enderecoRepository.save(endereco));
    }

    private MoradorModel buscarMorador(Long moradorId) {
        return moradorRepository.findById(moradorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Morador", moradorId));
    }

    private void validarNumero(EnderecoRequest request) {
        boolean numeroPreenchido = request.numero() != null && !request.numero().isBlank();
        if (request.semNumero() && numeroPreenchido) {
            throw new EntradaInvalidaException("numero", "deve ficar vazio quando semNumero é verdadeiro");
        }
        if (!request.semNumero() && !numeroPreenchido) {
            throw new EntradaInvalidaException("numero", "é obrigatório quando semNumero é falso");
        }
    }

    private void preencher(EnderecoModel endereco, EnderecoRequest request) {
        endereco.setLogradouro(request.logradouro());
        endereco.setSemNumero(request.semNumero());
        endereco.setNumero(request.semNumero() ? null : request.numero().trim());
        endereco.setBairro(request.bairro());
        endereco.setMunicipio(request.municipio());
        endereco.setUf(request.uf().toUpperCase(Locale.ROOT));
        endereco.setCep(request.cep());
        endereco.setComplemento(request.complemento());
    }

    private EnderecoResponse converter(EnderecoModel endereco) {
        return new EnderecoResponse(endereco.getId(), endereco.getMorador().getId(), endereco.getLogradouro(),
                endereco.getNumero(), endereco.isSemNumero(), endereco.getBairro(), endereco.getMunicipio(),
                endereco.getUf(), endereco.getCep(), endereco.getComplemento(), endereco.isAtivo());
    }
}
