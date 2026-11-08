package br.edu.ifs.web1.coletaseletiva.dto.morador;

import java.util.Locale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para cadastrar ou atualizar um morador")
public record MoradorRequest(
        @NotBlank @Size(max = 120) @Schema(description = "Nome do morador", example = "Maria Souza") String nome,
        @NotBlank @Email @Size(max = 160)
        @Schema(description = "E-mail único do morador; é normalizado (minúsculo, sem espaços) antes da gravação",
                example = "maria@exemplo.com") String email,
        @Size(max = 20) @Schema(description = "Telefone, opcional", example = "79999990000") String telefone) {

    public MoradorRequest {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }
}
