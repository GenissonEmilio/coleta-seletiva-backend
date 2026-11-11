# Registro de decisões - Materiais aceitos e limites por ponto (issue #10, API-07)

Registro das decisões da implementação dos vínculos entre pontos de coleta e materiais (issue #10, API-07), com justificativa e consequência de cada uma. Modelo de dados: [DER.md](../../DER.md). Contrato: [CONTRATO_API.md](../../CONTRATO_API.md). Padrão seguido: [Registro da API-06](../issue-api-06/Registro_de_decisoes.md) (pontos) e [Registro da API-04](../issue-api-04/Registro_de_decisoes.md) (endereços, recurso aninhado).

**Base:** `TrabalhoWeb01.pdf` (RF05, RN01, RN02 e RN05), o DER e o contrato da API. Cada decisão indica se veio do PDF ou se foi escolha do autor.

Legenda:
- **PDF**: exigido pelo enunciado.
- **Necessário**: o PDF não cita, mas sem isso uma exigência do PDF não funciona.
- **Escolha**: o PDF deixa em aberto; foi escolhido entre opções.

---

## 1. Decisões tomadas

### 1.1 Chave composta `PontoMaterialId` com `@ManyToOne` para ponto e material
- **Tipo:** Necessário.
- **Situação encontrada:** a tabela `ponto_material` (V3) não tem `id` próprio; a chave primária é `(ponto_id, material_id)`.
- **Opções:** (a) `@EmbeddedId PontoMaterialId` com `@MapsId` nas relações; (b) `@IdClass`; (c) nova migração criando um `id` sintético.
- **Escolhido:** (a). As relações seguem o `@ManyToOne(fetch = LAZY)` do `EnderecoModel`.
- **Por quê:** espelha o banco sem migração nova. A própria chave primária garante o vínculo único, que é o critério da issue.
- **Consequência:** `PontoMaterialId` é uma classe nova em `model/`, usada só por este vínculo.

### 1.2 `PUT` idempotente: `201` ao criar, `200` ao atualizar
- **Tipo:** Escolha (a issue pede "revisar semântica PUT idempotente versus criação explícita").
- **O que o contrato diz:** `PUT /pontos/{id}/materiais/{materialId}`, sem `POST`.
- **Opções:** (a) `PUT` cria ou atualiza; (b) `POST` para criar e `409 DUPLICIDADE` ao repetir, `PUT` só para atualizar.
- **Escolhido:** (a).
- **Por quê:** o caminho já identifica o vínculo (ponto + material), então repetir o mesmo pedido leva ao mesmo estado. Segue o contrato sem endpoint novo.
- **Consequência:** repetir o vínculo nunca cria uma segunda linha nem dá erro: atualiza observações e limite e responde `200` (teste `repetirVinculoAtualizaSemCriarSegundaLinha`). O `PUT` substitui os dois campos: omitir o limite o remove.

### 1.3 Limite opcional validado pelo `QuantidadeMaterialService`
- **Tipo:** PDF (RF05 e RN02).
- **Escolhido:** limite ausente não é validado (sem limite). Quando informado, `validarPositiva` na unidade do material: maior que zero, inteiro para `UNIDADE`, no máximo 9 dígitos inteiros e 3 casas decimais.
- **Por quê:** o contrato manda a API-07 reusar esse serviço e pular a validação quando o limite estiver ausente.
- **Consequência:** `0`, `-1` e `2.5` para peça respondem `400` apontando `limitePorSolicitacao`, sem gravar nada.

### 1.4 Material inativo não pode ganhar vínculo novo
- **Tipo:** Escolha.
- **O que o PDF diz:** RF03: material inativo não pode integrar uma nova solicitação. Não fala do vínculo.
- **Opções:** (a) rejeitar vínculo novo com `409 MATERIAL_INCOMPATIVEL`; (b) aceitar e deixar a API-10 barrar na solicitação.
- **Escolhido:** (a). Vínculos que já existem continuam e podem ser atualizados.
- **Por quê:** evita oferecer no ponto um material que não pode ser pedido. Usa um código de erro que já existe.
- **Consequência:** a API-10 continua precisando checar o material ativo, porque ele pode ser inativado depois do vínculo.

### 1.5 Consulta paginada, ordenada pelo nome do material
- **Tipo:** Necessário (convenção do contrato: toda listagem usa `PaginaResponse`).
- **Escolhido:** `GET /pontos/{id}/materiais?page=&size=`, ordem por nome do material sem diferenciar maiúsculas. A resposta traz `nomeMaterial`, `unidade`, `orientacaoPreparo`, `observacoes` e `limitePorSolicitacao` (sem zeros finais: o banco guarda `20.000`, a resposta mostra `20`).
- **Por quê:** são os dados que o formulário de solicitação precisa (critério da issue).
- **Consequência:** ponto inexistente responde `404`.

### 1.6 Remoção física do vínculo; proteção da RN05 fica com a API-14
- **Tipo:** Escolha.
- **O que a issue diz:** "integrar remoção protegida com API-14"; "remoção não contorna RN05".
- **Escolhido:** `DELETE` apaga a linha e responde `204`; vínculo inexistente (inclusive de ponto inexistente) responde `404`. O bloqueio quando há solicitações abertas afetadas não foi feito aqui.
- **Por quê:** a #17 (API-14) é dona desse bloqueio e precisa definir a estratégia de locks contra a corrida com a criação de solicitação (item 5 da #17). Uma checagem prévia aqui seria a solução incompleta que a #17 proíbe.
- **Consequência:** até a API-14, o `DELETE` remove mesmo com solicitações abertas. O código `PONTO_COM_SOLICITACOES_ABERTAS` já existe para ela usar.

---

## 2. Validação

**Ambiente:** PostgreSQL 17 descartável (container `postgres:17`, sem volume, só em `127.0.0.1`, porta 55432, senha descartável), Spring Boot 4.1.2, Java 21. O container foi parado ao fim.

**Comandos:**
1. Banco: `docker run --rm -d --name coleta-tmp-pg-10 --tmpfs /var/lib/postgresql/data -e POSTGRES_PASSWORD=<senha descartável> -e POSTGRES_DB=coleta_teste -p 127.0.0.1:55432:5432 postgres:17`.
2. Testes: `./mvnw -B -ntp test`, com `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD` apontando para o banco.
3. Limpeza: `docker stop coleta-tmp-pg-10`.

**Resultado:** **114 testes, 0 falhas, 0 erros** e `BUILD SUCCESS`. Desses, 9 são desta issue, em `PontoMaterialIntegracaoTest` (banco real, dados desfeitos ao fim de cada teste).

**Critérios de aceite da issue:**

| Critério | Teste que prova |
| --- | --- |
| Repetir vínculo nunca cria segunda linha; política documentada | `repetirVinculoAtualizaSemCriarSegundaLinha` e decisão 1.2 |
| Unicidade no banco | `bancoRejeitaVinculoRepetidoSemPassarPeloService` |
| Limite zero, negativo e fracionário para peça são rejeitados | `limiteZeroNegativoOuFracionarioParaPecaERejeitado` (3 casos) |
| Limite vazio é permitido | `repetirVinculoAtualizaSemCriarSegundaLinha` e `consultaTrazUnidade...` |
| Consulta traz unidade, orientação, observações e limite | `consultaTrazUnidadeOrientacaoObservacoesELimiteOrdenadosPorNome` |
| Material inativo não ganha vínculo | `materialInativoNaoPodeSerVinculado` |
| Remoção e `404` | `removerApagaOVinculoERepetirResponde404`, `pontoOuMaterialInexistenteResponde404` |
| Remoção não contorna RN05 | Depende da #17 (API-14), decisão 1.6 |

---

## 3. Pendentes

- Proteção da remoção com solicitações abertas (RN05) e política de locks: #17 (API-14).
- Respeitar o limite na solicitação e rejeitar material não aceito pelo ponto: #13 (API-10).
- Se a unidade de um material mudar depois do vínculo (por exemplo `KG` para `UNIDADE`), um limite fracionário já gravado fica incoerente. O `MaterialService` hoje permite essa troca; decidir com a equipe se a troca deve ser bloqueada quando houver vínculos.
- Revisão do Herbert sobre as decisões 1.2 (`PUT` idempotente) e 1.4 (material inativo).
