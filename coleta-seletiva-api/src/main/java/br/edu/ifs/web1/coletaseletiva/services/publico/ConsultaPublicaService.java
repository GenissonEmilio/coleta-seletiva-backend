package br.edu.ifs.web1.coletaseletiva.services.publico;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifs.web1.coletaseletiva.dto.common.PaginaResponse;
import br.edu.ifs.web1.coletaseletiva.dto.common.PaginacaoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.material.MaterialResponse;
import br.edu.ifs.web1.coletaseletiva.dto.pontomaterial.PontoMaterialResponse;
import br.edu.ifs.web1.coletaseletiva.dto.publico.PontoPublicoDetalheResponse;
import br.edu.ifs.web1.coletaseletiva.dto.publico.PontoPublicoResponse;
import br.edu.ifs.web1.coletaseletiva.exception.RecursoNaoEncontradoException;
import br.edu.ifs.web1.coletaseletiva.model.MaterialModel;
import br.edu.ifs.web1.coletaseletiva.model.PontoMaterialModel;
import br.edu.ifs.web1.coletaseletiva.model.PontoModel;
import br.edu.ifs.web1.coletaseletiva.repository.PontoMaterialRepository;
import br.edu.ifs.web1.coletaseletiva.repository.PontoRepository;
import br.edu.ifs.web1.coletaseletiva.services.material.MaterialService;
import br.edu.ifs.web1.coletaseletiva.services.paginacao.PaginacaoService;

/** Consulta pública (RF06): só pontos e materiais ativos, sem dados pessoais. */
@Service
public class ConsultaPublicaService {

    private final PontoRepository pontoRepository;
    private final PontoMaterialRepository pontoMaterialRepository;
    private final MaterialService materialService;
    private final PaginacaoService paginacaoService;

    public ConsultaPublicaService(PontoRepository pontoRepository, PontoMaterialRepository pontoMaterialRepository,
            MaterialService materialService, PaginacaoService paginacaoService) {
        this.pontoRepository = pontoRepository;
        this.pontoMaterialRepository = pontoMaterialRepository;
        this.materialService = materialService;
        this.paginacaoService = paginacaoService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PontoPublicoResponse> buscarPontos(String nome, String municipio, String bairro,
            Long materialId, PaginacaoRequest entrada) {
        Pageable pagina = paginacaoService.criarPageable(entrada, Sort.by(Sort.Order.asc("nome").ignoreCase()));
        return PaginaResponse.de(pontoRepository.buscarPublico(normalizar(nome), normalizar(municipio),
                normalizar(bairro), materialId, pagina), this::converterPonto);
    }

    @Transactional(readOnly = true)
    public PontoPublicoDetalheResponse detalharPonto(Long id) {
        PontoModel ponto = pontoRepository.findById(id).filter(PontoModel::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ponto", id));
        List<PontoMaterialResponse> materiais = pontoMaterialRepository
                .findByPontoIdAndMaterialAtivoTrue(id, Sort.by(Sort.Order.asc("material.nome").ignoreCase()))
                .stream().map(this::converterMaterial).toList();
        return new PontoPublicoDetalheResponse(converterPonto(ponto), materiais);
    }

    /** Reaproveita o catálogo da API-05, filtrando só os materiais ativos. */
    @Transactional(readOnly = true)
    public PaginaResponse<MaterialResponse> listarMateriais(PaginacaoRequest entrada) {
        return materialService.listar(true, entrada);
    }

    /** Filtro ausente vira "" (a consulta ignora); o resto é comparado sem diferenciar maiúsculas. */
    private static String normalizar(String filtro) {
        return filtro == null ? "" : filtro.trim().toLowerCase(Locale.ROOT);
    }

    private PontoPublicoResponse converterPonto(PontoModel ponto) {
        return new PontoPublicoResponse(ponto.getId(), ponto.getNome(), ponto.getLogradouro(), ponto.getNumero(),
                ponto.isSemNumero(), ponto.getBairro(), ponto.getMunicipio(), ponto.getUf(), ponto.getCep(),
                ponto.getComplemento(), ponto.getContato(), ponto.getHorarios(), ponto.isRecolhimentoDomiciliar());
    }

    private PontoMaterialResponse converterMaterial(PontoMaterialModel vinculo) {
        MaterialModel material = vinculo.getMaterial();
        return new PontoMaterialResponse(vinculo.getId().getPontoId(), material.getId(), material.getNome(),
                material.getUnidade(), material.getOrientacaoPreparo(), vinculo.getObservacoes(),
                semZerosFinais(vinculo.getLimitePorSolicitacao()));
    }

    /** O banco devolve numeric(12,3) como 20.000; a resposta mostra 20, igual à consulta administrativa. */
    private static BigDecimal semZerosFinais(BigDecimal valor) {
        return valor == null ? null : new BigDecimal(valor.stripTrailingZeros().toPlainString());
    }
}
