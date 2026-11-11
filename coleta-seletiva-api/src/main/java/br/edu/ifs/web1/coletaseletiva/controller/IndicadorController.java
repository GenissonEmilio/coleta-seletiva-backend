package br.edu.ifs.web1.coletaseletiva.controller;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.ifs.web1.coletaseletiva.dto.common.PaginaResponse;
import br.edu.ifs.web1.coletaseletiva.dto.common.PaginacaoRequest;
import br.edu.ifs.web1.coletaseletiva.dto.indicador.RecolhimentoPorMaterialResponse;
import br.edu.ifs.web1.coletaseletiva.dto.indicador.SolicitacoesPorSituacaoResponse;
import br.edu.ifs.web1.coletaseletiva.services.contexto.ContextoService;
import br.edu.ifs.web1.coletaseletiva.services.indicador.IndicadorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/indicadores")
@Tag(name = "Indicadores", description = "Resumo gerencial no contexto de demonstração do administrador")
public class IndicadorController {

    private final IndicadorService indicadorService;
    private final ContextoService contextoService;

    public IndicadorController(IndicadorService indicadorService, ContextoService contextoService) {
        this.indicadorService = indicadorService;
        this.contextoService = contextoService;
    }

    @GetMapping("/solicitacoes")
    @Operation(summary = "Conta as solicitações por situação atual e período de criação",
            description = "Início inclusivo, fim exclusivo. Datas em ISO 8601 com offset.")
    public List<SolicitacoesPorSituacaoResponse> solicitacoes(
            @RequestHeader(name = ContextoService.CABECALHO_TIPO, required = false) String atorTipo,
            @RequestHeader(name = ContextoService.CABECALHO_ID, required = false) Long atorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fim) {
        contextoService.exigirAdministrador(contextoService.resolver(atorTipo, atorId));
        return indicadorService.contarSolicitacoes(inicio, fim);
    }

    @GetMapping("/recolhimentos")
    @Operation(summary = "Soma os efetivos por material e unidade copiada, pela data de conclusão",
            description = "Considera somente solicitações concluídas. Início inclusivo, fim exclusivo.")
    public PaginaResponse<RecolhimentoPorMaterialResponse> recolhimentos(
            @RequestHeader(name = ContextoService.CABECALHO_TIPO, required = false) String atorTipo,
            @RequestHeader(name = ContextoService.CABECALHO_ID, required = false) Long atorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fim,
            @RequestParam(defaultValue = PaginacaoRequest.PAGE_PADRAO) int page,
            @RequestParam(defaultValue = PaginacaoRequest.SIZE_PADRAO) int size) {
        contextoService.exigirAdministrador(contextoService.resolver(atorTipo, atorId));
        return indicadorService.somarRecolhimentos(inicio, fim, new PaginacaoRequest(page, size));
    }
}
