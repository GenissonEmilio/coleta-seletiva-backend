package br.edu.ifs.web1.coletaseletiva.dto.operador;

import java.util.Locale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para cadastrar ou atualizar um operador de ponto")
public record OperadorRequest(
        @NotBlank @Size(max = 120) @Schema(example = "Ana Operadora") String nome,
        @NotBlank @Email @Size(max = 160)
        @Schema(description = "E-mail único; é normalizado (minúsculo, sem espaços) antes da gravação",
                example = "ana@exemplo.org") String email) {

    public OperadorRequest {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }
}
