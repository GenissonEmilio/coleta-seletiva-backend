package br.edu.ifs.web1.coletaseletiva.dto.contexto;

import java.util.List;

import br.edu.ifs.web1.coletaseletiva.services.contexto.TipoAtor;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ator de demonstração resolvido pelo backend. Simulação, não autenticação")
public record ContextoResponse(
        @Schema(example = "OPERADOR") TipoAtor tipo,
        @Schema(description = "Nulo para o administrador", example = "1") Long id,
        @Schema(example = "Ana Operadora") String nome,
        @Schema(description = "Pontos do operador, ordenados; vazio para os outros atores") List<Long> pontoIds) {
}
