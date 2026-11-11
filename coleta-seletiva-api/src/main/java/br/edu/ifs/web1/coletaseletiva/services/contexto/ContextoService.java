package br.edu.ifs.web1.coletaseletiva.services.contexto;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifs.web1.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.web1.coletaseletiva.model.MoradorModel;
import br.edu.ifs.web1.coletaseletiva.model.OperadorModel;
import br.edu.ifs.web1.coletaseletiva.model.PontoModel;
import br.edu.ifs.web1.coletaseletiva.repository.MoradorRepository;
import br.edu.ifs.web1.coletaseletiva.repository.OperadorRepository;

/**
 * Contexto de demonstração (seção 3 do PDF): o cliente escolhe um ator fictício pelos cabeçalhos
 * X-Ator-Tipo e X-Ator-Id, e o backend confere no banco quem ele é e a quais pontos está vinculado.
 *
 * ATENÇÃO: isto é simulação, não autenticação. Qualquer cliente pode enviar qualquer id.
 * Não usar em produção; se houver login, trocar por identidade autenticada (Spring Security).
 */
@Service
public class ContextoService {

    public static final String CABECALHO_TIPO = "X-Ator-Tipo";
    public static final String CABECALHO_ID = "X-Ator-Id";

    private final MoradorRepository moradorRepository;
    private final OperadorRepository operadorRepository;

    public ContextoService(MoradorRepository moradorRepository, OperadorRepository operadorRepository) {
        this.moradorRepository = moradorRepository;
        this.operadorRepository = operadorRepository;
    }

    /** Ausente, tipo inválido, id inexistente ou ator inativo: 403, sem dizer qual foi o motivo. */
    @Transactional(readOnly = true)
    public ContextoAtor resolver(String tipo, Long id) {
        return switch (converterTipo(tipo)) {
            case ADMINISTRADOR -> new ContextoAtor(TipoAtor.ADMINISTRADOR, null, "Administrador", Set.of());
            case MORADOR -> moradorRepository.findById(exigirId(id))
                    .filter(MoradorModel::isAtivo)
                    .map(morador -> new ContextoAtor(TipoAtor.MORADOR, morador.getId(), morador.getNome(), Set.of()))
                    .orElseThrow(ContextoService::atorDesconhecido);
            case OPERADOR -> operadorRepository.findById(exigirId(id))
                    .filter(OperadorModel::isAtivo)
                    .map(ContextoService::contextoDoOperador)
                    .orElseThrow(ContextoService::atorDesconhecido);
        };
    }

    public void exigirAdministrador(ContextoAtor ator) {
        if (!ator.is(TipoAtor.ADMINISTRADOR)) {
            throw new AcessoNegadoException("Operação permitida apenas ao administrador.");
        }
    }

    /** Criar e corrigir solicitação é ação do morador (seção 3 do PDF). */
    public void exigirMorador(ContextoAtor ator) {
        if (!ator.is(TipoAtor.MORADOR)) {
            throw new AcessoNegadoException("Operação permitida apenas ao morador.");
        }
    }

    /** O administrador vê qualquer operador; o operador só vê a si mesmo. */
    public void exigirAdministradorOuProprioOperador(ContextoAtor ator, Long operadorId) {
        boolean proprio = ator.is(TipoAtor.OPERADOR) && ator.id().equals(operadorId);
        if (!ator.is(TipoAtor.ADMINISTRADOR) && !proprio) {
            throw new AcessoNegadoException("Operador só pode consultar os próprios dados.");
        }
    }

    /** Para as ações do operador (API-12/API-13): só no ponto ao qual ele está vinculado (RF07). */
    public void exigirOperadorDoPonto(ContextoAtor ator, Long pontoId) {
        if (!ator.is(TipoAtor.OPERADOR) || !ator.pontoIds().contains(pontoId)) {
            throw new AcessoNegadoException("Operador não está vinculado a este ponto.");
        }
    }

    /** Para as consultas do morador (API-10/API-11): só os próprios registros. */
    public void exigirMoradorDono(ContextoAtor ator, Long moradorId) {
        if (!ator.is(TipoAtor.MORADOR) || !ator.id().equals(moradorId)) {
            throw new AcessoNegadoException("Morador só pode acessar os próprios registros.");
        }
    }

    private static TipoAtor converterTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            throw atorDesconhecido();
        }
        try {
            return TipoAtor.valueOf(tipo.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw atorDesconhecido();
        }
    }

    private static Long exigirId(Long id) {
        if (id == null) {
            throw atorDesconhecido();
        }
        return id;
    }

    private static ContextoAtor contextoDoOperador(OperadorModel operador) {
        Set<Long> pontoIds = operador.getPontos().stream().map(PontoModel::getId).collect(Collectors.toSet());
        return new ContextoAtor(TipoAtor.OPERADOR, operador.getId(), operador.getNome(), pontoIds);
    }

    private static AcessoNegadoException atorDesconhecido() {
        return new AcessoNegadoException("Selecione um ator de demonstração válido (" + CABECALHO_TIPO + " e "
                + CABECALHO_ID + ").");
    }
}
