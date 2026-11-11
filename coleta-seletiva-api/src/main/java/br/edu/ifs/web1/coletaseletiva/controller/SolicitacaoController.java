package br.edu.ifs.web1.coletaseletiva.controller;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.edu.ifs.web1.coletaseletiva.dto.common.PaginaResponse;
import br.edu.ifs.web1.coletaseletiva.dto.common.PaginacaoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.solicitacao.SolicitacaoCorrecaoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.solicitacao.SolicitacaoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.solicitacao.SolicitacaoResponse;
import br.edu.ifs.web1.coletaseletiva.dto.solicitacao.SolicitacaoResumoResponse;
import br.edu.ifs.web1.coletaseletiva.model.SituacaoSolicitacao;
import br.edu.ifs.web1.coletaseletiva.services.contexto.ContextoAtor;
import br.edu.ifs.web1.coletaseletiva.services.contexto.ContextoService;
import br.edu.ifs.web1.coletaseletiva.services.solicitacao.SolicitacaoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/solicitacoes")
@Tag(name = "Solicitações", description = "Criação e correção pelo morador; consultas pelo contexto do ator "
        + "(X-Ator-Tipo e X-Ator-Id). Simulação de demonstração, não é autenticação")
public class SolicitacaoController {

    private final SolicitacaoService solicitacaoService;
    private final ContextoService contextoService;

    public SolicitacaoController(SolicitacaoService solicitacaoService, ContextoService contextoService) {
        this.solicitacaoService = solicitacaoService;
        this.contextoService = contextoService;
    }

    @PostMapping
    @Operation(summary = "Cria uma solicitação com itens e histórico inicial, em uma única transação",
            description = "Exige ponto ativo com recolhimento domiciliar e materiais ativos, aceitos, sem "
                    + "repetição e dentro do limite. Qualquer erro não grava nada")
    public ResponseEntity<SolicitacaoResponse> criar(
            @RequestHeader(name = ContextoService.CABECALHO_TIPO, required = false) String atorTipo,
            @RequestHeader(name = ContextoService.CABECALHO_ID, required = false) Long atorId,
            @Valid @RequestBody SolicitacaoRequest request) {
        SolicitacaoResponse solicitacao = solicitacaoService.criar(resolverMorador(atorTipo, atorId), request);
        URI localizacao = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(solicitacao.id()).toUri();
        return ResponseEntity.created(localizacao).body(solicitacao);
    }

    @PutMapping("/{id}/dados")
    @Operation(summary = "Corrige endereço e itens de uma solicitação ainda em SOLICITADA",
            description = "Recebe a versão lida pelo cliente, refaz todas as validações e registra os dados "
                    + "anteriores no histórico. Versão antiga: 409 CONFLITO_VERSAO; já agendada: 409 "
                    + "TRANSICAO_INVALIDA")
    public SolicitacaoResponse corrigir(
            @RequestHeader(name = ContextoService.CABECALHO_TIPO, required = false) String atorTipo,
            @RequestHeader(name = ContextoService.CABECALHO_ID, required = false) Long atorId,
            @PathVariable Long id, @Valid @RequestBody SolicitacaoCorrecaoRequest request) {
        return solicitacaoService.corrigir(resolverMorador(atorTipo, atorId), id, request);
    }

    @GetMapping
    @Operation(summary = "Lista solicitações do contexto do ator, da mais recente para a mais antiga",
            description = "Morador vê as próprias; operador, as dos seus pontos; administrador, todas. Filtros "
                    + "opcionais e combináveis. Período pela data de criação, com início inclusivo e fim "
                    + "exclusivo, em ISO 8601 com offset (ex.: 2026-11-01T00:00:00Z)")
    public PaginaResponse<SolicitacaoResumoResponse> listar(
            @RequestHeader(name = ContextoService.CABECALHO_TIPO, required = false) String atorTipo,
            @RequestHeader(name = ContextoService.CABECALHO_ID, required = false) Long atorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fim,
            @RequestParam(required = false) SituacaoSolicitacao situacao,
            @RequestParam(required = false) Long pontoId,
            @RequestParam(defaultValue = PaginacaoRequest.PAGE_PADRAO) int page,
            @RequestParam(defaultValue = PaginacaoRequest.SIZE_PADRAO) int size) {
        return solicitacaoService.listar(contextoService.resolver(atorTipo, atorId), inicio, fim, situacao, pontoId,
                new PaginacaoRequest(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma solicitação do contexto do ator, com itens, endereço copiado e histórico",
            description = "Fora do contexto responde 404, como se não existisse")
    public SolicitacaoResponse buscar(
            @RequestHeader(name = ContextoService.CABECALHO_TIPO, required = false) String atorTipo,
            @RequestHeader(name = ContextoService.CABECALHO_ID, required = false) Long atorId,
            @PathVariable Long id) {
        return solicitacaoService.buscar(contextoService.resolver(atorTipo, atorId), id);
    }

    @GetMapping("/{id}/historico")
    @Operation(summary = "Histórico da solicitação em ordem cronológica: situações, data, responsável e motivo",
            description = "Fora do contexto responde 404, como se não existisse")
    public List<SolicitacaoResponse.Historico> listarHistorico(
            @RequestHeader(name = ContextoService.CABECALHO_TIPO, required = false) String atorTipo,
            @RequestHeader(name = ContextoService.CABECALHO_ID, required = false) Long atorId,
            @PathVariable Long id) {
        return solicitacaoService.listarHistorico(contextoService.resolver(atorTipo, atorId), id);
    }

    private ContextoAtor resolverMorador(String atorTipo, Long atorId) {
        ContextoAtor ator = contextoService.resolver(atorTipo, atorId);
        contextoService.exigirMorador(ator);
        return ator;
    }
}
