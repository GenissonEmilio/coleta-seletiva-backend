package br.edu.ifs.web1.coletaseletiva.dto.endereco;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Endereço cadastrado de um morador")
public record EnderecoResponse(
        @Schema(description = "Identificador do endereço", example = "1") Long id,
        @Schema(description = "Identificador do morador proprietário do endereço", example = "1") Long moradorId,
        @Schema(description = "Rua, avenida ou similar", example = "Rua das Flores") String logradouro,
        @Schema(description = "Número do imóvel; nulo quando semNumero é verdadeiro", example = "120") String numero,
        @Schema(description = "Verdadeiro quando o imóvel não tem número", example = "false") boolean semNumero,
        @Schema(description = "Bairro", example = "Centro") String bairro,
        @Schema(description = "Município", example = "Cidade Teste") String municipio,
        @Schema(description = "Sigla da UF em maiúsculas", example = "SE") String uf,
        @Schema(description = "CEP com 8 dígitos, só números", example = "49000000") String cep,
        @Schema(description = "Complemento, quando informado", example = "Apto 2") String complemento,
        @Schema(description = "Indica se o endereço está ativo; endereços são inativados, não excluídos",
                example = "true") boolean ativo) {
}
