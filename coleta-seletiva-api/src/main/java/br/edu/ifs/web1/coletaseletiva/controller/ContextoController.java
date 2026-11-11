package br.edu.ifs.web1.coletaseletiva.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifs.web1.coletaseletiva.dto.contexto.ContextoResponse;
import br.edu.ifs.web1.coletaseletiva.services.contexto.ContextoAtor;
import br.edu.ifs.web1.coletaseletiva.services.contexto.ContextoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/contexto")
@Tag(name = "Contexto de demonstração", description = "Seleção de ator fictício. Não é autenticação nem serve "
        + "para produção")
public class ContextoController {

    private final ContextoService contextoService;

    public ContextoController(ContextoService contextoService) {
        this.contextoService = contextoService;
    }

    @GetMapping
    @Operation(summary = "Mostra quem é o ator dos cabeçalhos X-Ator-Tipo e X-Ator-Id e seus pontos",
            description = "Ator ausente, desconhecido ou inativo responde 403 ACESSO_NEGADO")
    public ContextoResponse atual(
            @RequestHeader(name = ContextoService.CABECALHO_TIPO, required = false) String atorTipo,
            @RequestHeader(name = ContextoService.CABECALHO_ID, required = false) Long atorId) {
        ContextoAtor ator = contextoService.resolver(atorTipo, atorId);
        return new ContextoResponse(ator.tipo(), ator.id(), ator.nome(), ator.pontoIds().stream().sorted().toList());
    }
}
