package br.edu.ifs.web1.coletaseletiva.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.edu.ifs.web1.coletaseletiva.model.SituacaoSolicitacao;
import br.edu.ifs.web1.coletaseletiva.model.SolicitacaoModel;

public interface SolicitacaoRepository extends JpaRepository<SolicitacaoModel, Long> {

    boolean existsByProtocolo(String protocolo);

    /**
     * Busca a solicitação do morador travando a linha (SELECT ... FOR UPDATE) até o fim da transação.
     * Duas alterações simultâneas entram em fila: a segunda lê a versão já atualizada e recebe 409.
     * Solicitação de outro morador não é encontrada (404), como o endereço de outro morador.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SolicitacaoModel s where s.id = :id and s.morador.id = :moradorId")
    Optional<SolicitacaoModel> travarParaAlteracao(@Param("id") Long id, @Param("moradorId") Long moradorId);

    /** Usado só dentro da transação que já travou a linha com travarParaAlteracao. */
    @Modifying
    @Query("update SolicitacaoModel s set s.versao = s.versao + 1 where s.id = :id")
    void incrementarVersao(@Param("id") Long id);

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
}
