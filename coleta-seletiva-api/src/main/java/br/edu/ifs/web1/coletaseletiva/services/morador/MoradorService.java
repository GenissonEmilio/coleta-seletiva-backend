package br.edu.ifs.web1.coletaseletiva.services.morador;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifs.web1.coletaseletiva.dto.morador.MoradorRequest;
import br.edu.ifs.web1.coletaseletiva.dto.morador.MoradorResponse;
import br.edu.ifs.web1.coletaseletiva.dto.morador.MoradorSituacaoRequest;
import br.edu.ifs.web1.coletaseletiva.exception.CodigoErro;
import br.edu.ifs.web1.coletaseletiva.exception.RecursoNaoEncontradoException;
import br.edu.ifs.web1.coletaseletiva.exception.RegraNegocioException;
import br.edu.ifs.web1.coletaseletiva.model.MoradorModel;
import br.edu.ifs.web1.coletaseletiva.repository.MoradorRepository;

@Service
public class MoradorService {

    private final MoradorRepository moradorRepository;

    public MoradorService(MoradorRepository moradorRepository) {
        this.moradorRepository = moradorRepository;
    }

    @Transactional
    public MoradorResponse cadastrar(MoradorRequest request) {
        if (moradorRepository.existsByEmail(request.email())) {
            throw emailDuplicado();
        }
        MoradorModel morador = new MoradorModel();
        preencher(morador, request);
        return converter(salvar(morador));
    }

    @Transactional(readOnly = true)
    public MoradorResponse buscar(Long id) {
        return converter(buscarModel(id));
    }

    @Transactional
    public MoradorResponse atualizar(Long id, MoradorRequest request) {
        MoradorModel morador = buscarModel(id);
        boolean emailDeOutroMorador = moradorRepository.findByEmail(request.email())
                .filter(existente -> !existente.getId().equals(id))
                .isPresent();
        if (emailDeOutroMorador) {
            throw emailDuplicado();
        }
        preencher(morador, request);
        return converter(salvar(morador));
    }

    @Transactional
    public MoradorResponse alterarSituacao(Long id, MoradorSituacaoRequest request) {
        MoradorModel morador = buscarModel(id);
        morador.setAtivo(request.ativo());
        return converter(salvar(morador));
    }

    private MoradorModel buscarModel(Long id) {
        return moradorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Morador", id));
    }

    private MoradorModel salvar(MoradorModel morador) {
        try {
            return moradorRepository.saveAndFlush(morador);
        } catch (DataIntegrityViolationException e) {
            throw emailDuplicado();
        }
    }

    private void preencher(MoradorModel morador, MoradorRequest request) {
        morador.setNome(request.nome());
        morador.setEmail(request.email());
        morador.setTelefone(request.telefone());
    }

    private MoradorResponse converter(MoradorModel morador) {
        return new MoradorResponse(morador.getId(), morador.getNome(), morador.getEmail(),
                morador.getTelefone(), morador.isAtivo());
    }

    private RegraNegocioException emailDuplicado() {
        return new RegraNegocioException(CodigoErro.DUPLICIDADE, "Já existe um morador com este e-mail.");
    }
}
