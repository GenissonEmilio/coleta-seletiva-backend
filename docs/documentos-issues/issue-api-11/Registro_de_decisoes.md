# Registro de decisões - Consultar solicitações, itens e histórico por contexto (issue #14, API-11)

Registro das decisões das consultas de solicitação (issue #14, API-11), com justificativa e consequência de cada uma. Modelo de dados: [DER.md](../../DER.md). Contrato: [CONTRATO_API.md](../../CONTRATO_API.md#consulta-de-solicitações-api-11). Padrão seguido: [Registro da API-10](../issue-api-10/Registro_de_decisoes.md) (solicitação) e [Registro da API-09](../issue-api-09/Registro_de_decisoes.md) (contexto do ator).

**Base:** `TrabalhoWeb01.pdf` (RF09, RN03, RN04, RNF03, RNF04 e RNF06), o DER e o contrato da API. Cada decisão indica se veio do PDF ou se foi escolha do autor.

Legenda:
- **PDF**: exigido pelo enunciado.
- **Necessário**: o PDF não cita, mas sem isso uma exigência do PDF não funciona.
- **Escolha**: o PDF deixa em aberto; foi escolhido entre opções.

---

## 0. O que foi feito

| Arquivo | Papel |
| --- | --- |
| `repository/SolicitacaoRepository.java` | Método acrescentado: `consultar` (lista com contexto e filtros) |
| `dto/solicitacao/SolicitacaoResumoResponse.java` | Novo. Linha da lista |
| `dto/solicitacao/SolicitacaoResponse.java` | Campos acrescentados: `agendadaPara`, `concluidaEm` e `agendamentoNovo` (no histórico) |
| `services/solicitacao/SolicitacaoService.java` | Métodos acrescentados: `listar`, `buscar`, `listarHistorico` e a regra de contexto |
| `controller/SolicitacaoController.java` | Três rotas `GET` acrescentadas |
| `test/.../SolicitacaoConsultaIntegracaoTest.java` | Novo. 9 testes com banco real |
| `docs/CONTRATO_API.md` | Nova seção "Consulta de solicitações (API-11)" |

Só arquivos da própria solicitação (API-10) foram alterados; nenhum arquivo de outro integrante foi tocado. O `GET /solicitacoes/{id}` passa a responder ao `Location` devolvido na criação.

---

## 1. Decisões tomadas

### 1.1 O contexto é aplicado pelo backend, dentro da consulta
- **Tipo:** PDF (RF09: "a consulta retorna somente registros que correspondam aos filtros e ao contexto do ator").
- **Escolhido:** o controller resolve o ator pelos cabeçalhos (API-09) e o `SolicitacaoService` monta a restrição: morador filtra por `morador_id`; operador filtra pelos pontos carregados do banco (`ContextoAtor.pontoIds`); administrador não tem restrição.
- **Por quê:** a restrição entra na própria consulta SQL, junto com os filtros. Um filtro enviado pelo cliente só consegue reduzir o resultado, nunca ampliar.
- **Consequência:** operador filtrando por `pontoId` de outro ponto recebe lista vazia; operador sem nenhum ponto vinculado recebe lista vazia. Continua sendo simulação, não autenticação (decisão 1.5 da API-09).

### 1.2 Fora do contexto responde `404`, como se não existisse
- **Tipo:** Escolha, decidida pelo responsável da issue (o critério pede "contrato define erro consistente").
- **Opções:** (a) `404 REGISTRO_NAO_ENCONTRADO`; (b) `403 ACESSO_NEGADO`.
- **Escolhido:** (a), no detalhe e no histórico.
- **Por quê:** é a mesma resposta da correção de solicitação de outro morador (decisão 1.11 da API-10) e do endereço de outro morador (API-04). A resposta é idêntica à de um id inexistente, então não confirma que a solicitação existe nem vaza dado algum.
- **Consequência:** `403` fica só para ator ausente, desconhecido ou inativo. As regras `exigirOperadorDoPonto` e `exigirMoradorDono` da API-09 (que respondem `403`) continuam reservadas para as **ações** das issues #15 e #16.

### 1.3 Período: criação, início inclusivo, fim exclusivo, offset obrigatório
- **Tipo:** PDF (RF09: "filtrar por período"; RNF05: datas com fuso ou offset) + convenção do contrato (API-03).
- **Escolhido:** `inicio` e `fim` em ISO 8601 com offset, comparados com `criada_em` (`>= inicio` e `< fim`). Cada um é opcional. Sem offset, ou `inicio` igual ou posterior a `fim`: `400` apontando o campo.
- **Por quê fim exclusivo:** períodos seguidos não se sobrepõem. "Janeiro" é de `2026-01-01T00:00:00` até `2026-02-01T00:00:00`, sem precisar de `23:59:59.999`.
- **Por quê offset obrigatório:** sem ele, o servidor teria de adivinhar o fuso. Com offset, `09:00-03:00` e `12:00Z` são o mesmo instante (testado).
- **Consequência:** o front monta o período no fuso do usuário e envia com offset. Na URL, `+` deve ir como `%2B`.
- **Detalhe técnico:** período ausente vira limites fixos que não restringem (1970 e 3000), e situação ausente vira a lista das seis situações. Isso evita parâmetro nulo na consulta, que o PostgreSQL recusa por não saber o tipo.

### 1.4 Filtros de situação e ponto
- **Tipo:** PDF (RF09).
- **Escolhido:** `situacao` (um dos seis valores do enum) e `pontoId`, opcionais e combináveis entre si e com o período. Valor de situação desconhecido: `400` apontando `situacao`.
- **Consequência:** um valor por filtro; filtrar por várias situações ao mesmo tempo não foi pedido.

### 1.5 Ordem da mais recente para a mais antiga; máximo de 100
- **Tipo:** PDF (RNF04) + convenção do contrato.
- **Escolhido:** `criada_em` decrescente, com o `id` como desempate (acrescentado pelo `PaginacaoService`); `size` de 1 a 100.
- **Consequência:** `size=101` responde `400` (testado).

### 1.6 Lista resumida; detalhe completo
- **Tipo:** Escolha.
- **Opções:** (a) lista com um resumo e detalhe com tudo; (b) lista já com itens e histórico.
- **Escolhido:** (a). A lista (`SolicitacaoResumoResponse`) traz `id`, `protocolo`, `situacao`, `criadaEm`, `agendadaPara`, `concluidaEm`, `moradorId`, `pontoId` e `nomePonto`. O detalhe reusa o `SolicitacaoResponse` da API-10.
- **Por quê:** com (b), cada linha da página buscaria seus itens e seu histórico no banco (até 200 consultas extras numa página de 100), o que pesa na meta de desempenho (RNF04). O nome do ponto vem na mesma consulta (`@EntityGraph`).
- **Consequência:** o front abre o detalhe para ver itens e histórico. Acrescentei `agendadaPara` e `concluidaEm` ao `SolicitacaoResponse` (a issue pede "agendamento e datas") e `agendamentoNovo` à linha do histórico.

### 1.7 Detalhe mostra as cópias, não o cadastro
- **Tipo:** PDF (RN04) e item 4 da issue.
- **Escolhido:** endereço e unidade vêm dos campos copiados na solicitação e no item.
- **Consequência:** depois de alterar o endereço cadastrado e a unidade do material, o detalhe continua mostrando os valores da época (teste `detalheContinuaMostrandoACopiaDepoisDeMudarOCadastro`). O nome do material vem do cadastro atual, porque só a unidade é copiada (RN04).

### 1.8 Histórico em ordem cronológica, sem paginação
- **Tipo:** PDF (RN03) e item 4 da issue.
- **Escolhido:** `GET /solicitacoes/{id}/historico` devolve a lista ordenada por `ocorrido_em` e depois por `id`. O mesmo histórico já vem dentro do detalhe.
- **Por quê sem paginação:** o histórico de uma solicitação é curto (uma linha por alteração). Paginar obrigaria o front a juntar páginas para mostrar uma linha do tempo.
- **Consequência:** foge da convenção "toda listagem usa `PaginaResponse`"; fica marcado para a revisão.

### 1.9 Dados pessoais do morador ficam de fora
- **Tipo:** Escolha, decidida pelo responsável da issue (RNF06).
- **Opções:** (a) só `moradorId` e o endereço de atendimento; (b) incluir nome e telefone do morador.
- **Escolhido:** (a).
- **Por quê:** o RF09 não pede nome nem contato na consulta, e a RNF06 pede cuidado com dados pessoais. O endereço de atendimento é necessário para o recolhimento.
- **Consequência:** o operador vê onde buscar, mas não o nome nem o telefone. Fica pendente a equipe decidir quando fizer a tela do operador.

---

## 2. Validação

**Ambiente:** PostgreSQL 17 descartável (container `postgres:17`, sem volume, só em `127.0.0.1`, porta 55432, senha descartável), Java 21. Container parado, API encerrada e arquivos temporários apagados ao fim.

**Comandos:**
1. Banco: `docker run --rm -d --name coleta-tmp-pg-14 --tmpfs /var/lib/postgresql/data -e POSTGRES_PASSWORD=<senha descartável> -e POSTGRES_DB=coleta_teste -p 127.0.0.1:55432:5432 postgres:17`.
2. Testes: `./mvnw -B -ntp test`, com `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD` apontando para o banco.
3. API real: `./mvnw -B -ntp -q spring-boot:run -Dspring-boot.run.profiles=demo -Dspring-boot.run.arguments=--server.port=18080` e chamadas `curl`.
4. Limpeza: encerrar a API e `docker stop coleta-tmp-pg-14`.

**Resultado dos testes (2026-11-11):** **160 testes, 0 falhas, 0 erros** e `BUILD SUCCESS`. Desses, 9 são desta issue, em `SolicitacaoConsultaIntegracaoTest` (banco real, dados desfeitos ao fim de cada teste). Antes eram 151.

**Cenário dos testes:** Maria pediu no ponto A (10/01) e no ponto B (10/02); João pediu no ponto A (20/01, agendada). Ana opera o ponto A e Bruno o ponto B.

**Chamadas reais** (perfil `demo`; morador 52; Ana = operador 27 do ponto 71; Bruno = operador 28 do ponto 72; solicitação 39 no ponto A e 40 no ponto B):

| # | Chamada | Resposta |
| --- | --- | --- |
| 1 | `GET /solicitacoes` como o morador | `200`, 2 solicitações, a 40 (mais recente) antes da 39 |
| 2 | `GET /solicitacoes` como Ana | `200`, só a 39 (ponto A) |
| 3 | `GET /solicitacoes?pontoId=72` como Ana | `200`, lista vazia |
| 4 | Administrador com `inicio=2026-11-11T00:00:00-03:00`, `fim=2026-11-12T00:00:00-03:00`, `situacao=SOLICITADA`, `pontoId=72` | `200`, só a 40 |
| 5 | `inicio=2026-11-11T00:00:00` (sem offset) | `400`, campo `inicio`, "valor em formato inválido" |
| 6 | `GET /solicitacoes/39` como Bruno | `404 REGISTRO_NAO_ENCONTRADO` |
| 7 | `GET /solicitacoes/39/historico` como Ana | `200`, 1 linha: criação, `MORADOR` 52 |
| 8 | `GET /solicitacoes/39` como Ana | `200`, com endereço copiado, itens e histórico |
| 9 | `GET /solicitacoes/39` sem cabeçalhos | `403 ACESSO_NEGADO` |

**Critérios de aceite da issue:**

| Critério | Teste que prova |
| --- | --- |
| Filtro combinado retorna somente os registros correspondentes | `filtrosDePeriodoSituacaoEPontoSeCombinam` (período + situação + ponto; limites inclusivo e exclusivo; offset) |
| Respeita page/size máximo 100 | `administradorListaTodasDaMaisRecenteParaAMaisAntigaComPaginacao` |
| Morador consulta apenas seus atendimentos | `moradorListaApenasAsProprias` |
| Operador consulta apenas os de seus pontos | `operadorListaApenasAsDosSeusPontos` |
| Detalhe/histórico fora do contexto não vaza dados; erro consistente | `detalheEHistoricoForaDoContextoRespondem404SemVazarDados` |
| Datas, autor, motivo e eventos aparecem em ordem | `historicoVemEmOrdemComDataResponsavelEMotivo` |
| Detalhe com itens, unidades, agendamento e datas | `detalheMostraItensEnderecoCopiadoAgendamentoEHistoricoNoContexto` |
| Endereço e unidades copiados são respeitados | `detalheContinuaMostrandoACopiaDepoisDeMudarOCadastro` |
| Filtro inválido | `filtroInvalidoResponde400ApontandoOCampo` |

---

## 3. Pendentes

- Os testes colocam a situação `AGENDADA` e a data do agendamento direto no banco, porque o agendamento é da #15 (API-12). Trocar pela ação real quando existir.
- Decidir com a equipe se o operador deve ver nome e telefone do morador (decisão 1.9).
- Evidência de desempenho (p95 ≤ 2 s com 1.000 solicitações, RNF04): fica com a #21 (API-18). A lista usa duas consultas por página (os dados e a contagem do total) e conta com os índices `idx_solicitacao_ponto_id_situacao_criada_em` e `idx_solicitacao_morador_id_criada_em` da V6.
- Revisão do Genisson sobre as decisões 1.2 (`404` fora do contexto) e 1.8 (histórico sem paginação).
