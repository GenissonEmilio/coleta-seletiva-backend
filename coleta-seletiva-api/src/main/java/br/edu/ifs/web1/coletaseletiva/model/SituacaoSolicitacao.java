package br.edu.ifs.web1.coletaseletiva.model;

/** Situações da solicitação (tabela de transições do PDF). CONCLUIDA fica sem acento no banco e no JSON. */
public enum SituacaoSolicitacao {
    SOLICITADA,
    AGENDADA,
    RECUSADA,
    CANCELADA,
    EM_RECOLHIMENTO,
    CONCLUIDA
}
