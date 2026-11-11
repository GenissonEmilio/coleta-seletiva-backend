package br.edu.ifs.web1.coletaseletiva.services.operador;

import java.util.Comparator;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifs.web1.coletaseletiva.dto.common.PaginaResponse;
import br.edu.ifs.web1.coletaseletiva.dto.common.PaginacaoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.operador.OperadorRequest;
import br.edu.ifs.web1.coletaseletiva.dto.operador.OperadorResponse;
import br.edu.ifs.web1.coletaseletiva.exception.CodigoErro;
import br.edu.ifs.web1.coletaseletiva.exception.RecursoNaoEncontradoException;
import br.edu.ifs.web1.coletaseletiva.exception.RegraNegocioException;
import br.edu.ifs.web1.coletaseletiva.model.OperadorModel;
import br.edu.ifs.web1.coletaseletiva.model.PontoModel;
import br.edu.ifs.web1.coletaseletiva.repository.OperadorRepository;
import br.edu.ifs.web1.coletaseletiva.repository.PontoRepository;
import br.edu.ifs.web1.coletaseletiva.services.paginacao.PaginacaoService;

@Service
public class OperadorService {

    private final OperadorRepository operadorRepository;
    private final PontoRepository pontoRepository;
    private final PaginacaoService paginacaoService;

    public OperadorService(OperadorRepository operadorRepository, PontoRepository pontoRepository,
            PaginacaoService paginacaoService) {
        this.operadorRepository = operadorRepository;
        this.pontoRepository = pontoRepository;
        this.paginacaoService = paginacaoService;
    }

    /** Resposta do PUT do vínculo: o operador atualizado e se o vínculo foi criado agora (201) ou já existia (200). */
    public record VinculoGravado(OperadorResponse operador, boolean criado) {
    }

    @Transactional
    public OperadorResponse cadastrar(OperadorRequest request) {
        if (operadorRepository.existsByEmail(request.email())) {
            throw emailDuplicado();
        }
        OperadorModel operador = new OperadorModel();
        preencher(operador, request);
        return converter(salvar(operador));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<OperadorResponse> listar(PaginacaoRequest entrada) {
        Pageable pagina = paginacaoService.criarPageable(entrada, Sort.by(Sort.Order.asc("nome").ignoreCase()));
        return PaginaResponse.de(operadorRepository.findAll(pagina), this::converter);
    }

    @Transactional(readOnly = true)
    public OperadorResponse buscar(Long id) {
        return converter(buscarModel(id));
    }

    @Transactional
    public OperadorResponse atualizar(Long id, OperadorRequest request) {
        OperadorModel operador = buscarModel(id);
        boolean emailDeOutroOperador = operadorRepository.findByEmail(request.email())
                .filter(existente -> !existente.getId().equals(id))
                .isPresent();
        if (emailDeOutroOperador) {
            throw emailDuplicado();
        }
        preencher(operador, request);
        return converter(salvar(operador));
    }

    @Transactional
    public VinculoGravado vincular(Long operadorId, Long pontoId) {
        OperadorModel operador = buscarModel(operadorId);
        PontoModel ponto = pontoRepository.findById(pontoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ponto", pontoId));
        boolean criado = operador.getPontos().add(ponto);
        return new VinculoGravado(converter(operadorRepository.saveAndFlush(operador)), criado);
    }

    @Transactional
    public void desvincular(Long operadorId, Long pontoId) {
        OperadorModel operador = buscarModel(operadorId);
        boolean removido = operador.getPontos().removeIf(ponto -> ponto.getId().equals(pontoId));
        if (!removido) {
            throw new RecursoNaoEncontradoException("Vínculo com o ponto", pontoId);
        }
        operadorRepository.save(operador);
    }

    private OperadorModel buscarModel(Long id) {
        return operadorRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Operador", id));
    }

    private OperadorModel salvar(OperadorModel operador) {
        try {
            return operadorRepository.saveAndFlush(operador);
        } catch (DataIntegrityViolationException e) {
            throw emailDuplicado();
        }
    }

    private void preencher(OperadorModel operador, OperadorRequest request) {
        operador.setNome(request.nome());
        operador.setEmail(request.email());
    }

    private OperadorResponse converter(OperadorModel operador) {
        List<OperadorResponse.PontoVinculado> pontos = operador.getPontos().stream()
                .sorted(Comparator.comparing(PontoModel::getId))
                .map(ponto -> new OperadorResponse.PontoVinculado(ponto.getId(), ponto.getNome()))
                .toList();
        return new OperadorResponse(operador.getId(), operador.getNome(), operador.getEmail(), operador.isAtivo(),
                pontos);
    }

    private RegraNegocioException emailDuplicado() {
        return new RegraNegocioException(CodigoErro.DUPLICIDADE, "Já existe um operador com este e-mail.");
    }
}
