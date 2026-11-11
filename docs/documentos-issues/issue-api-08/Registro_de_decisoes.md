# Registro de decisões - Consulta pública de pontos e materiais (issue #11, API-08)

Registro das decisões da consulta pública de pontos de coleta e materiais (issue #11, API-08), com justificativa e consequência de cada uma. Modelo de dados: [DER.md](../../DER.md). Contrato: [CONTRATO_API.md](../../CONTRATO_API.md#consulta-pública-api-08). Padrão seguido: [Registro da API-06](../issue-api-06/Registro_de_decisoes.md) (pontos) e [Registro da API-07](../issue-api-07/Registro_de_decisoes.md) (materiais aceitos).

**Base:** `TrabalhoWeb01.pdf` (RF06, RNF03, RNF04 e RNF06), o DER e o contrato da API. Cada decisão indica se veio do PDF ou se foi escolha do autor.

Legenda:
- **PDF**: exigido pelo enunciado.
- **Necessário**: o PDF não cita, mas sem isso uma exigência do PDF não funciona.
- **Escolha**: o PDF deixa em aberto; foi escolhido entre opções.

---

## 0. O que foi feito

| Arquivo | Papel |
| --- | --- |
| `controller/ConsultaPublicaController.java` | Novo. Três `GET` em `/api/v1/publico` |
| `services/publico/ConsultaPublicaService.java` | Novo. Regras da consulta: só ativos, filtros, conversão para DTO público |
| `dto/publico/PontoPublicoResponse.java` | Novo. Ponto sem o campo `ativo` e sem dados pessoais |
| `dto/publico/PontoPublicoDetalheResponse.java` | Novo. Ponto + lista de materiais aceitos |
| `repository/PontoRepository.java` | Novo método `buscarPublico` (consulta com filtros) |
| `repository/PontoMaterialRepository.java` | Novo método `findByPontoIdAndMaterialAtivoTrue` |
| `test/.../controller/ConsultaPublicaIntegracaoTest.java` | Novo. 7 testes com banco real |
| `docs/CONTRATO_API.md` | Nova seção "Consulta pública (API-08)" |

Nenhum método existente foi alterado. Nos dois repositories compartilhados, só foram **acrescentados** métodos.

---

## 1. Decisões tomadas

### 1.1 Camadas separadas: `ConsultaPublicaController` + `ConsultaPublicaService`
- **Tipo:** Escolha.
- **Opções:** (a) controller e service próprios para o público; (b) acrescentar os endpoints no `PontoController`/`PontoService` da API-06.
- **Escolhido:** (a), em `services/publico` e `dto/publico`, seguindo o padrão da equipe `services/<funcionalidade>` e `dto/<funcionalidade>`.
- **Por quê:** a API-06 é a área administrativa (mostra inativos e o campo `ativo`); a pública tem outras regras. Separar evita mexer no código de outro integrante e deixa claro o que é público.
- **Consequência:** a conversão do ponto para DTO aparece duas vezes (admin e público), cada uma com seus campos.

### 1.2 Caminhos do contrato: `/publico/pontos`, `/publico/pontos/{id}` e `/publico/materiais`
- **Tipo:** PDF (RF06) + contrato da equipe.
- **Escolhido:** exatamente os caminhos do mapa de endpoints do `CONTRATO_API.md`, com o filtro de material por `materialId`.
- **Por quê:** o contrato já tinha sido combinado; o front usa a lista de `/publico/materiais` para montar o seletor e manda o `materialId`.
- **Consequência:** filtro por nome do material ("vidro") não existe; o cliente escolhe o material pelo id.

### 1.3 Só pontos ativos; ponto inativo no detalhe responde `404`
- **Tipo:** PDF (RF06: "filtrar pontos ativos") e item 4 da issue.
- **Opções para o detalhe:** (a) `404`, como se não existisse; (b) `200` mostrando que está inativo.
- **Escolhido:** (a).
- **Por quê:** a consulta pública só oferece pontos disponíveis. Mesmo comportamento do endereço de outro morador (API-04): o que o usuário não deve usar responde `404`.
- **Consequência:** o DTO público não tem o campo `ativo` (sempre seria `true`). Inativos continuam visíveis em `GET /pontos` (admin).

### 1.4 Filtro de material com `EXISTS`, não com `JOIN`
- **Tipo:** Necessário (critério: "paginação não duplica/omite resultados por join").
- **Problema:** com `JOIN ponto_material`, um ponto que aceita 2 materiais vira 2 linhas. A página de 20 traria pontos repetidos e o `totalElements` ficaria errado.
- **Opções:** (a) `JOIN` + `DISTINCT`; (b) subconsulta `EXISTS`.
- **Escolhido:** (b). Cada ponto é uma linha; o `EXISTS` só responde "aceita ou não".
- **Por quê:** `DISTINCT` junto com `ORDER BY lower(nome)` e paginação dá problemas no Postgres e no Hibernate; o `EXISTS` é mais simples e usa o índice `idx_ponto_material_material_id` (V6).
- **Consequência:** teste `paginacaoNaoDuplicaNemOmitePontosComVariosMateriais`: 3 pontos com 2 materiais cada, páginas de tamanho 1, total 3, sem repetir nenhum.

### 1.5 Material inativo não conta no filtro nem no detalhe
- **Tipo:** Escolha (coerente com RF03 e com a decisão 1.4 da API-07).
- **Escolhido:** o filtro `materialId` só encontra pontos se o material estiver ativo; o detalhe só lista materiais ativos; `/publico/materiais` só lista ativos (reusa `MaterialService.listar(true, ...)` da API-05).
- **Por quê:** material inativo não pode ser solicitado, então não deve ser oferecido ao público.
- **Consequência:** o vínculo continua no banco e volta a aparecer se o material for reativado.

### 1.6 Como cada filtro compara
- **Tipo:** Escolha.
- **Escolhido:** todos sem diferenciar maiúsculas e sem espaços nas pontas. `nome` **contém** o texto ("eco" acha "Ecoponto Centro"); `municipio` e `bairro` são **iguais** ao texto.
- **Por quê:** quem procura um ponto lembra parte do nome; município e bairro são valores fixos que o front pode oferecer em lista, e o igual evita que "Centro" ache "Centro Histórico".
- **Consequência:** acentos contam ("Sao" não acha "São"). Os caracteres `%` e `_` digitados no nome funcionam como coringa do `LIKE`; não é risco de segurança (o parâmetro é ligado, sem SQL injection), só afeta o resultado.
- **Detalhe técnico:** filtro ausente vira `""` no Service, e a consulta ignora `""`. Isso evita o erro do Postgres "could not determine data type of parameter" com `lower(:param)` quando o parâmetro é `null`.

### 1.7 Ordenação estável e limite de 100 reusando o `PaginacaoService`
- **Tipo:** PDF (RNF04) + contrato (API-03).
- **Escolhido:** ordem por nome do ponto sem diferenciar maiúsculas, com `id` como desempate (o `PaginacaoService` acrescenta sozinho); `size` de 1 a 100.
- **Consequência:** `size=100` funciona e `size=101` responde `400` apontando `size` (testado).

### 1.8 Recolhimento domiciliar desabilitado: identificado, não bloqueado na consulta
- **Tipo:** PDF (critério da issue).
- **Escolhido:** o ponto aparece normalmente com `recolhimentoDomiciliar: false`. O front usa esse campo para não oferecer "solicitar coleta em casa".
- **Por quê:** o ponto ainda recebe materiais levados pelo morador, então deve aparecer na busca. Quem impede a solicitação é o backend, no `PontoService.buscarDisponivelParaSolicitacao` (API-06), que a API-10 vai chamar.
- **Consequência:** o filtro `domiciliar=true` (proposto no PLAN-03) **não** foi feito: está marcado como "a confirmar com a equipe" no contrato.

### 1.9 DTO público sem dados pessoais
- **Tipo:** PDF (RNF06).
- **Escolhido:** `PontoPublicoResponse` só tem dados do ponto (endereço do ponto, contato público, horários). Nunca serializa entidade JPA. Os materiais usam o `PontoMaterialResponse` da API-07, que também não tem dado pessoal.
- **Consequência:** teste `respostasPublicasNaoTrazemDadosDeMoradores`: cadastra um morador e confere que o nome e o e-mail dele não aparecem na lista nem no detalhe.

### 1.10 Detalhe com lista simples de materiais e conversor próprio
- **Tipo:** Escolha.
- **Opções:** (a) lista completa, sem paginação, dentro do detalhe; (b) mandar o front chamar `GET /pontos/{id}/materiais`.
- **Escolhido:** (a), ordenada por nome.
- **Por quê:** um ponto aceita poucos materiais e a issue pede o detalhe com "materiais aceitos, unidades e orientações". A rota admin mostra materiais inativos.
- **Consequência:** a conversão do vínculo e o ajuste `semZerosFinais` (`20.000` → `20`) foram repetidos no `ConsultaPublicaService`, porque os do `PontoMaterialService` são `private` e esse arquivo é de outro integrante. Se a equipe preferir, dá para expor um método lá e remover a repetição.

---

## 2. Validação

**Ambiente:** PostgreSQL 17 descartável (container `postgres:17`, sem volume, só em `127.0.0.1`, porta 55432, senha descartável), Java 21. O container foi parado ao fim e a senha temporária apagada.

**Comandos:**
1. Banco: `docker run --rm -d --name coleta-tmp-pg-11 --tmpfs /var/lib/postgresql/data -e POSTGRES_PASSWORD=<senha descartável> -e POSTGRES_DB=coleta_teste -p 127.0.0.1:55432:5432 postgres:17`.
2. Testes: `./mvnw -B -ntp test`, com `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD` apontando para o banco.
3. Limpeza: `docker stop coleta-tmp-pg-11`.

**Resultado (2026-11-11):** **121 testes, 0 falhas, 0 erros** e `BUILD SUCCESS`. Desses, 7 são desta issue, em `ConsultaPublicaIntegracaoTest` (banco real, dados desfeitos ao fim de cada teste). Antes eram 114.

**Critérios de aceite da issue:**

| Critério | Teste que prova |
| --- | --- |
| Filtro por vidro retorna somente pontos ativos que o aceitam | `filtroPorVidroRetornaSomentePontosAtivosQueOAceitam` |
| Combinação de filtros respeitada | `filtrosCombinadosSaoRespeitadosSemDiferenciarMaiusculas` (município + bairro + material + nome) |
| Paginação não duplica/omite por join; `size=100` | `paginacaoNaoDuplicaNemOmitePontosComVariosMateriais` |
| Ponto com domiciliar desabilitado é identificado | `detalheMostraMateriaisAtivosEIdentificaPontoSemDomiciliar` e decisão 1.8 |
| Ponto inativo | `filtroPorVidro...` (fora da lista) e `pontoInativoNaoApareceNoDetalhePublico` (`404`) |
| DTO público sem informações pessoais | `respostasPublicasNaoTrazemDadosDeMoradores` |
| Materiais públicos só ativos | `listaPublicaDeMateriaisTrazSoOsAtivos` |

---

## 3. Pendentes

- Filtro `domiciliar=true` em `GET /publico/pontos`: proposto no PLAN-03, ainda a confirmar com a equipe (decisão 1.8).
- Evidência de desempenho (p95 ≤ 2 s, RNF04): fica com a #21 (API-18).
- Busca sem acento ("Sao" achar "São"): não pedida; exigiria `unaccent` no Postgres (decisão 1.6).
- Revisão do Genisson (revisor da issue) sobre as decisões 1.3 (`404` para inativo), 1.6 (igual x contém) e 1.10 (repetição do conversor).
