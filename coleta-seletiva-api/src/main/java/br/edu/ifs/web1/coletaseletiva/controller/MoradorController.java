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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.edu.ifs.web1.coletaseletiva.dto.morador.MoradorRequest;
import br.edu.ifs.web1.coletaseletiva.dto.morador.MoradorResponse;
import br.edu.ifs.web1.coletaseletiva.services.morador.MoradorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/moradores")
@Tag(name = "Moradores", description = "Cadastro, consulta e atualização de moradores")
public class MoradorController {

    private final MoradorService moradorService;

    public MoradorController(MoradorService moradorService) {
        this.moradorService = moradorService;
    }

    @PostMapping
    @Operation(summary = "Cadastra um morador; o e-mail é normalizado e deve ser único")
    public ResponseEntity<MoradorResponse> cadastrar(@Valid @RequestBody MoradorRequest request) {
        MoradorResponse morador = moradorService.cadastrar(request);
        URI localizacao = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(morador.id()).toUri();
        return ResponseEntity.created(localizacao).body(morador);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um morador pelo identificador")
    public MoradorResponse buscar(@PathVariable Long id) {
        return moradorService.buscar(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza nome, e-mail e telefone de um morador")
    public MoradorResponse atualizar(@PathVariable Long id, @Valid @RequestBody MoradorRequest request) {
        return moradorService.atualizar(id, request);
    }
}
