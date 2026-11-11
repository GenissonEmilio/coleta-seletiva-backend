# Registro de decisões - Operadores, vínculos e contexto de demonstração (issue #12, API-09)

Registro das decisões do cadastro de operadores, dos vínculos com pontos e do contexto de demonstração (issue #12, API-09), com justificativa e consequência de cada uma. Modelo de dados: [DER.md](../../DER.md). Contrato: [CONTRATO_API.md](../../CONTRATO_API.md#operadores-e-contexto-de-demonstração-api-09). Padrão seguido: [Registro da API-04](../issue-api-04/Registro_de_decisoes.md) (moradores) e [Registro da API-07](../issue-api-07/Registro_de_decisoes.md) (vínculo idempotente).

**Base:** `TrabalhoWeb01.pdf` (seção 3 "Atores e permissões", RF07, RF09 e RNF06), o DER e o contrato da API. Cada decisão indica se veio do PDF ou se foi escolha do autor.

Legenda:
- **PDF**: exigido pelo enunciado.
- **Necessário**: o PDF não cita, mas sem isso uma exigência do PDF não funciona.
- **Escolha**: o PDF deixa em aberto; foi escolhido entre opções.

---

## 0. O que foi feito

| Arquivo | Papel |
| --- | --- |
| `model/OperadorModel.java` | Novo. Tabela `operador` e o vínculo `operador_ponto` (`@ManyToMany`) |
| `repository/OperadorRepository.java` | Novo. `existsByEmail` e `findByEmail` |
| `dto/operador/OperadorRequest.java`, `OperadorResponse.java` | Novos. Entrada (nome, e-mail) e saída (com a lista `pontos`) |
| `services/operador/OperadorService.java` | Novo. Cadastro, consulta, atualização, vincular e desvincular |
| `controller/OperadorController.java` | Novo. Rotas `/api/v1/operadores`, todas conferindo o ator |
| `services/contexto/TipoAtor.java`, `ContextoAtor.java` | Novos. Os três atores do PDF e o ator já conferido |
| `services/contexto/ContextoService.java` | Novo. Resolve o ator pelos cabeçalhos e traz as regras de escopo |
| `dto/contexto/ContextoResponse.java`, `controller/ContextoController.java` | Novos. `GET /api/v1/contexto` |
| `services/contexto/DadosDemoService.java`, `controller/DemoController.java` | Novos. Só existem no perfil `demo` |
| `exception/AcessoNegadoException.java` | Novo. Erro `403` |
| `exception/CodigoErro.java` | Uma linha acrescentada: `ACESSO_NEGADO(FORBIDDEN)` |
| `test/.../OperadorIntegracaoTest.java` (8) e `ContextoServiceTest.java` (4) | Novos testes |
| `docs/CONTRATO_API.md`, `README.md` | Nova seção da API-09, código `ACESSO_NEGADO` e como rodar o perfil `demo` |

Removidas as três classes vazias `TesteOperadorRequest`, `TesteOperadorResponse` e `TesteOperadorService` (marcadores de pasta, sem uso). O `GlobalExceptionHandler` **não** foi alterado.

---

## 1. Decisões tomadas

### 1.1 Operador no mesmo padrão do morador
- **Tipo:** PDF (RF07 e seção 10: "ID, nome, e-mail e vínculos com pontos").
- **Escolhido:** `nome` e `email` obrigatórios; e-mail normalizado (minúsculo, sem espaços) no construtor do `OperadorRequest` e único entre operadores (`409 DUPLICIDADE`), com a mesma dupla checagem do `MoradorService` (consulta antes + `UNIQUE` do banco).
- **Por quê:** o DER já define `email UNIQUE` pelo mesmo motivo do morador; repetir o padrão evita regra nova.
- **Consequência:** não há exclusão de operador (o histórico guardará o id dele). A inativação fica com a #17 (API-14); o campo `ativo` já existe e o contexto já recusa operador inativo.

### 1.2 Vínculo com `@ManyToMany`, sem entidade própria
- **Tipo:** Escolha.
- **Situação encontrada:** `operador_ponto` (V4) só tem as duas chaves, sem coluna extra.
- **Opções:** (a) `@ManyToMany` com `@JoinTable` no `OperadorModel`; (b) entidade `OperadorPontoModel` com chave composta, como o `PontoMaterialModel` da API-07.
- **Escolhido:** (a).
- **Por quê:** a API-07 precisou de entidade porque o vínculo tem `observacoes` e `limite`. Aqui não há nada além das chaves, então (a) usa dois arquivos a menos.
- **Consequência:** os pontos ficam num `Set`, que não aceita repetido; a chave primária do banco garante o mesmo. Se o vínculo ganhar colunas no futuro, será preciso migrar para (b).

### 1.3 `PUT` de vínculo idempotente: `201` ao criar, `200` se já existia
- **Tipo:** Escolha (igual à decisão 1.2 da API-07; o contrato já pedia isso).
- **Escolhido:** `PUT /operadores/{id}/pontos/{pontoId}` sem corpo. `DELETE` responde `204`; vínculo inexistente responde `404`.
- **Consequência:** repetir o vínculo nunca cria segunda linha nem dá erro (teste `vinculoRepetidoNaoDuplicaEConsultaInformaOsPontos`). O vínculo com ponto inativo é permitido: o operador pode ter atendimentos antigos daquele ponto.

### 1.4 Seleção de ator por cabeçalhos `X-Ator-Tipo` e `X-Ator-Id`
- **Tipo:** Escolha (o PDF permite "atores fictícios e uma seleção de usuário para demonstração").
- **Opções:** (a) cabeçalhos HTTP; (b) parâmetros na URL (`?atorTipo=&atorId=`).
- **Escolhido:** (a), decidido pelo responsável da issue.
- **Por quê:** não muda o caminho de nenhuma rota, não aparece em links nem em logs de URL, e é onde uma autenticação de verdade colocaria o token depois.
- **Consequência:** o front guarda o ator escolhido e envia os dois cabeçalhos. `ADMINISTRADOR` não tem cadastro no DER, então dispensa o `X-Ator-Id`.

### 1.5 O backend resolve o contexto; o id não é prova de identidade
- **Tipo:** PDF (seção 3 e critério "IDs enviados pelo cliente não são apresentados como prova de identidade").
- **Escolhido:** o `ContextoService.resolver` busca o ator no banco pelo tipo informado, exige que esteja ativo e carrega os pontos do operador. O cliente nunca informa "meus pontos": isso vem do banco. O aviso de que é simulação está no Javadoc do `ContextoService`, na descrição do OpenAPI, no contrato e no README.
- **Por quê:** o PDF diz que a seleção "não constitui autenticação nem deve ser utilizada em produção". O que o backend garante é a coerência (tipo certo, ator existente, vínculos reais), não a identidade.
- **Consequência:** qualquer pessoa pode se passar por qualquer ator trocando o cabeçalho. Não foi implementado JWT nem Spring Security (extensão opcional do PDF, seção 12).

### 1.6 Novo erro `403 ACESSO_NEGADO`
- **Tipo:** Escolha, decidida pelo responsável da issue.
- **Opções:** (a) código novo `ACESSO_NEGADO` com HTTP 403; (b) reusar `409 REGRA_NEGOCIO` ou `404`.
- **Escolhido:** (a). `AcessoNegadoException` estende `RegraNegocioException`, então o `GlobalExceptionHandler` já a trata sem mudança; só entrou uma linha no enum `CodigoErro`.
- **Por quê:** 403 é o código HTTP para "sem permissão", e o front consegue diferenciar de conflito de regra.
- **Consequência:** ator ausente, de tipo inválido, inexistente ou inativo responde o mesmo `403`, com a mesma mensagem, sem revelar qual foi o motivo. Altera um arquivo da API-03 (do Genisson), só acrescentando.

### 1.7 Onde o contexto vale nesta entrega
- **Tipo:** Escolha, decidida pelo responsável da issue.
- **Opções:** (a) só nas rotas novas + serviço pronto para as próximas issues; (b) exigir administrador também em materiais, pontos e vínculos já entregues.
- **Escolhido:** (a).
- **Por quê:** (b) mudaria código e testes de outros integrantes e obrigaria o front a mandar cabeçalho em tudo. As ações em que o PDF exige o bloqueio (RF07: operador não atualiza solicitação de outro ponto; RF09: consulta pelo contexto do ator) só existem a partir da #13.
- **Consequência:** as rotas de operador exigem administrador; `GET /operadores/{id}` também aceita o próprio operador. `exigirOperadorDoPonto` e `exigirMoradorDono` estão prontos e testados, mas ainda sem rota que os chame: as issues #13 a #16 devem usá-los. As rotas administrativas antigas continuam sem conferir ator.

### 1.8 `GET /contexto` e `GET /operadores` (listagem)
- **Tipo:** Escolha (ambos eram propostas do PLAN-03 "a confirmar com a equipe" no contrato).
- **Escolhido:** incluir os dois. `GET /contexto` devolve tipo, id, nome e `pontoIds` do ator dos cabeçalhos. `GET /operadores` é paginado, por nome, só para o administrador.
- **Por quê:** o front precisa confirmar "quem sou eu e quais são meus pontos" depois de selecionar o ator, e a tela administrativa precisa listar operadores.
- **Consequência:** dois endpoints além do mapa original; ficam para a revisão confirmar.

### 1.9 Atores fictícios isolados no perfil Spring `demo`
- **Tipo:** PDF (RNF06: dados fictícios) + CONTRIBUTING ("isole a seleção de ator de demonstração em um perfil local").
- **Opções:** (a) classe Java `@Profile("demo")` que cria os dados ao subir; (b) script Flyway numa pasta `db/demo`; (c) só documentar os `curl`.
- **Escolhido:** (a): `DadosDemoService` cria 1 moradora, 2 pontos (A e B) e 2 operadores (Ana no A, Bruno no B). `GET /demo/atores` (também `@Profile("demo")`) lista os atores com seus ids.
- **Por quê não (b):** o Flyway grava cada script no histórico; ao subir depois **sem** o perfil, ele acusaria "migração aplicada não encontrada" e a API não subiria.
- **Consequência:** sem o perfil, as duas classes nem são carregadas e `/demo/atores` responde `404` (testado). A criação é idempotente (confere o e-mail da Ana antes): reiniciar não duplica. Os e-mails usam o domínio fictício `demo.exemplo.org`. Os ids dependem do banco, por isso o front deve ler `/demo/atores` em vez de fixar números.

---

## 2. Validação

**Ambiente:** PostgreSQL 17 descartável (container `postgres:17`, sem volume, só em `127.0.0.1`, porta 55432, senha descartável), Java 21. Container parado, API encerrada e senha temporária apagada ao fim.

**Comandos:**
1. Banco: `docker run --rm -d --name coleta-tmp-pg-12 --tmpfs /var/lib/postgresql/data -e POSTGRES_PASSWORD=<senha descartável> -e POSTGRES_DB=coleta_teste -p 127.0.0.1:55432:5432 postgres:17`.
2. Testes: `./mvnw -B -ntp test`, com `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD` apontando para o banco.
3. Perfil demo: `./mvnw -B -ntp -q spring-boot:run -Dspring-boot.run.profiles=demo -Dspring-boot.run.arguments=--server.port=18080` e chamadas `curl`.
4. Limpeza: encerrar a API e `docker stop coleta-tmp-pg-12`.

**Resultado dos testes (2026-11-11):** **133 testes, 0 falhas, 0 erros** e `BUILD SUCCESS`. Desses, 12 são desta issue: 8 em `OperadorIntegracaoTest` (banco real, dados desfeitos ao fim) e 4 em `ContextoServiceTest` (sem banco). Antes eram 121.

**Chamadas reais no perfil `demo`** (ids gerados pelo banco: Ana = operador 8, ponto A = 34; Bruno = operador 9, ponto B = 35):

| # | Chamada | Resposta |
| --- | --- | --- |
| 1 | `GET /demo/atores` | `200`: Administrador, Maria (morador 14), Ana (`pontoIds [34]`), Bruno (`pontoIds [35]`) |
| 2 | `GET /contexto` sem cabeçalhos | `403 ACESSO_NEGADO` |
| 3 | `GET /contexto` como Ana | `200`, `{"tipo":"OPERADOR","id":8,...,"pontoIds":[34]}` |
| 4 | `GET /operadores/8` como Ana | `200`, com `pontos: [{"id":34,"nome":"Ecoponto Demo A"}]` |
| 5 | `GET /operadores/9` como Ana | `403`, "Operador só pode consultar os próprios dados." |
| 6 | `PUT /operadores/8/pontos/35` como Ana (ponto B) | `403`, "Operação permitida apenas ao administrador." |
| 7 | `PUT /operadores/8/pontos/34` como administrador (repetido) | `200`, continua um vínculo só |
| 8 | `GET /contexto` como operador `999999` | `403 ACESSO_NEGADO` |
| 9 | `GET /operadores?size=1` como administrador | `200`, `totalElements: 2`, `totalPages: 2` |
| 10 | `GET /v3/api-docs` | traz `/contexto`, `/demo/atores`, `/operadores`, `/operadores/{id}` e `/operadores/{id}/pontos/{pontoId}` |

Depois de reiniciar a API no perfil `demo`, o banco continuou com 2 operadores, 2 pontos, 1 morador e 2 vínculos (sem duplicar).

**Critérios de aceite da issue:**

| Critério | O que prova |
| --- | --- |
| Operador vinculado a pontos conforme DER; consulta informa os vínculos | `vinculoRepetidoNaoDuplicaEConsultaInformaOsPontos`; chamada 4 |
| Contexto distingue tipos de ator e restringe o escopo | `contextoDistingueOsTiposDeAtorEInformaOsPontosDoOperador`, `operadorSoConsultaASiMesmoENaoAdministraCadastros`; chamadas 3, 5 e 6 |
| IDs do cliente não são prova de identidade | Decisão 1.5; avisos no código, OpenAPI, contrato e README |
| Perfil demo e limitações documentados, dados fictícios | Decisão 1.9; README e contrato; chamada 1 |
| Vínculo repetido | `vinculoRepetido...`; chamada 7 |
| Ator desconhecido | `atorAusenteDesconhecidoOuDeTipoInvalidoResponde403`; chamadas 2 e 8 |
| Operador do ponto A tentando ação no ponto B | `ContextoServiceTest.operadorDoPontoANaoAgeNoPontoB` (regra pronta para as solicitações); chamadas 5 e 6 |

---

## 3. Pendentes

- Ligar `exigirOperadorDoPonto` e `exigirMoradorDono` às rotas de solicitação: #13 a #16 (API-10 a API-13). Só então o aceite do RF07 ("não pode atualizar solicitações de outro ponto") fica demonstrado ponta a ponta.
- Inativar operador e bloquear remoção de vínculo com atendimentos abertos (RN05): #17 (API-14).
- Decidir com a equipe se as rotas administrativas antigas (materiais, pontos, materiais aceitos) passam a exigir `ADMINISTRADOR` (decisão 1.7).
- O perfil `demo` não tem teste automatizado: um teste com o perfil ligado gravaria dados permanentes no banco dos outros testes. Foi validado pelas chamadas reais acima.
- Revisão do Genisson sobre as decisões 1.6 (linha nova no `CodigoErro` da API-03), 1.7 (escopo) e 1.8 (dois endpoints a mais).
