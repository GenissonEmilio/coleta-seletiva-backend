package br.edu.ifs.web1.coletaseletiva.controller;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.edu.ifs.web1.coletaseletiva.dto.common.PaginaResponse;
import br.edu.ifs.web1.coletaseletiva.dto.common.PaginacaoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.operador.OperadorRequest;
import br.edu.ifs.web1.coletaseletiva.dto.operador.OperadorResponse;
import br.edu.ifs.web1.coletaseletiva.services.contexto.ContextoAtor;
import br.edu.ifs.web1.coletaseletiva.services.contexto.ContextoService;
import br.edu.ifs.web1.coletaseletiva.services.operador.OperadorService;
import br.edu.ifs.web1.coletaseletiva.services.operador.OperadorService.VinculoGravado;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/operadores")
@Tag(name = "Operadores", description = "Cadastro de operadores e vínculos com pontos. Exige o cabeçalho "
        + "X-Ator-Tipo (simulação de demonstração, não é autenticação)")
public class OperadorController {

    private final OperadorService operadorService;
    private final ContextoService contextoService;

    public OperadorController(OperadorService operadorService, ContextoService contextoService) {
        this.operadorService = operadorService;
        this.contextoService = contextoService;
    }

    @PostMapping
    @Operation(summary = "Cadastra um operador (administrador); o e-mail é normalizado e deve ser único")
    public ResponseEntity<OperadorResponse> cadastrar(
            @RequestHeader(name = ContextoService.CABECALHO_TIPO, required = false) String atorTipo,
            @RequestHeader(name = ContextoService.CABECALHO_ID, required = false) Long atorId,
            @Valid @RequestBody OperadorRequest request) {
        contextoService.exigirAdministrador(contextoService.resolver(atorTipo, atorId));
        OperadorResponse operador = operadorService.cadastrar(request);
        URI localizacao = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(operador.id()).toUri();
        return ResponseEntity.created(localizacao).body(operador);
    }

    @GetMapping
    @Operation(summary = "Lista operadores por nome, com seus pontos (administrador)")
    public PaginaResponse<OperadorResponse> listar(
            @RequestHeader(name = ContextoService.CABECALHO_TIPO, required = false) String atorTipo,
            @RequestHeader(name = ContextoService.CABECALHO_ID, required = false) Long atorId,
            @RequestParam(defaultValue = PaginacaoRequest.PAGE_PADRAO) int page,
            @RequestParam(defaultValue = PaginacaoRequest.SIZE_PADRAO) int size) {
        contextoService.exigirAdministrador(contextoService.resolver(atorTipo, atorId));
        return operadorService.listar(new PaginacaoRequest(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um operador e seus vínculos (administrador ou o próprio operador)")
    public OperadorResponse buscar(
            @RequestHeader(name = ContextoService.CABECALHO_TIPO, required = false) String atorTipo,
            @RequestHeader(name = ContextoService.CABECALHO_ID, required = false) Long atorId,
            @PathVariable Long id) {
        ContextoAtor ator = contextoService.resolver(atorTipo, atorId);
        contextoService.exigirAdministradorOuProprioOperador(ator, id);
        return operadorService.buscar(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza nome e e-mail de um operador (administrador)")
    public OperadorResponse atualizar(
            @RequestHeader(name = ContextoService.CABECALHO_TIPO, required = false) String atorTipo,
            @RequestHeader(name = ContextoService.CABECALHO_ID, required = false) Long atorId,
            @PathVariable Long id, @Valid @RequestBody OperadorRequest request) {
        contextoService.exigirAdministrador(contextoService.resolver(atorTipo, atorId));
        return operadorService.atualizar(id, request);
    }

    @PutMapping("/{id}/pontos/{pontoId}")
    @Operation(summary = "Vincula o operador ao ponto (administrador); repetir nunca cria segunda linha",
            description = "201 quando o vínculo é criado, 200 quando já existia")
    public ResponseEntity<OperadorResponse> vincular(
            @RequestHeader(name = ContextoService.CABECALHO_TIPO, required = false) String atorTipo,
            @RequestHeader(name = ContextoService.CABECALHO_ID, required = false) Long atorId,
            @PathVariable Long id, @PathVariable Long pontoId) {
        contextoService.exigirAdministrador(contextoService.resolver(atorTipo, atorId));
        VinculoGravado gravado = operadorService.vincular(id, pontoId);
        return ResponseEntity.status(gravado.criado() ? HttpStatus.CREATED : HttpStatus.OK).body(gravado.operador());
    }

    @DeleteMapping("/{id}/pontos/{pontoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove o vínculo entre o operador e o ponto (administrador)")
    public void desvincular(
            @RequestHeader(name = ContextoService.CABECALHO_TIPO, required = false) String atorTipo,
            @RequestHeader(name = ContextoService.CABECALHO_ID, required = false) Long atorId,
            @PathVariable Long id, @PathVariable Long pontoId) {
        contextoService.exigirAdministrador(contextoService.resolver(atorTipo, atorId));
        operadorService.desvincular(id, pontoId);
    }
}
