package br.edu.ifs.web1.coletaseletiva.dto.material;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import br.edu.ifs.web1.coletaseletiva.model.UnidadeMaterial;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para cadastrar ou atualizar um material; a situação é alterada em operação própria")
public record MaterialRequest(
        @NotBlank @Size(max = 80)
        @Schema(description = "Nome único, ignorando maiúsculas e espaços nas pontas", example = "Papel") String nome,
        @Schema(description = "Descrição opcional", example = "Papel e papelão secos") String descricao,
        @NotNull @Schema(description = "KG e LITRO permitem fração; UNIDADE exige quantidades inteiras", example = "KG")
        UnidadeMaterial unidade,
        @Schema(description = "Orientação opcional de preparo", example = "Separar limpo e seco") String orientacaoPreparo) {

    public MaterialRequest {
        if (nome != null) {
            nome = nome.trim();
        }
    }
}
