CREATE INDEX idx_solicitacao_criada_em_situacao ON solicitacao (criada_em, situacao);
CREATE INDEX idx_solicitacao_situacao_concluida_em ON solicitacao (situacao, concluida_em);
