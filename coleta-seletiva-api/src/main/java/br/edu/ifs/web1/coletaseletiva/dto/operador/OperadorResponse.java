package br.edu.ifs.web1.coletaseletiva.dto.operador;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Operador cadastrado e os pontos aos quais está vinculado")
public record OperadorResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Ana Operadora") String nome,
        @Schema(example = "ana@exemplo.org") String email,
        @Schema(example = "true") boolean ativo,
        @Schema(description = "Pontos vinculados, ordenados por id") List<PontoVinculado> pontos) {

    @Schema(description = "Ponto ao qual o operador está vinculado")
    public record PontoVinculado(@Schema(example = "1") Long id, @Schema(example = "Ecoponto Centro") String nome) {
    }
}
