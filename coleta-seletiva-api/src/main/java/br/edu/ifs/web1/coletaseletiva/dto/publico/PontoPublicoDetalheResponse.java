package br.edu.ifs.web1.coletaseletiva.dto.publico;

import java.util.List;

import br.edu.ifs.web1.coletaseletiva.dto.pontomaterial.PontoMaterialResponse;

import io.swagger.v3.oas.annotations.media.Schema;

/** Detalhe público: o ponto e os materiais ativos que ele aceita, com unidade, orientação e limite. */
@Schema(description = "Detalhe público de um ponto ativo com seus materiais aceitos")
public record PontoPublicoDetalheResponse(
        PontoPublicoResponse ponto,
        @Schema(description = "Materiais ativos aceitos, ordenados por nome") List<PontoMaterialResponse> materiais) {
}
