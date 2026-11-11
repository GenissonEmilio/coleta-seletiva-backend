package br.edu.ifs.web1.coletaseletiva.controller;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.edu.ifs.web1.coletaseletiva.dto.common.PaginaResponse;
import br.edu.ifs.web1.coletaseletiva.dto.common.PaginacaoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.ponto.PontoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.ponto.PontoResponse;
import br.edu.ifs.web1.coletaseletiva.dto.ponto.PontoSituacaoRequest;
import br.edu.ifs.web1.coletaseletiva.services.ponto.PontoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/pontos")
@Tag(name = "Pontos de coleta", description = "Cadastro administrativo de pontos de coleta, incluindo pontos inativos")
public class PontoController {

    private final PontoService pontoService;

    public PontoController(PontoService pontoService) {
        this.pontoService = pontoService;
    }

    @PostMapping
    @Operation(summary = "Cadastra um ponto de coleta ativo")
    public ResponseEntity<PontoResponse> cadastrar(@Valid @RequestBody PontoRequest request) {
        PontoResponse ponto = pontoService.cadastrar(request);
        URI localizacao = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(ponto.id()).toUri();
        return ResponseEntity.created(localizacao).body(ponto);
    }

    @GetMapping
    @Operation(summary = "Lista pontos de coleta por nome, com paginação e filtro opcional de situação")
    public PaginaResponse<PontoResponse> listar(@RequestParam(required = false) Boolean ativo,
            @RequestParam(defaultValue = PaginacaoRequest.PAGE_PADRAO) int page,
            @RequestParam(defaultValue = PaginacaoRequest.SIZE_PADRAO) int size) {
        return pontoService.listar(ativo, new PaginacaoRequest(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um ponto de coleta, inclusive se estiver inativo")
    public PontoResponse buscar(@PathVariable Long id) {
        return pontoService.buscar(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados de um ponto de coleta sem alterar sua situação")
    public PontoResponse atualizar(@PathVariable Long id, @Valid @RequestBody PontoRequest request) {
        return pontoService.atualizar(id, request);
    }

    @PatchMapping("/{id}/situacao")
    @Operation(summary = "Inativa ou reativa um ponto de coleta, preservando seu cadastro")
    public PontoResponse alterarSituacao(@PathVariable Long id, @Valid @RequestBody PontoSituacaoRequest request) {
        return pontoService.alterarSituacao(id, request);
    }
}
