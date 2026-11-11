package br.edu.ifs.web1.coletaseletiva.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
