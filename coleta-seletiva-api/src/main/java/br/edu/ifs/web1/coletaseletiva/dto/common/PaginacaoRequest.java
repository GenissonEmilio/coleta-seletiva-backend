package br.edu.ifs.web1.coletaseletiva.dto.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import io.swagger.v3.oas.annotations.media.Schema;

/** Parâmetros de entrada compartilhados pelas listagens paginadas. */
@Schema(description = "Parâmetros de paginação")
public record PaginacaoRequest(
        @Min(0) @Schema(defaultValue = "0", minimum = "0") int page,
        @Min(1) @Max(100) @Schema(defaultValue = "20", minimum = "1", maximum = "100") int size) {

    public static final String PAGE_PADRAO = "0";
    public static final String SIZE_PADRAO = "20";
    public static final int SIZE_MAXIMO = 100;
}
