package br.edu.ifs.web1.coletaseletiva.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.edu.ifs.web1.coletaseletiva.model.HistoricoSolicitacaoModel;

public interface HistoricoSolicitacaoRepository extends JpaRepository<HistoricoSolicitacaoModel, Long> {

    /** Ordem cronológica, com o id como desempate. */
    List<HistoricoSolicitacaoModel> findBySolicitacaoIdOrderByOcorridoEmAscIdAsc(Long solicitacaoId);
}
