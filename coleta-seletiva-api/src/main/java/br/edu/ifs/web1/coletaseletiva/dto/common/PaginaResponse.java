package br.edu.ifs.web1.coletaseletiva.dto.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Envelope padrão de listagens paginadas.
 * Existe para não expor a estrutura interna do Page do Spring e manter o mesmo formato em todos os módulos.
 */
@Schema(description = "Página de resultados")
public record PaginaResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PaginaResponse<T> de(Page<T> pagina) {
        return new PaginaResponse<>(pagina.getContent(), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }

    /** Converte entidade em DTO ao montar a resposta. */
    public static <E, T> PaginaResponse<T> de(Page<E> pagina, Function<E, T> conversor) {
        return de(pagina.map(conversor));
    }
}
