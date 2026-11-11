package br.edu.ifs.web1.coletaseletiva.dto.solicitacao;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import br.edu.ifs.web1.coletaseletiva.model.ResponsavelTipo;
import br.edu.ifs.web1.coletaseletiva.model.SituacaoSolicitacao;
import br.edu.ifs.web1.coletaseletiva.model.UnidadeMaterial;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitação com as cópias do endereço e das unidades, os itens e o histórico")
public record SolicitacaoResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "CS-20261111-K7P3QX") String protocolo,
        @Schema(example = "SOLICITADA") SituacaoSolicitacao situacao,
        @Schema(description = "Enviar de volta nas alterações", example = "0") Long versao,
        @Schema(example = "2026-11-11T14:30:00-03:00") OffsetDateTime criadaEm,
        @Schema(example = "1") Long moradorId,
        @Schema(example = "1") Long pontoId,
        EnderecoCopiado endereco,
        @Schema(example = "Portão azul, tocar a campainha") String observacoes,
        @Schema(description = "Ordenados pelo nome do material") List<Item> itens,
        @Schema(description = "Em ordem cronológica") List<Historico> historico) {

    @Schema(description = "Cópia do endereço no momento da solicitação; não muda se o cadastro mudar")
    public record EnderecoCopiado(
            @Schema(description = "Cadastro de onde a cópia foi tirada", example = "1") Long enderecoId,
            @Schema(example = "Rua das Flores") String logradouro,
            @Schema(example = "200") String numero,
            @Schema(example = "false") boolean semNumero,
            @Schema(example = "Centro") String bairro,
            @Schema(example = "Cidade Teste") String municipio,
            @Schema(example = "SE") String uf,
            @Schema(example = "49000000") String cep,
            @Schema(example = "Apto 2") String complemento) {
    }

    @Schema(description = "Item da solicitação, com a unidade copiada do material")
    public record Item(
            @Schema(example = "2") Long materialId,
            @Schema(example = "Vidro") String nomeMaterial,
            @Schema(example = "KG") UnidadeMaterial unidade,
            @Schema(example = "2.5") BigDecimal quantidadeEstimada,
            @Schema(description = "Nulo até a conclusão") BigDecimal quantidadeEfetiva) {
    }

    @Schema(description = "Alteração registrada: situação anterior e nova, data, responsável e motivo")
    public record Historico(
            @Schema(description = "Nulo na criação") SituacaoSolicitacao situacaoAnterior,
            @Schema(example = "SOLICITADA") SituacaoSolicitacao situacaoNova,
            @Schema(example = "2026-11-11T14:30:00-03:00") OffsetDateTime ocorridoEm,
            @Schema(example = "MORADOR") ResponsavelTipo responsavelTipo,
            @Schema(example = "1") Long responsavelId,
            @Schema(description = "Na correção, guarda o endereço e os itens anteriores") String motivo) {
    }
}
