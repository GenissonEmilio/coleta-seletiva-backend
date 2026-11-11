-- Carga fictícia para o roteiro de desempenho, após a migração e o perfil demo.
BEGIN;

DO $$
BEGIN
    IF current_database() <> 'coleta_desempenho' OR EXISTS (SELECT 1 FROM solicitacao) THEN
        RAISE EXCEPTION 'Use o banco descartável e vazio criado por desempenho_api.py.';
    END IF;
END
$$;

INSERT INTO morador (nome, email)
SELECT 'Morador fictício ' || n, 'morador' || n || '@example.org'
FROM generate_series(1, 20) AS serie(n);

INSERT INTO endereco (morador_id, logradouro, numero, bairro, municipio, uf, cep)
SELECT id, 'Rua Fictícia do Ensaio', '10', 'Centro', 'Cidade Demonstração', 'SE', '49000000'
FROM morador WHERE email LIKE 'morador%@example.org';

INSERT INTO material (nome, unidade)
VALUES ('Papel', 'KG'), ('Plástico', 'UNIDADE'), ('Óleo', 'LITRO');

INSERT INTO ponto_material (ponto_id, material_id)
SELECT ponto_coleta.id, material.id FROM ponto_coleta CROSS JOIN material
WHERE ponto_coleta.nome IN ('Ecoponto Demo A', 'Ecoponto Demo B');

WITH enderecos AS (
    SELECT endereco.*, row_number() OVER (ORDER BY id) - 1 AS posicao FROM endereco
), pontos AS (
    SELECT id, row_number() OVER (ORDER BY id) - 1 AS posicao FROM ponto_coleta
), carga AS (
    SELECT n, (ARRAY['SOLICITADA', 'AGENDADA', 'RECUSADA', 'CANCELADA',
                     'EM_RECOLHIMENTO', 'CONCLUIDA'])[(n - 1) % 6 + 1] AS situacao,
           TIMESTAMPTZ '2026-09-01 00:00:00Z' + n * INTERVAL '1 hour' AS criada_em
    FROM generate_series(1, 1000) AS serie(n)
)
INSERT INTO solicitacao (protocolo, morador_id, ponto_id, endereco_id, end_logradouro,
                         end_numero, end_sem_numero, end_bairro, end_municipio, end_uf,
                         end_cep, situacao, criada_em, agendada_para, concluida_em, versao)
SELECT 'DEMO-' || lpad(n::text, 4, '0'), e.morador_id, p.id, e.id, e.logradouro,
       e.numero, e.sem_numero, e.bairro, e.municipio, e.uf, e.cep, c.situacao, c.criada_em,
       CASE WHEN c.situacao = 'AGENDADA' THEN CURRENT_DATE + INTERVAL '2 days'
            WHEN c.situacao IN ('EM_RECOLHIMENTO', 'CONCLUIDA') THEN c.criada_em + INTERVAL '2 hours' END,
       CASE WHEN c.situacao = 'CONCLUIDA' THEN c.criada_em + INTERVAL '3 hours' END,
       CASE c.situacao WHEN 'SOLICITADA' THEN 0 WHEN 'EM_RECOLHIMENTO' THEN 2
                       WHEN 'CONCLUIDA' THEN 3 ELSE 1 END
FROM carga c
JOIN enderecos e ON e.posicao = (c.n - 1) % 20
JOIN pontos p ON p.posicao = (c.n / 5) % 2
ORDER BY c.n;

INSERT INTO item_solicitacao (solicitacao_id, material_id, unidade, quantidade_estimada, quantidade_efetiva)
SELECT s.id, m.id, m.unidade,
       CASE m.unidade WHEN 'KG' THEN 4 WHEN 'UNIDADE' THEN 3 ELSE 2 END,
       CASE WHEN s.situacao = 'CONCLUIDA' THEN
           CASE m.unidade WHEN 'KG' THEN 2.5 WHEN 'UNIDADE' THEN 2 ELSE 1.25 END
       END
FROM solicitacao s
JOIN material m ON m.nome = 'Papel' OR m.nome =
    CASE WHEN (substring(s.protocolo FROM 6)::int / 6) % 2 = 0 THEN 'Plástico' ELSE 'Óleo' END;

INSERT INTO historico_solicitacao (solicitacao_id, situacao_anterior, situacao_nova, ocorrido_em,
                                   responsavel_tipo, responsavel_id, motivo, agendamento_novo)
SELECT s.id, evento.anterior, evento.nova, s.criada_em + evento.horas * INTERVAL '1 hour',
       CASE WHEN evento.morador THEN 'MORADOR' ELSE 'OPERADOR' END,
       CASE WHEN evento.morador THEN s.morador_id ELSE op.operador_id END,
       evento.motivo, CASE WHEN evento.nova = 'AGENDADA' THEN s.agendada_para END
FROM solicitacao s
JOIN operador_ponto op ON op.ponto_id = s.ponto_id
CROSS JOIN LATERAL (VALUES
    (NULL, 'SOLICITADA', 0, TRUE, NULL, TRUE),
    ('SOLICITADA', 'AGENDADA', 1, FALSE, NULL,
        s.situacao IN ('AGENDADA', 'EM_RECOLHIMENTO', 'CONCLUIDA')),
    ('SOLICITADA', 'RECUSADA', 1, FALSE, 'Disponibilidade fictícia', s.situacao = 'RECUSADA'),
    ('SOLICITADA', 'CANCELADA', 1, TRUE, 'Cancelamento fictício', s.situacao = 'CANCELADA'),
    ('AGENDADA', 'EM_RECOLHIMENTO', 2, FALSE, NULL, s.situacao IN ('EM_RECOLHIMENTO', 'CONCLUIDA')),
    ('EM_RECOLHIMENTO', 'CONCLUIDA', 3, FALSE, NULL, s.situacao = 'CONCLUIDA')
) AS evento(anterior, nova, horas, morador, motivo, aplicavel)
WHERE evento.aplicavel;

COMMIT;
ANALYZE;
