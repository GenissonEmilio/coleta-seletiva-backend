-- O nome continua com a apresentação informada. A unicidade ignora caixa e espaços nas pontas.
-- Não altera nem exclui materiais existentes: duplicatas antigas devem ser revisadas pela equipe.
CREATE UNIQUE INDEX idx_material_nome_normalizado ON material (LOWER(BTRIM(nome)));
