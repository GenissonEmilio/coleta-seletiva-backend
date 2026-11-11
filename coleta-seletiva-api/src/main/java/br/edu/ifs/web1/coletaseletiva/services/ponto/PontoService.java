package br.edu.ifs.web1.coletaseletiva.services.ponto;

import java.util.Locale;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifs.web1.coletaseletiva.dto.common.PaginaResponse;
import br.edu.ifs.web1.coletaseletiva.dto.common.PaginacaoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.ponto.PontoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.ponto.PontoResponse;
import br.edu.ifs.web1.coletaseletiva.dto.ponto.PontoSituacaoRequest;
import br.edu.ifs.web1.coletaseletiva.exception.CodigoErro;
import br.edu.ifs.web1.coletaseletiva.exception.EntradaInvalidaException;
import br.edu.ifs.web1.coletaseletiva.exception.RecursoNaoEncontradoException;
import br.edu.ifs.web1.coletaseletiva.exception.RegraNegocioException;
import br.edu.ifs.web1.coletaseletiva.model.PontoModel;
import br.edu.ifs.web1.coletaseletiva.repository.PontoRepository;
import br.edu.ifs.web1.coletaseletiva.services.paginacao.PaginacaoService;

@Service
public class PontoService {

    private final PontoRepository pontoRepository;
    private final PaginacaoService paginacaoService;

    public PontoService(PontoRepository pontoRepository, PaginacaoService paginacaoService) {
        this.pontoRepository = pontoRepository;
        this.paginacaoService = paginacaoService;
    }

    @Transactional
    public PontoResponse cadastrar(PontoRequest request) {
        validarNumero(request);
        PontoModel ponto = new PontoModel();
        preencher(ponto, request);
        return converter(pontoRepository.save(ponto));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PontoResponse> listar(Boolean ativo, PaginacaoRequest entrada) {
        Pageable pagina = paginacaoService.criarPageable(entrada,
                Sort.by(Sort.Order.asc("nome").ignoreCase()));
        return PaginaResponse.de(pontoRepository.listar(ativo, pagina), this::converter);
    }

    @Transactional(readOnly = true)
    public PontoResponse buscar(Long id) {
        return converter(buscarModel(id));
    }

    /** A API-10 deve chamar este método dentro da transação que cria/corrige a solicitação. */
    @Transactional(readOnly = true)
    public PontoModel buscarDisponivelParaSolicitacao(Long id) {
        PontoModel ponto = buscarModel(id);
        if (!ponto.isAtivo() || !ponto.isRecolhimentoDomiciliar()) {
            throw new RegraNegocioException(CodigoErro.REGRA_NEGOCIO,
                    "Ponto inativo ou sem recolhimento domiciliar não recebe nova solicitação.");
        }
        return ponto;
    }

    @Transactional
    public PontoResponse atualizar(Long id, PontoRequest request) {
        validarNumero(request);
        PontoModel ponto = buscarModel(id);
        preencher(ponto, request);
        return converter(pontoRepository.save(ponto));
    }

    @Transactional
    public PontoResponse alterarSituacao(Long id, PontoSituacaoRequest request) {
        PontoModel ponto = buscarModel(id);
        ponto.setAtivo(request.ativo());
        return converter(pontoRepository.save(ponto));
    }

    private PontoModel buscarModel(Long id) {
        return pontoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ponto", id));
    }

    private void validarNumero(PontoRequest request) {
        boolean numeroPreenchido = request.numero() != null && !request.numero().isBlank();
        if (request.semNumero() && numeroPreenchido) {
            throw new EntradaInvalidaException("numero", "deve ficar vazio quando semNumero é verdadeiro");
        }
        if (!request.semNumero() && !numeroPreenchido) {
            throw new EntradaInvalidaException("numero", "é obrigatório quando semNumero é falso");
        }
    }

    private void preencher(PontoModel ponto, PontoRequest request) {
        ponto.setNome(request.nome());
        ponto.setLogradouro(request.logradouro());
        ponto.setSemNumero(request.semNumero());
        ponto.setNumero(request.semNumero() ? null : request.numero().trim());
        ponto.setBairro(request.bairro());
        ponto.setMunicipio(request.municipio());
        ponto.setUf(request.uf().toUpperCase(Locale.ROOT));
        ponto.setCep(request.cep());
        ponto.setComplemento(request.complemento());
        ponto.setContato(request.contato());
        ponto.setHorarios(request.horarios());
        ponto.setRecolhimentoDomiciliar(request.recolhimentoDomiciliar());
    }

    private PontoResponse converter(PontoModel ponto) {
        return new PontoResponse(ponto.getId(), ponto.getNome(), ponto.getLogradouro(), ponto.getNumero(),
                ponto.isSemNumero(), ponto.getBairro(), ponto.getMunicipio(), ponto.getUf(), ponto.getCep(),
                ponto.getComplemento(), ponto.getContato(), ponto.getHorarios(), ponto.isRecolhimentoDomiciliar(),
                ponto.isAtivo());
    }
}
