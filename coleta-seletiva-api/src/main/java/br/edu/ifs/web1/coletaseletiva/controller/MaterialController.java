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
import br.edu.ifs.web1.coletaseletiva.dto.material.MaterialRequest;
import br.edu.ifs.web1.coletaseletiva.dto.material.MaterialResponse;
import br.edu.ifs.web1.coletaseletiva.dto.material.MaterialSituacaoRequest;
import br.edu.ifs.web1.coletaseletiva.services.material.MaterialService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/materiais")
@Tag(name = "Materiais", description = "Catálogo administrativo de materiais e unidades, incluindo materiais inativos")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @PostMapping
    @Operation(summary = "Cadastra um material ativo com nome único")
    public ResponseEntity<MaterialResponse> cadastrar(@Valid @RequestBody MaterialRequest request) {
        MaterialResponse material = materialService.cadastrar(request);
        URI localizacao = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(material.id()).toUri();
        return ResponseEntity.created(localizacao).body(material);
    }

    @GetMapping
    @Operation(summary = "Lista materiais por nome, com paginação e filtro opcional de situação")
    public PaginaResponse<MaterialResponse> listar(@RequestParam(required = false) Boolean ativo,
            @RequestParam(defaultValue = PaginacaoRequest.PAGE_PADRAO) int page,
            @RequestParam(defaultValue = PaginacaoRequest.SIZE_PADRAO) int size) {
        return materialService.listar(ativo, new PaginacaoRequest(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um material, inclusive se estiver inativo")
    public MaterialResponse buscar(@PathVariable Long id) {
        return materialService.buscar(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados de um material sem alterar sua situação ou cópias históricas")
    public MaterialResponse atualizar(@PathVariable Long id, @Valid @RequestBody MaterialRequest request) {
        return materialService.atualizar(id, request);
    }

    @PatchMapping("/{id}/situacao")
    @Operation(summary = "Inativa ou reativa um material, preservando seus vínculos")
    public MaterialResponse alterarSituacao(@PathVariable Long id, @Valid @RequestBody MaterialSituacaoRequest request) {
        return materialService.alterarSituacao(id, request);
    }
}
