package br.edu.ifs.web1.coletaseletiva.services.material;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifs.web1.coletaseletiva.dto.common.PaginaResponse;
import br.edu.ifs.web1.coletaseletiva.dto.common.PaginacaoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.material.MaterialRequest;
import br.edu.ifs.web1.coletaseletiva.dto.material.MaterialResponse;
import br.edu.ifs.web1.coletaseletiva.dto.material.MaterialSituacaoRequest;
import br.edu.ifs.web1.coletaseletiva.exception.CodigoErro;
import br.edu.ifs.web1.coletaseletiva.exception.RecursoNaoEncontradoException;
import br.edu.ifs.web1.coletaseletiva.exception.RegraNegocioException;
import br.edu.ifs.web1.coletaseletiva.model.MaterialModel;
import br.edu.ifs.web1.coletaseletiva.repository.MaterialRepository;
import br.edu.ifs.web1.coletaseletiva.services.paginacao.PaginacaoService;

@Service
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final PaginacaoService paginacaoService;

    public MaterialService(MaterialRepository materialRepository, PaginacaoService paginacaoService) {
        this.materialRepository = materialRepository;
        this.paginacaoService = paginacaoService;
    }

    @Transactional
    public MaterialResponse cadastrar(MaterialRequest request) {
        validarNome(request.nome(), null);
        MaterialModel material = new MaterialModel();
        preencher(material, request);
        return converter(salvar(material));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<MaterialResponse> listar(Boolean ativo, PaginacaoRequest entrada) {
        Pageable pagina = paginacaoService.criarPageable(entrada,
                Sort.by(Sort.Order.asc("nome").ignoreCase()));
        return PaginaResponse.de(materialRepository.listar(ativo, pagina), this::converter);
    }

    @Transactional(readOnly = true)
    public MaterialResponse buscar(Long id) {
        return converter(buscarModel(id));
    }

    @Transactional
    public MaterialResponse atualizar(Long id, MaterialRequest request) {
        MaterialModel material = buscarModel(id);
        validarNome(request.nome(), id);
        preencher(material, request);
        return converter(salvar(material));
    }

    @Transactional
    public MaterialResponse alterarSituacao(Long id, MaterialSituacaoRequest request) {
        MaterialModel material = buscarModel(id);
        material.setAtivo(request.ativo());
        return converter(salvar(material));
    }

    /** A API-10 deve chamar este método dentro da transação que cria/corrige a solicitação. */
    @Transactional(readOnly = true)
    public MaterialModel buscarAtivoParaSolicitacao(Long id) {
        MaterialModel material = buscarModel(id);
        if (!material.isAtivo()) {
            throw new RegraNegocioException(CodigoErro.MATERIAL_INCOMPATIVEL,
                    "Material inativo não pode ser usado em uma nova solicitação.");
        }
        return material;
    }

    private MaterialModel buscarModel(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material", id));
    }

    private void validarNome(String nome, Long idIgnorado) {
        if (materialRepository.existeNome(nome, idIgnorado)) {
            throw nomeDuplicado();
        }
    }

    private MaterialModel salvar(MaterialModel material) {
        try {
            return materialRepository.saveAndFlush(material);
        } catch (DataIntegrityViolationException ex) {
            for (Throwable causa = ex; causa != null; causa = causa.getCause()) {
                if (causa instanceof ConstraintViolationException violacao
                        && ("material_nome_key".equals(violacao.getConstraintName())
                        || "idx_material_nome_normalizado".equals(violacao.getConstraintName()))) {
                    throw nomeDuplicado();
                }
            }
            throw ex;
        }
    }

    private void preencher(MaterialModel material, MaterialRequest request) {
        material.setNome(request.nome());
        material.setDescricao(request.descricao());
        material.setUnidade(request.unidade());
        material.setOrientacaoPreparo(request.orientacaoPreparo());
    }

    private MaterialResponse converter(MaterialModel material) {
        return new MaterialResponse(material.getId(), material.getNome(), material.getDescricao(),
                material.getUnidade(), material.getOrientacaoPreparo(), material.isAtivo());
    }

    private RegraNegocioException nomeDuplicado() {
        return new RegraNegocioException(CodigoErro.DUPLICIDADE, "Já existe um material com este nome.");
    }
}
