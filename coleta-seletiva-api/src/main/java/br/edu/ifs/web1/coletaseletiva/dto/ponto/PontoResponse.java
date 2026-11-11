package br.edu.ifs.web1.coletaseletiva.dto.ponto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ponto de coleta cadastrado, incluindo sua situação atual")
public record PontoResponse(
        @Schema(description = "Identificador do ponto", example = "1") Long id,
        @Schema(description = "Nome do ponto", example = "Ecoponto Centro") String nome,
        @Schema(description = "Rua, avenida ou similar", example = "Rua das Flores") String logradouro,
        @Schema(description = "Número do imóvel; nulo quando semNumero é verdadeiro", example = "120") String numero,
        @Schema(description = "Verdadeiro quando o imóvel não tem número", example = "false") boolean semNumero,
        @Schema(description = "Bairro", example = "Centro") String bairro,
        @Schema(description = "Município", example = "Cidade Teste") String municipio,
        @Schema(description = "Sigla da UF em maiúsculas", example = "SE") String uf,
        @Schema(description = "CEP com 8 dígitos, só números", example = "49000000") String cep,
        @Schema(description = "Complemento, quando informado", example = "Galpão 2") String complemento,
        @Schema(description = "Contato do ponto, quando informado", example = "ecoponto@exemplo.org") String contato,
        @Schema(description = "Horários de funcionamento, quando informados", example = "Seg a sex, 8h às 17h")
        String horarios,
        @Schema(description = "Verdadeiro se o ponto faz recolhimento domiciliar", example = "true")
        boolean recolhimentoDomiciliar,
        @Schema(description = "Falso após a inativação; o registro é preservado", example = "true") boolean ativo) {
}
