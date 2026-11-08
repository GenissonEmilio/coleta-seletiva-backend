package br.edu.ifs.web1.coletaseletiva.dto.endereco;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para cadastrar ou atualizar um endereço de morador")
public record EnderecoRequest(
        @NotBlank @Size(max = 150) @Schema(description = "Rua, avenida ou similar", example = "Rua das Flores")
        String logradouro,
        @Size(max = 10) @Schema(description = "Número do imóvel, texto por causa de casos como 10A; vazio quando semNumero é verdadeiro",
                example = "120") String numero,
        @Schema(description = "Verdadeiro quando o imóvel não tem número; ausente vale falso", example = "false")
        Boolean semNumero,
        @NotBlank @Size(max = 80) @Schema(description = "Bairro", example = "Centro") String bairro,
        @NotBlank @Size(max = 80) @Schema(description = "Município", example = "Cidade Teste") String municipio,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "deve ter 2 letras")
        @Schema(description = "Sigla da UF, 2 letras", example = "SE") String uf,
        @NotBlank @Pattern(regexp = "\\d{8}", message = "deve ter 8 dígitos, sem traço")
        @Schema(description = "CEP com 8 dígitos, só números", example = "49000000") String cep,
        @Size(max = 80) @Schema(description = "Complemento, opcional", example = "Apto 2") String complemento) {

    public EnderecoRequest {
        if (semNumero == null) {
            semNumero = false;
        }
    }
}
