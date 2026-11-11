package br.edu.ifs.web1.coletaseletiva.controller;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.edu.ifs.web1.coletaseletiva.dto.solicitacao.SolicitacaoCorrecaoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.solicitacao.SolicitacaoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.solicitacao.SolicitacaoResponse;
import br.edu.ifs.web1.coletaseletiva.services.contexto.ContextoAtor;
import br.edu.ifs.web1.coletaseletiva.services.contexto.ContextoService;
import br.edu.ifs.web1.coletaseletiva.services.solicitacao.SolicitacaoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/solicitacoes")
@Tag(name = "Solicitações", description = "Criação e correção pelo morador do contexto (X-Ator-Tipo: MORADOR e "
        + "X-Ator-Id). Simulação de demonstração, não é autenticação")
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

    private ContextoAtor resolverMorador(String atorTipo, Long atorId) {
        ContextoAtor ator = contextoService.resolver(atorTipo, atorId);
        contextoService.exigirMorador(ator);
        return ator;
    }
}
