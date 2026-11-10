# Registro de decisões — Materiais, unidades e inativação (issue #8, API-05)

Base: [requisitos](../../REQUISITOS.md), [DER](../../DER.md), [contrato da API](../../CONTRATO_API.md) e critérios da issue #8. RF03 é o escopo principal; RN01, RN02 e RN03 são atendidas na parte de materiais, com os pontos de integração indicados abaixo.

## 1. Decisões de implementação

### 1.1 Organização e dependências

Seguir a estrutura da equipe: `controller/MaterialController`, `services/material`, `repository/MaterialRepository`, `model/MaterialModel` e Requests/Responses em `dto/material`. Reutilizar erros, exceções e paginação existentes; remover os três marcadores `TesteMaterial*`. Getters e setters explícitos acompanham os Models de morador/endereço. Nenhuma dependência nova.

### 1.2 Nome único, inclusive após inativação

RF03 exige nome único; comparar sem distinguir caixa e sem os espaços nas pontas é uma escolha desta implementação. O Request apara as pontas antes da validação; a consulta verifica duplicidade e o banco garante a regra também em gravações concorrentes. Preservar caixa para exibição; acentos e espaços internos são significativos. Não reutilizar o nome de um material inativo: reativar/editar seu cadastro preserva a identidade e os vínculos.

Nova migração V7: índice único `idx_material_nome_normalizado` sobre `LOWER(BTRIM(nome))`, mantendo o `UNIQUE` anterior e os checksums de V1 a V6. Não há transformação nem exclusão de dados. Duplicatas antigas pela nova comparação fazem a migração falhar para revisão explícita, sem descartar histórico. Para verificá-las antes de atualizar um banco existente:

```sql
SELECT LOWER(BTRIM(nome)) AS nome_comparado, COUNT(*)
FROM material
GROUP BY LOWER(BTRIM(nome))
HAVING COUNT(*) > 1;
```

`saveAndFlush` permite traduzir a violação das duas restrições de nome (`material_nome_key` e o índice novo) em `409 DUPLICIDADE`. Outras falhas de integridade seguem o tratamento global; não são rotuladas como nome repetido.

### 1.3 Catálogo e quantidades

O enum `UnidadeMaterial`, em `model`, representa os valores já definidos na V2: `KG`, `LITRO` e `UNIDADE`. Persistência por nome (`EnumType.STRING`), sem ordinal. JSON usa exatamente esses valores. Peso/volume aceitam fração, peças exigem valor inteiro.

`QuantidadeMaterialService` concentra as regras reutilizáveis da RN02: estimativa/limite informado estritamente positivo, efetivo não negativo, peça integral e limites do `NUMERIC(12,3)` definido no banco. O serviço rejeita perdas de precisão, em vez de arredondar. Zeros à direita não mudam o valor: `1.0000` é aceito como uma peça, mas `1.0001` não. A regra de conclusão com ao menos um efetivo positivo continua na API-13.

### 1.4 Atualização e situação separadas

`MaterialRequest` contém nome, descrição, unidade e preparo. Nome (até 80 caracteres) e unidade são obrigatórios; descrição e preparo continuam opcionais, conforme a nulabilidade do DER. O cadastro começa ativo. `PUT` altera esses quatro dados e mantém a situação.

`PATCH /materiais/{id}/situacao` recebe `MaterialSituacaoRequest` com `Boolean ativo` obrigatório. Ausente/nulo retorna `400`; `false` inativa e `true` reativa. A repetição é idempotente. Resposta `200` com a representação atual, conforme o contrato comum. Não há endpoint de exclusão física.

### 1.5 Histórico e integração com solicitações

Inativar só altera `material.ativo`, sem cascata para vínculos ou itens. Mudar a unidade do cadastro não reescreve `item_solicitacao.unidade`. O teste integrado cria vínculos e item com `KG`, inativa e troca o cadastro para `UNIDADE`, conferindo que a cópia continua em `KG`, com a quantidade anterior intacta.

`MaterialService.buscarAtivoParaSolicitacao` devolve o Model internamente e rejeita inativo com `MATERIAL_INCOMPATIVEL`; inexistente continua `404`. A API-10 deve usá-lo na transação de criação/correção, validar ponto/vínculo/limite e copiar a unidade no item. A API-13 valida os efetivos pela unidade da cópia, mesmo que o cadastro mude. A integração completa e suas disputas de concorrência continuam nas respectivas issues; esta entrega não cria endpoints de solicitação.

### 1.6 Listagem administrativa

`GET /materiais` inclui ativos e inativos por padrão. Filtro opcional `ativo=true/false` aplicado no Repository, antes da paginação. Ordenação por nome sem distinguir caixa, com ID como desempate. Usa `PaginaResponse`, página inicial 0, tamanho padrão 20 e máximo 100. O catálogo público é da API-08.

## 2. Validação

Validação local em 10/11/2026: JDK 25.0.4.1 compilando para Java 21, PostgreSQL 17 em container descartável, sem volumes persistentes e com porta aleatória em `127.0.0.1`. Banco limitado a 512 MiB de RAM/swap e uma CPU; dados em `tmpfs` de até 256 MiB. Credenciais geradas para o teste, sem carregar o `.env` nem usar o banco de desenvolvimento. O container foi encerrado após os testes.

Comando executado em `coleta-seletiva-api/`, com as três variáveis `SPRING_DATASOURCE_*` apontando para esse banco temporário:

```bash
MAVEN_OPTS="-Xmx384m -XX:ActiveProcessorCount=2" \
  /home/genissonemilio/.local/bin/codex-build --memory-mib 1280 -- \
  sh ./mvnw -B -ntp -T 1 test \
  -DargLine="-Xmx384m -XX:ActiveProcessorCount=2" \
  -DforkCount=1 -DreuseForks=true
```

Resultado: **88 testes, zero falhas, erros ou testes ignorados; BUILD SUCCESS**. As migrações V1 a V7 foram aplicadas em banco novo e o Hibernate validou o esquema. A execução foi serial, protegida pelo limite de memória do notebook. O CI da PR também executa a suíte com Java 21 e PostgreSQL 17; a evidência fica nos checks e relatórios da PR.

Dos 88 testes, 37 foram acrescentados na API-05:

| Classe | Casos executados | O que comprova |
| --- | --- | --- |
| `MaterialIntegracaoTest` | 19 | Cadastro/detalhe; campos opcionais; atualização; duplicidade normalizada inclusive inativo; validação de nome/unidade; filtro antes da paginação; parâmetros inválidos; situação obrigatória; idempotência; material ativo para solicitação; 404; índice único no banco; preservação de vínculos, quantidade e unidade histórica; ausência de DELETE |
| `MaterialServiceTest` | 3 | Violação das restrições de nome traduzida em DUPLICIDADE e outras falhas de integridade preservadas |
| `QuantidadeMaterialServiceTest` | 15 | Frações para peso/volume, peça integral, positividade, efetivo zero, campo ausente e fronteiras de precisão do banco |

Os 51 testes anteriores também passaram. Os testes integrados percorrem HTTP, Service, JPA e PostgreSQL real, com rollback de seus dados. A fixture de solicitação é SQL de teste porque a API-10 ainda será implementada; ela comprova preservação dos registros existentes, sem antecipar seus endpoints.

## 3. Revisão

A PR aponta diretamente para `main`, referencia a issue #8 e fica aberta para Genisson revisar e fazer o merge. As decisões de comparação de nome, reativação e pontos de integração ficam explícitas para a equipe.
