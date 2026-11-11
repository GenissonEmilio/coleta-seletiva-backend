package br.edu.ifs.web1.coletaseletiva.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifs.web1.coletaseletiva.dto.common.PaginaResponse;
import br.edu.ifs.web1.coletaseletiva.dto.common.PaginacaoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.material.MaterialResponse;
import br.edu.ifs.web1.coletaseletiva.dto.publico.PontoPublicoDetalheResponse;
import br.edu.ifs.web1.coletaseletiva.dto.publico.PontoPublicoResponse;
import br.edu.ifs.web1.coletaseletiva.services.publico.ConsultaPublicaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/publico")
@Tag(name = "Consulta pública", description = "Pontos e materiais ativos, sem dados de moradores ou operadores")
public class ConsultaPublicaController {

    private final ConsultaPublicaService consultaPublicaService;

    public ConsultaPublicaController(ConsultaPublicaService consultaPublicaService) {
        this.consultaPublicaService = consultaPublicaService;
    }

    @GetMapping("/pontos")
    @Operation(summary = "Busca pontos ativos por nome (contém), município, bairro e material aceito",
            description = "Filtros opcionais e combináveis, sem diferenciar maiúsculas. Ordem por nome e id")
    public PaginaResponse<PontoPublicoResponse> buscarPontos(@RequestParam(required = false) String nome,
            @RequestParam(required = false) String municipio,
            @RequestParam(required = false) String bairro,
            @RequestParam(required = false) Long materialId,
            @RequestParam(defaultValue = PaginacaoRequest.PAGE_PADRAO) int page,
            @RequestParam(defaultValue = PaginacaoRequest.SIZE_PADRAO) int size) {
        return consultaPublicaService.buscarPontos(nome, municipio, bairro, materialId,
                new PaginacaoRequest(page, size));
    }

    @GetMapping("/pontos/{id}")
    @Operation(summary = "Detalha um ponto ativo com endereço, contato, horários e materiais aceitos",
            description = "Ponto inativo responde 404, como se não existisse")
    public PontoPublicoDetalheResponse detalharPonto(@PathVariable Long id) {
        return consultaPublicaService.detalharPonto(id);
    }

    @GetMapping("/materiais")
    @Operation(summary = "Lista os materiais ativos, por nome, com unidade e orientação de preparo")
    public PaginaResponse<MaterialResponse> listarMateriais(
            @RequestParam(defaultValue = PaginacaoRequest.PAGE_PADRAO) int page,
            @RequestParam(defaultValue = PaginacaoRequest.SIZE_PADRAO) int size) {
        return consultaPublicaService.listarMateriais(new PaginacaoRequest(page, size));
    }
}
