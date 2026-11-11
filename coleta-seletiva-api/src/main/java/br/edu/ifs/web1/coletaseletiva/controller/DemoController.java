package br.edu.ifs.web1.coletaseletiva.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifs.web1.coletaseletiva.dto.contexto.ContextoResponse;
import br.edu.ifs.web1.coletaseletiva.services.contexto.DadosDemoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Só existe no perfil local "demo"; fora dele, a rota responde 404. */
@RestController
@Profile("demo")
@RequestMapping("/api/v1/demo")
@Tag(name = "Contexto de demonstração")
public class DemoController {

    private final DadosDemoService dadosDemoService;

    public DemoController(DadosDemoService dadosDemoService) {
        this.dadosDemoService = dadosDemoService;
    }

    @GetMapping("/atores")
    @Operation(summary = "Lista os atores fictícios que o front pode selecionar (só no perfil demo)")
    public List<ContextoResponse> atores() {
        return dadosDemoService.listarAtores();
    }
}
