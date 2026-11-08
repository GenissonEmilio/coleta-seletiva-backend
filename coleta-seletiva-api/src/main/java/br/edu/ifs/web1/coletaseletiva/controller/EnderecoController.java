package br.edu.ifs.web1.coletaseletiva.controller;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
import br.edu.ifs.web1.coletaseletiva.dto.endereco.EnderecoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.endereco.EnderecoResponse;
import br.edu.ifs.web1.coletaseletiva.services.endereco.EnderecoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/moradores/{moradorId}/enderecos")
@Tag(name = "Endereços", description = "Endereços de um morador; cada endereço pertence a um único morador")
public class EnderecoController {

    private final EnderecoService enderecoService;

    public EnderecoController(EnderecoService enderecoService) {
        this.enderecoService = enderecoService;
    }

    @PostMapping
    @Operation(summary = "Cadastra um endereço para o morador")
    public ResponseEntity<EnderecoResponse> cadastrar(@PathVariable Long moradorId,
            @Valid @RequestBody EnderecoRequest request) {
        EnderecoResponse endereco = enderecoService.cadastrar(moradorId, request);
        URI localizacao = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(endereco.id()).toUri();
        return ResponseEntity.created(localizacao).body(endereco);
    }

    @GetMapping
    @Operation(summary = "Lista, em páginas, os endereços do morador")
    public PaginaResponse<EnderecoResponse> listar(@PathVariable Long moradorId,
            @RequestParam(defaultValue = PaginacaoRequest.PAGE_PADRAO) int page,
            @RequestParam(defaultValue = PaginacaoRequest.SIZE_PADRAO) int size) {
        return enderecoService.listar(moradorId, new PaginacaoRequest(page, size));
    }

    @PutMapping("/{enderecoId}")
    @Operation(summary = "Atualiza um endereço do morador; endereço de outro morador responde 404")
    public EnderecoResponse atualizar(@PathVariable Long moradorId, @PathVariable Long enderecoId,
            @Valid @RequestBody EnderecoRequest request) {
        return enderecoService.atualizar(moradorId, enderecoId, request);
    }
}
