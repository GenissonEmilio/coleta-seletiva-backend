package br.edu.ifs.web1.coletaseletiva.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifs.web1.coletaseletiva.dto.common.PaginaResponse;
import br.edu.ifs.web1.coletaseletiva.dto.common.PaginacaoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.pontomaterial.PontoMaterialRequest;
import br.edu.ifs.web1.coletaseletiva.dto.pontomaterial.PontoMaterialResponse;
import br.edu.ifs.web1.coletaseletiva.services.pontomaterial.PontoMaterialService;
import br.edu.ifs.web1.coletaseletiva.services.pontomaterial.PontoMaterialService.VinculoGravado;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/pontos/{pontoId}/materiais")
@Tag(name = "Materiais aceitos", description = "Vínculo entre pontos de coleta e materiais, com observações e limite")
public class PontoMaterialController {

    private final PontoMaterialService pontoMaterialService;

    public PontoMaterialController(PontoMaterialService pontoMaterialService) {
        this.pontoMaterialService = pontoMaterialService;
    }

    @PutMapping("/{materialId}")
    @Operation(summary = "Vincula um material ao ponto ou atualiza o vínculo; repetir nunca cria segunda linha",
            description = "201 quando o vínculo é criado, 200 quando já existia e foi atualizado")
    public ResponseEntity<PontoMaterialResponse> vincular(@PathVariable Long pontoId, @PathVariable Long materialId,
            @Valid @RequestBody PontoMaterialRequest request) {
        VinculoGravado gravado = pontoMaterialService.vincular(pontoId, materialId, request);
        return ResponseEntity.status(gravado.criado() ? HttpStatus.CREATED : HttpStatus.OK).body(gravado.vinculo());
    }

    @GetMapping
    @Operation(summary = "Lista os materiais aceitos pelo ponto, por nome, com unidade, orientação e limite")
    public PaginaResponse<PontoMaterialResponse> listar(@PathVariable Long pontoId,
            @RequestParam(defaultValue = PaginacaoRequest.PAGE_PADRAO) int page,
            @RequestParam(defaultValue = PaginacaoRequest.SIZE_PADRAO) int size) {
        return pontoMaterialService.listar(pontoId, new PaginacaoRequest(page, size));
    }

    @DeleteMapping("/{materialId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove o vínculo entre o ponto e o material")
    public void remover(@PathVariable Long pontoId, @PathVariable Long materialId) {
        pontoMaterialService.remover(pontoId, materialId);
    }
}
