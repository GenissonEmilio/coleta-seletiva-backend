package br.edu.ifs.web1.coletaseletiva.services.solicitacao;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifs.web1.coletaseletiva.dto.solicitacao.ItemSolicitacaoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.solicitacao.SolicitacaoCorrecaoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.solicitacao.SolicitacaoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.solicitacao.SolicitacaoResponse;
import br.edu.ifs.web1.coletaseletiva.exception.CodigoErro;
import br.edu.ifs.web1.coletaseletiva.exception.EntradaInvalidaException;
import br.edu.ifs.web1.coletaseletiva.exception.RecursoNaoEncontradoException;
import br.edu.ifs.web1.coletaseletiva.exception.RegraNegocioException;
import br.edu.ifs.web1.coletaseletiva.model.EnderecoModel;
import br.edu.ifs.web1.coletaseletiva.model.HistoricoSolicitacaoModel;
import br.edu.ifs.web1.coletaseletiva.model.ItemSolicitacaoModel;
import br.edu.ifs.web1.coletaseletiva.model.MaterialModel;
import br.edu.ifs.web1.coletaseletiva.model.PontoMaterialId;
import br.edu.ifs.web1.coletaseletiva.model.PontoMaterialModel;
import br.edu.ifs.web1.coletaseletiva.model.PontoModel;
import br.edu.ifs.web1.coletaseletiva.model.ResponsavelTipo;
import br.edu.ifs.web1.coletaseletiva.model.SituacaoSolicitacao;
import br.edu.ifs.web1.coletaseletiva.model.SolicitacaoModel;
import br.edu.ifs.web1.coletaseletiva.repository.EnderecoRepository;
import br.edu.ifs.web1.coletaseletiva.repository.HistoricoSolicitacaoRepository;
import br.edu.ifs.web1.coletaseletiva.repository.MoradorRepository;
import br.edu.ifs.web1.coletaseletiva.repository.PontoMaterialRepository;
import br.edu.ifs.web1.coletaseletiva.repository.PontoRepository;
import br.edu.ifs.web1.coletaseletiva.repository.SolicitacaoRepository;
import br.edu.ifs.web1.coletaseletiva.services.contexto.ContextoAtor;
import br.edu.ifs.web1.coletaseletiva.services.material.MaterialService;
import br.edu.ifs.web1.coletaseletiva.services.material.QuantidadeMaterialService;
import br.edu.ifs.web1.coletaseletiva.services.ponto.PontoService;

/**
 * Criação e correção de solicitação (RF08). Cada método público é uma única transação (RN06):
 * solicitação, itens e histórico são gravados juntos ou nada é gravado.
 */
@Service
public class SolicitacaoService {

    private static final String LETRAS_PROTOCOLO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int TAMANHO_CODIGO = 6;
    private static final DateTimeFormatter DATA_PROTOCOLO = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SolicitacaoRepository solicitacaoRepository;
    private final HistoricoSolicitacaoRepository historicoRepository;
    private final MoradorRepository moradorRepository;
    private final EnderecoRepository enderecoRepository;
    private final PontoRepository pontoRepository;
    private final PontoMaterialRepository pontoMaterialRepository;
    private final PontoService pontoService;
    private final MaterialService materialService;
    private final QuantidadeMaterialService quantidadeMaterialService;
    private final EntityManager entityManager;
    private final SecureRandom sorteio = new SecureRandom();

    public SolicitacaoService(SolicitacaoRepository solicitacaoRepository,
            HistoricoSolicitacaoRepository historicoRepository, MoradorRepository moradorRepository,
            EnderecoRepository enderecoRepository, PontoRepository pontoRepository,
            PontoMaterialRepository pontoMaterialRepository, PontoService pontoService,
            MaterialService materialService, QuantidadeMaterialService quantidadeMaterialService,
            EntityManager entityManager) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.historicoRepository = historicoRepository;
        this.moradorRepository = moradorRepository;
        this.enderecoRepository = enderecoRepository;
        this.pontoRepository = pontoRepository;
        this.pontoMaterialRepository = pontoMaterialRepository;
        this.pontoService = pontoService;
        this.materialService = materialService;
        this.quantidadeMaterialService = quantidadeMaterialService;
        this.entityManager = entityManager;
    }

    /** Item já conferido: material ativo, aceito pelo ponto, quantidade válida e dentro do limite. */
    private record ItemValidado(MaterialModel material, BigDecimal quantidade) {
    }

    @Transactional
    public SolicitacaoResponse criar(ContextoAtor morador, SolicitacaoRequest request) {
        EnderecoModel endereco = buscarEnderecoDoMorador(request.enderecoId(), morador.id());
        PontoModel ponto = travarPontoDisponivel(request.pontoId());
        List<ItemValidado> itens = validarItens(ponto.getId(), request.itens());
        OffsetDateTime agora = agora();

        SolicitacaoModel solicitacao = new SolicitacaoModel();
        solicitacao.setProtocolo(gerarProtocolo(agora));
        solicitacao.setMorador(moradorRepository.getReferenceById(morador.id()));
        solicitacao.setPonto(ponto);
        solicitacao.copiarEndereco(endereco);
        solicitacao.setObservacoes(request.observacoes());
        solicitacao.setCriadaEm(agora);
        solicitacaoRepository.saveAndFlush(solicitacao);

        adicionarItens(solicitacao, itens);
        historicoRepository.save(new HistoricoSolicitacaoModel(solicitacao, null, SituacaoSolicitacao.SOLICITADA,
                agora, ResponsavelTipo.MORADOR, morador.id(), null));
        solicitacaoRepository.flush();
        return converter(solicitacao);
    }

    /** Só em SOLICITADA (RN04). Refaz todas as validações e guarda os dados anteriores no histórico. */
    @Transactional
    public SolicitacaoResponse corrigir(ContextoAtor morador, Long id, SolicitacaoCorrecaoRequest request) {
        SolicitacaoModel solicitacao = solicitacaoRepository.travarParaAlteracao(id, morador.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação", id));
        Long versaoLida = solicitacao.getVersao();
        if (!versaoLida.equals(request.versao())) {
            throw new RegraNegocioException(CodigoErro.CONFLITO_VERSAO,
                    "A solicitação foi alterada por outra operação. Recarregue e tente novamente.");
        }
        if (solicitacao.getSituacao() != SituacaoSolicitacao.SOLICITADA) {
            throw new RegraNegocioException(CodigoErro.TRANSICAO_INVALIDA,
                    "Itens e endereço só podem ser corrigidos antes do agendamento.");
        }
        EnderecoModel endereco = buscarEnderecoDoMorador(request.enderecoId(), morador.id());
        PontoModel ponto = travarPontoDisponivel(solicitacao.getPonto().getId());
        List<ItemValidado> itens = validarItens(ponto.getId(), request.itens());
        String dadosAnteriores = descreverDadosAtuais(solicitacao);

        solicitacao.copiarEndereco(endereco);
        solicitacao.getItens().clear();
        solicitacaoRepository.flush();
        adicionarItens(solicitacao, itens);
        historicoRepository.save(new HistoricoSolicitacaoModel(solicitacao, SituacaoSolicitacao.SOLICITADA,
                SituacaoSolicitacao.SOLICITADA, agora(), ResponsavelTipo.MORADOR, morador.id(), dadosAnteriores));
        solicitacaoRepository.flush();
        garantirNovaVersao(solicitacao, versaoLida);
        return converter(solicitacao);
    }

    private EnderecoModel buscarEnderecoDoMorador(Long enderecoId, Long moradorId) {
        EnderecoModel endereco = enderecoRepository.findByIdAndMoradorId(enderecoId, moradorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Endereço", enderecoId));
        if (!endereco.isAtivo()) {
            throw new RegraNegocioException(CodigoErro.REGRA_NEGOCIO,
                    "Endereço inativo não pode ser usado em uma solicitação.");
        }
        return endereco;
    }

    /** Trava a linha do ponto (ver PontoRepository) e só então confere se está ativo e faz coleta em casa. */
    private PontoModel travarPontoDisponivel(Long pontoId) {
        pontoRepository.travarParaSolicitacao(pontoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ponto", pontoId));
        return pontoService.buscarDisponivelParaSolicitacao(pontoId);
    }

    private List<ItemValidado> validarItens(Long pontoId, List<ItemSolicitacaoRequest> itens) {
        List<ItemValidado> validados = new ArrayList<>();
        Set<Long> materiaisVistos = new HashSet<>();
        for (int i = 0; i < itens.size(); i++) {
            ItemSolicitacaoRequest item = itens.get(i);
            String campo = "itens[" + i + "].";
            if (!materiaisVistos.add(item.materialId())) {
                throw new EntradaInvalidaException(campo + "materialId", "material repetido na solicitação");
            }
            MaterialModel material = materialService.buscarAtivoParaSolicitacao(item.materialId());
            quantidadeMaterialService.validarPositiva(item.quantidadeEstimada(), material.getUnidade(),
                    campo + "quantidadeEstimada");
            PontoMaterialModel vinculo = pontoMaterialRepository
                    .findById(new PontoMaterialId(pontoId, material.getId()))
                    .orElseThrow(() -> new RegraNegocioException(CodigoErro.MATERIAL_INCOMPATIVEL,
                            "O ponto não aceita o material " + material.getNome() + "."));
            BigDecimal limite = vinculo.getLimitePorSolicitacao();
            if (limite != null && item.quantidadeEstimada().compareTo(limite) > 0) {
                throw new RegraNegocioException(CodigoErro.MATERIAL_INCOMPATIVEL, "A quantidade de "
                        + material.getNome() + " excede o limite de " + semZerosFinais(limite) + " por solicitação.");
            }
            validados.add(new ItemValidado(material, item.quantidadeEstimada()));
        }
        return validados;
    }

    private void adicionarItens(SolicitacaoModel solicitacao, List<ItemValidado> itens) {
        for (ItemValidado item : itens) {
            solicitacao.getItens().add(new ItemSolicitacaoModel(solicitacao, item.material(), item.quantidade()));
        }
    }

    /** Data local do servidor + código sem caracteres que se confundem (O/0, I/1, L). O UNIQUE do banco garante. */
    private String gerarProtocolo(OffsetDateTime agora) {
        String data = agora.atZoneSameInstant(ZoneId.systemDefault()).format(DATA_PROTOCOLO);
        String protocolo;
        do {
            StringBuilder codigo = new StringBuilder();
            for (int i = 0; i < TAMANHO_CODIGO; i++) {
                codigo.append(LETRAS_PROTOCOLO.charAt(sorteio.nextInt(LETRAS_PROTOCOLO.length())));
            }
            protocolo = "CS-" + data + "-" + codigo;
        } while (solicitacaoRepository.existsByProtocolo(protocolo));
        return protocolo;
    }

    /**
     * Trocar só os itens não altera a linha da solicitação, e o Hibernate não subiria a versão.
     * Neste caso a versão é somada direto no banco (a linha já está travada por esta transação)
     * e a solicitação é relida, para toda correção devolver versão nova.
     */
    private void garantirNovaVersao(SolicitacaoModel solicitacao, Long versaoLida) {
        if (solicitacao.getVersao().equals(versaoLida)) {
            solicitacaoRepository.incrementarVersao(solicitacao.getId());
            entityManager.refresh(solicitacao);
        }
    }

    /** Texto guardado no motivo do histórico da correção: o que valia antes de corrigir. */
    private String descreverDadosAtuais(SolicitacaoModel solicitacao) {
        String numero = solicitacao.isEndSemNumero() ? "s/n" : solicitacao.getEndNumero();
        String itens = itensOrdenados(solicitacao).stream()
                .map(item -> item.getMaterial().getNome() + " " + semZerosFinais(item.getQuantidadeEstimada()) + " "
                        + item.getUnidade())
                .collect(Collectors.joining("; "));
        return "Correção de dados. Endereço anterior: " + solicitacao.getEndLogradouro() + ", " + numero + ", "
                + solicitacao.getEndBairro() + ", " + solicitacao.getEndMunicipio() + "/" + solicitacao.getEndUf()
                + ", CEP " + solicitacao.getEndCep() + ". Itens anteriores: " + itens + ".";
    }

    private static List<ItemSolicitacaoModel> itensOrdenados(SolicitacaoModel solicitacao) {
        return solicitacao.getItens().stream()
                .sorted(Comparator.comparing(item -> item.getMaterial().getNome(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private SolicitacaoResponse converter(SolicitacaoModel solicitacao) {
        SolicitacaoResponse.EnderecoCopiado endereco = new SolicitacaoResponse.EnderecoCopiado(
                solicitacao.getEndereco().getId(), solicitacao.getEndLogradouro(), solicitacao.getEndNumero(),
                solicitacao.isEndSemNumero(), solicitacao.getEndBairro(), solicitacao.getEndMunicipio(),
                solicitacao.getEndUf(), solicitacao.getEndCep(), solicitacao.getEndComplemento());
        List<SolicitacaoResponse.Item> itens = itensOrdenados(solicitacao).stream()
                .map(item -> new SolicitacaoResponse.Item(item.getMaterial().getId(), item.getMaterial().getNome(),
                        item.getUnidade(), semZerosFinais(item.getQuantidadeEstimada()),
                        semZerosFinais(item.getQuantidadeEfetiva())))
                .toList();
        List<SolicitacaoResponse.Historico> historico = historicoRepository
                .findBySolicitacaoIdOrderByOcorridoEmAscIdAsc(solicitacao.getId()).stream()
                .map(linha -> new SolicitacaoResponse.Historico(linha.getSituacaoAnterior(), linha.getSituacaoNova(),
                        linha.getOcorridoEm(), linha.getResponsavelTipo(), linha.getResponsavelId(),
                        linha.getMotivo()))
                .toList();
        return new SolicitacaoResponse(solicitacao.getId(), solicitacao.getProtocolo(), solicitacao.getSituacao(),
                solicitacao.getVersao(), solicitacao.getCriadaEm(), solicitacao.getMorador().getId(),
                solicitacao.getPonto().getId(), endereco, solicitacao.getObservacoes(), itens, historico);
    }

    /** Sempre em UTC: é assim que o banco devolve o timestamptz, então a resposta fica uniforme. */
    private static OffsetDateTime agora() {
        return OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);
    }

    /** O banco devolve numeric(12,3) como 20.000; a resposta mostra 20, igual às outras consultas. */
    private static BigDecimal semZerosFinais(BigDecimal valor) {
        return valor == null ? null : new BigDecimal(valor.stripTrailingZeros().toPlainString());
    }
}
