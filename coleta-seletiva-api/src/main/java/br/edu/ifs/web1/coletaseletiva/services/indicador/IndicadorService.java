package br.edu.ifs.web1.coletaseletiva.services.indicador;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifs.web1.coletaseletiva.dto.common.PaginaResponse;
import br.edu.ifs.web1.coletaseletiva.dto.common.PaginacaoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.indicador.RecolhimentoPorMaterialResponse;
import br.edu.ifs.web1.coletaseletiva.dto.indicador.SolicitacoesPorSituacaoResponse;
import br.edu.ifs.web1.coletaseletiva.exception.EntradaInvalidaException;
import br.edu.ifs.web1.coletaseletiva.model.SituacaoSolicitacao;
import br.edu.ifs.web1.coletaseletiva.repository.SolicitacaoRepository;
import br.edu.ifs.web1.coletaseletiva.services.paginacao.PaginacaoService;

@Service
@Transactional(readOnly = true)
public class IndicadorService {

    private final SolicitacaoRepository solicitacaoRepository;
    private final PaginacaoService paginacaoService;

    public IndicadorService(SolicitacaoRepository solicitacaoRepository, PaginacaoService paginacaoService) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.paginacaoService = paginacaoService;
    }

    public List<SolicitacoesPorSituacaoResponse> contarSolicitacoes(OffsetDateTime inicio, OffsetDateTime fim) {
        validarPeriodo(inicio, fim);
        Map<SituacaoSolicitacao, Long> totais = new EnumMap<>(SituacaoSolicitacao.class);
        solicitacaoRepository.contarPorSituacao(inicio, fim)
                .forEach(linha -> totais.put(linha.situacao(), linha.total()));
        return Arrays.stream(SituacaoSolicitacao.values())
                .map(situacao -> new SolicitacoesPorSituacaoResponse(situacao, totais.getOrDefault(situacao, 0L)))
                .toList();
    }

    public PaginaResponse<RecolhimentoPorMaterialResponse> somarRecolhimentos(
            OffsetDateTime inicio, OffsetDateTime fim, PaginacaoRequest entrada) {
        validarPeriodo(inicio, fim);
        return PaginaResponse.de(solicitacaoRepository.somarRecolhimentos(
                inicio, fim, paginacaoService.criarPageable(entrada)));
    }

    private void validarPeriodo(OffsetDateTime inicio, OffsetDateTime fim) {
        if (inicio != null && fim != null && !inicio.isBefore(fim)) {
            throw new EntradaInvalidaException("fim", "deve ser posterior ao início");
        }
    }
}
