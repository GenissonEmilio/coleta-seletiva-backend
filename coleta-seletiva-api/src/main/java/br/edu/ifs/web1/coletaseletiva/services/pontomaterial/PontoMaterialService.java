package br.edu.ifs.web1.coletaseletiva.services.pontomaterial;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifs.web1.coletaseletiva.dto.common.PaginaResponse;
import br.edu.ifs.web1.coletaseletiva.dto.common.PaginacaoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.pontomaterial.PontoMaterialRequest;
import br.edu.ifs.web1.coletaseletiva.dto.pontomaterial.PontoMaterialResponse;
import br.edu.ifs.web1.coletaseletiva.exception.CodigoErro;
import br.edu.ifs.web1.coletaseletiva.exception.RecursoNaoEncontradoException;
import br.edu.ifs.web1.coletaseletiva.exception.RegraNegocioException;
import br.edu.ifs.web1.coletaseletiva.model.MaterialModel;
import br.edu.ifs.web1.coletaseletiva.model.PontoMaterialId;
import br.edu.ifs.web1.coletaseletiva.model.PontoMaterialModel;
import br.edu.ifs.web1.coletaseletiva.model.PontoModel;
import br.edu.ifs.web1.coletaseletiva.repository.MaterialRepository;
import br.edu.ifs.web1.coletaseletiva.repository.PontoMaterialRepository;
import br.edu.ifs.web1.coletaseletiva.repository.PontoRepository;
import br.edu.ifs.web1.coletaseletiva.services.material.QuantidadeMaterialService;
import br.edu.ifs.web1.coletaseletiva.services.paginacao.PaginacaoService;
import br.edu.ifs.web1.coletaseletiva.services.ponto.PontoService;

@Service
public class PontoMaterialService {

    private final PontoMaterialRepository pontoMaterialRepository;
    private final PontoRepository pontoRepository;
    private final PontoService pontoService;
    private final MaterialRepository materialRepository;
    private final QuantidadeMaterialService quantidadeMaterialService;
    private final PaginacaoService paginacaoService;

    public PontoMaterialService(PontoMaterialRepository pontoMaterialRepository, PontoRepository pontoRepository,
            PontoService pontoService, MaterialRepository materialRepository,
            QuantidadeMaterialService quantidadeMaterialService, PaginacaoService paginacaoService) {
        this.pontoMaterialRepository = pontoMaterialRepository;
        this.pontoRepository = pontoRepository;
        this.pontoService = pontoService;
        this.materialRepository = materialRepository;
        this.quantidadeMaterialService = quantidadeMaterialService;
        this.paginacaoService = paginacaoService;
    }

    /** Resposta do PUT: o vínculo gravado e se ele foi criado agora (201) ou só atualizado (200). */
    public record VinculoGravado(PontoMaterialResponse vinculo, boolean criado) {
    }

    @Transactional
    public VinculoGravado vincular(Long pontoId, Long materialId, PontoMaterialRequest request) {
        PontoModel ponto = pontoService.travarParaAlteracao(pontoId);
        MaterialModel material = buscarMaterial(materialId);
        validarLimite(request, material);
        Optional<PontoMaterialModel> atual =
                pontoMaterialRepository.findById(new PontoMaterialId(pontoId, materialId));
        boolean criado = atual.isEmpty();
        if (criado && !material.isAtivo()) {
            throw new RegraNegocioException(CodigoErro.MATERIAL_INCOMPATIVEL,
                    "Material inativo não pode ser vinculado a um ponto.");
        }
        PontoMaterialModel vinculo = atual.orElseGet(() -> new PontoMaterialModel(ponto, material));
        vinculo.setObservacoes(request.observacoes());
        vinculo.setLimitePorSolicitacao(request.limitePorSolicitacao());
        return new VinculoGravado(converter(pontoMaterialRepository.saveAndFlush(vinculo)), criado);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PontoMaterialResponse> listar(Long pontoId, PaginacaoRequest entrada) {
        buscarPonto(pontoId);
        Pageable pagina = paginacaoService.criarPageable(entrada,
                Sort.by(Sort.Order.asc("material.nome").ignoreCase()));
        return PaginaResponse.de(pontoMaterialRepository.findByPontoId(pontoId, pagina), this::converter);
    }

    @Transactional
    public void remover(Long pontoId, Long materialId) {
        pontoService.travarParaAlteracao(pontoId);
        PontoMaterialModel vinculo = pontoMaterialRepository.findById(new PontoMaterialId(pontoId, materialId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material aceito", materialId));
        pontoService.exigirSemSolicitacoesAbertas(pontoId, materialId);
        pontoMaterialRepository.delete(vinculo);
    }

    private void validarLimite(PontoMaterialRequest request, MaterialModel material) {
        if (request.limitePorSolicitacao() != null) {
            quantidadeMaterialService.validarPositiva(request.limitePorSolicitacao(), material.getUnidade(),
                    "limitePorSolicitacao");
        }
    }

    private PontoModel buscarPonto(Long id) {
        return pontoRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Ponto", id));
    }

    private MaterialModel buscarMaterial(Long id) {
        return materialRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Material", id));
    }

    private PontoMaterialResponse converter(PontoMaterialModel vinculo) {
        MaterialModel material = vinculo.getMaterial();
        return new PontoMaterialResponse(vinculo.getId().getPontoId(), vinculo.getId().getMaterialId(),
                material.getNome(), material.getUnidade(), material.getOrientacaoPreparo(), vinculo.getObservacoes(), semZerosFinais(vinculo.getLimitePorSolicitacao()));
    }

    /** O banco devolve numeric(12,3) como 20.000; a resposta mostra 20, igual ao que o cliente enviou. */
    private static BigDecimal semZerosFinais(BigDecimal valor) {
        return valor == null ? null : new BigDecimal(valor.stripTrailingZeros().toPlainString());
    }
}
