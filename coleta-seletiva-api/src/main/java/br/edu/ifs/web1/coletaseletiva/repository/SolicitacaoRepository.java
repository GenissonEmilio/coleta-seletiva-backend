package br.edu.ifs.web1.coletaseletiva.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.ifs.web1.coletaseletiva.dto.indicador.RecolhimentoPorMaterialResponse;
import br.edu.ifs.web1.coletaseletiva.dto.indicador.SolicitacoesPorSituacaoResponse;
import br.edu.ifs.web1.coletaseletiva.model.SituacaoSolicitacao;
import br.edu.ifs.web1.coletaseletiva.model.SolicitacaoModel;

public interface SolicitacaoRepository extends JpaRepository<SolicitacaoModel, Long> {

    String ITENS_RECOLHIDOS_NO_PERIODO = """
            from ItemSolicitacaoModel i join i.solicitacao s join i.material m
            where s.situacao = br.edu.ifs.web1.coletaseletiva.model.SituacaoSolicitacao.CONCLUIDA
            and s.concluidaEm is not null
            and i.quantidadeEfetiva is not null
            and (cast(:inicio as OffsetDateTime) is null or s.concluidaEm >= :inicio)
            and (cast(:fim as OffsetDateTime) is null or s.concluidaEm < :fim)
            """;

    boolean existsByProtocolo(String protocolo);

    Optional<SolicitacaoModel> findByIdAndMoradorId(Long id, Long moradorId);

    /** Correções que só mudam itens precisam disputar a versão da linha da solicitação também. */
    @Modifying
    @Query("update SolicitacaoModel s set s.versao = s.versao + 1 where s.id = :id and s.versao = :versao")
    int incrementarVersao(@Param("id") Long id, @Param("versao") Long versao);

    /**
     * Lista da API-11. O contexto do ator vem do Service: moradorId preenchido para o morador,
     * todosOsPontos = false com os pontos do operador, e sem restrição para o administrador.
     * Filtros ausentes chegam já trocados por valores que não restringem (todas as situações, período aberto).
     */
    @EntityGraph(attributePaths = "ponto")
    @Query("""
            select s from SolicitacaoModel s
            where (:moradorId is null or s.morador.id = :moradorId)
            and (:todosOsPontos = true or s.ponto.id in :pontoIds)
            and (:pontoId is null or s.ponto.id = :pontoId)
            and s.situacao in :situacoes
            and s.criadaEm >= :inicio and s.criadaEm < :fim
            """)
    Page<SolicitacaoModel> consultar(@Param("moradorId") Long moradorId,
            @Param("todosOsPontos") boolean todosOsPontos, @Param("pontoIds") Collection<Long> pontoIds,
            @Param("pontoId") Long pontoId, @Param("situacoes") Collection<SituacaoSolicitacao> situacoes,
            @Param("inicio") OffsetDateTime inicio, @Param("fim") OffsetDateTime fim, Pageable pageable);

    @Query("""
            select count(s) from SolicitacaoModel s
            where s.ponto.id = :pontoId and s.situacao in :situacoes
            and (:materialId is null or exists (
                select 1 from ItemSolicitacaoModel i where i.solicitacao = s and i.material.id = :materialId))
            """)
    long contarAbertasAfetadas(@Param("pontoId") Long pontoId, @Param("materialId") Long materialId,
            @Param("situacoes") Collection<SituacaoSolicitacao> situacoes);

    @Query("""
            select new br.edu.ifs.web1.coletaseletiva.dto.indicador.SolicitacoesPorSituacaoResponse(s.situacao, count(s))
            from SolicitacaoModel s
            where (cast(:inicio as OffsetDateTime) is null or s.criadaEm >= :inicio)
            and (cast(:fim as OffsetDateTime) is null or s.criadaEm < :fim)
            group by s.situacao
            """)
    List<SolicitacoesPorSituacaoResponse> contarPorSituacao(
            @Param("inicio") OffsetDateTime inicio, @Param("fim") OffsetDateTime fim);

    @Query(value = """
            select new br.edu.ifs.web1.coletaseletiva.dto.indicador.RecolhimentoPorMaterialResponse(
                m.id, m.nome, i.unidade, sum(i.quantidadeEfetiva))
            """ + ITENS_RECOLHIDOS_NO_PERIODO + """
            group by m.id, m.nome, i.unidade
            order by lower(m.nome), m.id, i.unidade
            """,
            countQuery = "select count(distinct (m.id, i.unidade)) " + ITENS_RECOLHIDOS_NO_PERIODO)
    Page<RecolhimentoPorMaterialResponse> somarRecolhimentos(
            @Param("inicio") OffsetDateTime inicio, @Param("fim") OffsetDateTime fim, Pageable pageable);
}
