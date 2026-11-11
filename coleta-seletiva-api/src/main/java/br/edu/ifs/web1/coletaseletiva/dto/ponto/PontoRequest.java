package br.edu.ifs.web1.coletaseletiva.dto.ponto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para cadastrar ou atualizar um ponto de coleta")
public record PontoRequest(
        @NotBlank @Size(max = 120) @Schema(description = "Nome do ponto", example = "Ecoponto Centro") String nome,
        @NotBlank @Size(max = 150) @Schema(description = "Rua, avenida ou similar", example = "Rua das Flores")
        String logradouro,
        @Size(max = 10) @Schema(description = "Número do imóvel; vazio quando semNumero é verdadeiro", example = "120")
        String numero,
        @Schema(description = "Verdadeiro quando o imóvel não tem número; ausente vale falso", example = "false")
        Boolean semNumero,
        @NotBlank @Size(max = 80) @Schema(description = "Bairro", example = "Centro") String bairro,
        @NotBlank @Size(max = 80) @Schema(description = "Município", example = "Cidade Teste") String municipio,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "deve ter 2 letras")
        @Schema(description = "Sigla da UF, 2 letras", example = "SE") String uf,
        @NotBlank @Pattern(regexp = "\\d{8}", message = "deve ter 8 dígitos, sem traço")
        @Schema(description = "CEP com 8 dígitos, só números", example = "49000000") String cep,
        @Size(max = 80) @Schema(description = "Complemento, opcional", example = "Galpão 2") String complemento,
        @Size(max = 120) @Schema(description = "Contato do ponto, opcional", example = "ecoponto@exemplo.org")
        String contato,
        @Schema(description = "Horários de funcionamento, texto livre, opcional", example = "Seg a sex, 8h às 17h")
        String horarios,
        @NotNull @Schema(description = "Verdadeiro se o ponto faz recolhimento domiciliar", example = "true")
        Boolean recolhimentoDomiciliar) {

    public PontoRequest {
        if (semNumero == null) {
            semNumero = false;
        }
    }
}
