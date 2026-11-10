CREATE INDEX idx_ponto_coleta_municipio ON ponto_coleta (municipio);
CREATE INDEX idx_ponto_coleta_bairro ON ponto_coleta (bairro);
CREATE INDEX idx_ponto_coleta_ativo_recolhimento_domiciliar ON ponto_coleta (ativo, recolhimento_domiciliar);
CREATE INDEX idx_ponto_material_material_id ON ponto_material (material_id);
CREATE INDEX idx_solicitacao_ponto_id_situacao_criada_em ON solicitacao (ponto_id, situacao, criada_em);
CREATE INDEX idx_solicitacao_morador_id_criada_em ON solicitacao (morador_id, criada_em);
CREATE INDEX idx_item_solicitacao_material_id ON item_solicitacao (material_id);
CREATE INDEX idx_historico_solicitacao_solicitacao_id_ocorrido_em ON historico_solicitacao (solicitacao_id, ocorrido_em);
CREATE INDEX idx_endereco_morador_id ON endereco (morador_id);
