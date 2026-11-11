# Registro de decisões - Pontos de coleta e disponibilidade (issue #9, API-06)

Registro das decisões da implementação de pontos de coleta (issue #9, API-06), com justificativa e consequência de cada uma. Modelo de dados: [DER.md](../../DER.md). Contrato: [CONTRATO_API.md](../../CONTRATO_API.md). Padrão seguido: [Registro da API-04](../issue-api-04/Registro_de_decisoes.md) (moradores e endereços) e [Registro da API-05](../issue-api-05/Registro_de_decisoes.md) (materiais).

**Base:** `TrabalhoWeb01.pdf` (RF04), o DER e o contrato da API. Cada decisão indica se veio do PDF ou se foi escolha do autor.

Legenda:
- **PDF**: exigido pelo enunciado.
- **Necessário**: o PDF não cita, mas sem isso uma exigência do PDF não funciona.
- **Escolha**: o PDF deixa em aberto; foi escolhido entre opções.

---

## 1. Decisões tomadas

### 1.1 Endereço do ponto com campos próprios no `PontoModel`
- **Tipo:** Escolha.
- **O que o PDF diz:** RF04 pede ponto com nome, endereço, contato, horários, situação e disponibilidade domiciliar. Não diz como organizar o endereço no código.
- **Situação encontrada:** a tabela `ponto_coleta` (V3) tem os campos de endereço na própria tabela, os mesmos 8 do `endereco` do morador. O `EnderecoModel` e a regra "número ou sem número" do `EnderecoService` já foram mergeados na API-04.
- **Opções:** (a) campos próprios no `PontoModel` e a regra do número repetida no `PontoService`; (b) classe `@Embeddable` compartilhada por `EnderecoModel` e `PontoModel`; (c) campos próprios, mas a validação do número extraída para um método compartilhado.
- **Escolhido:** (a).
- **Por quê:** a tabela já é plana, então o Model espelha o banco. (b) altera o `EnderecoModel` da API-04 e cria uma estrutura nova, o que é decisão de arquitetura da equipe. (c) obriga a alterar o `EnderecoService`, já revisado e mergeado.
- **Consequência:** os 8 campos e a regra do número ficam repetidos entre morador e ponto. Se a equipe quiser unificar depois, a refatoração (b) ou (c) mexe nos dois módulos de uma vez.

### 1.2 Listagem administrativa `GET /pontos` incluída na API-06
- **Tipo:** Escolha (reconsiderada em 2026-11-11; a primeira escolha foi não incluir).
- **O que o PDF diz:** o administrador deve "gerenciar pontos" (item 3), e as interfaces mínimas incluem "administração de materiais e pontos" (item 11). A consulta pública (RF06) devolve só pontos ativos. O PDF não cita um endpoint de listagem administrativa.
- **O que o contrato diz:** a tabela de caminhos lista `POST /pontos`, `GET /pontos/{id}`, `PUT /pontos/{id}` e `PATCH /pontos/{id}/situacao` para a API-06. `GET /pontos` aparece só como proposta vinda da revisão das telas (PLAN-03), "ainda a confirmar com a equipe".
- **Opções:** (a) não incluir; (b) incluir, copiando o `listar()` do `MaterialService` (filtro opcional `ativo` e paginação).
- **Escolhido:** (b). `GET /api/v1/pontos?ativo=&page=&size=`, ordenado por nome sem diferenciar maiúsculas, com o `id` como desempate (`PaginacaoService`), e sem `ativo` devolve todos, inclusive inativos.
- **Por quê:** sem a listagem, quem inativa um ponto só o encontra se souber o `id`, e a consulta pública não mostra inativos, então a tela de administração (Unidade 2) não teria como reativá-lo. O custo é pequeno, porque o padrão já existe no material.
- **Consequência:** é um endpoint que o contrato ainda trata como proposta. Entra na revisão do Herbert, e `CONTRATO_API.md` precisa ser atualizado se a equipe confirmar. Se a equipe recusar, o endpoint sai sem afetar os demais.

### 1.3 Só o DTO administrativo; o DTO público fica para a API-08
- **Tipo:** Escolha (leitura do item 4 da issue: "separar DTO administrativo e dados públicos para consulta").
- **O que o contrato diz:** a consulta pública (`GET /publico/pontos`, `GET /publico/pontos/{id}`) é da API-08 (issue #11, Herbert).
- **Opções:** (a) criar só os DTOs administrativos (`PontoRequest`, `PontoResponse`, que traz `ativo`, e `PontoSituacaoRequest`); (b) criar também um `PontoPublicoResponse` agora.
- **Escolhido:** (a).
- **Por quê:** nenhum código da API-06 chamaria o DTO público, e código sem uso é código morto. A API-08 é quem expõe a consulta pública e define o que ela mostra.
- **Consequência:** a separação pedida no item 4 acontece quando a API-08 criar o DTO público. É uma leitura menos literal da issue, por isso entra na revisão do Herbert.

### 1.4 `uf` e `cep` mapeados como `CHAR` no Hibernate
- **Tipo:** Necessário (mesmo caso da [API-04, decisão 1.2](../issue-api-04/Registro_de_decisoes.md)).
- **O que o PDF diz:** RF04 exige o endereço do ponto. A V3 define `uf CHAR(2)` e `cep CHAR(8)` em `ponto_coleta`.
- **Por quê:** com `ddl-auto=validate`, o Hibernate espera `VARCHAR` para uma `String`, e o banco tem `bpchar`. O `@JdbcTypeCode(SqlTypes.CHAR)` em `uf` e `cep` do `PontoModel` mantém o banco como o DER define.
- **Consequência:** a anotação foi aplicada desde o início, então o erro da API-04 não foi reproduzido aqui. O `validate` aceitou o `PontoModel` contra a tabela real na execução dos testes (seção 2).

### 1.5 Um único `PontoRequest` para cadastrar e atualizar, sem `ativo`
- **Tipo:** Escolha.
- **O que o PDF diz:** RF04 pede cadastrar e atualizar ponto. A situação (ativo/inativo) é um dado do ponto, mas o contrato tem um caminho próprio para ela (`PATCH /pontos/{id}/situacao`).
- **Opções:** (a) um `PontoRequest` sem `ativo` para `POST` e `PUT`, e a situação só pelo `PATCH`; (b) `ativo` dentro do `PontoRequest`; (c) dois DTOs, um por operação.
- **Escolhido:** (a).
- **Por quê:** os campos e as validações do `POST` e do `PUT` são os mesmos (como na API-04, decisão 1.3). Com `ativo` no `PUT`, editar o endereço poderia inativar o ponto por engano, e a mesma mudança teria dois caminhos.
- **Consequência:** o `PUT` nunca altera a situação, e o teste `atualizarAlteraOsDadosSemMudarASituacao` prova isso com um ponto inativo. Se o `PUT` precisar de um campo a mais, o DTO deverá ser dividido.

### 1.6 Mesmos formatos e normalização do endereço do morador
- **Tipo:** Escolha (segue a API-04, decisões 1.4, 1.6, 1.10 e 1.11).
- **Escolhido:**
  - `uf` com 2 letras na entrada e gravada em maiúsculas; `cep` com 8 dígitos, sem traço;
  - regra do número estrita no `PontoService`: `semNumero = true` exige `numero` vazio, e `semNumero = false` exige `numero` preenchido, com `400` apontando o campo `numero`;
  - `numero` gravado sem espaços nas pontas e `null` quando `semNumero` é verdadeiro;
  - `semNumero` ausente no JSON vale `false` (`Boolean` com valor padrão no construtor do `PontoRequest`).
- **Por quê:** o endereço do ponto e o do morador seguem as mesmas regras, e um cliente não precisa aprender dois formatos. O `Boolean` com padrão evita o `400` sem detalhe que o Jackson 3 devolve quando um campo `boolean` é omitido.
- **Consequência:** `49000-000`, com traço, é rejeitado com `400`. A regra do número precisa de teste no Service, e ela existe (decisão 1.1 trata da repetição entre os módulos).

### 1.7 `recolhimentoDomiciliar` obrigatório no JSON
- **Tipo:** Escolha.
- **O que o PDF diz:** RF04 pede a disponibilidade de recolhimento domiciliar. A V3 define `recolhimento_domiciliar BOOLEAN NOT NULL`, sem valor padrão.
- **Opções:** (a) obrigatório (`Boolean` com `@NotNull`); (b) ausente vale `false`, como o `semNumero`.
- **Escolhido:** (a).
- **Por quê:** o banco não tem valor padrão, então assumir `false` em silêncio esconderia um esquecimento do cliente. É esse campo que decide se uma solicitação domiciliar pode ser feita no ponto (API-10).
- **Consequência:** todo `POST` e `PUT` envia o campo. Sem ele, a API responde `400` apontando `recolhimentoDomiciliar` (teste `recolhimentoDomiciliarAusenteERejeitado`).

### 1.8 Ponto nasce ativo e a situação muda só pelo `PATCH`, com `ativo` obrigatório
- **Tipo:** Escolha (formato do corpo); a existência do `PATCH` vem do contrato.
- **O que o PDF diz:** RF04 pede situação do ponto e preservar o cadastro. O contrato lista `PATCH /pontos/{id}/situacao`.
- **Escolhido:** o `POST` cria o ponto ativo (o `PontoModel` começa com `ativo = true`, como o `DEFAULT TRUE` da V3). O `PATCH` recebe `{"ativo": true|false}`, com `@NotNull`, no mesmo formato do material.
- **Por quê:** o `@NotNull` impede que um corpo `{}` inative o ponto por acidente (teste `situacaoSemAtivoERejeitadaSemInativarPorAcidente`). Inativar só troca a marca, sem apagar: não existe `DELETE`, e a FK de `ponto_material` é `ON DELETE RESTRICT`.
- **Consequência:** o ponto nunca é excluído pela API. Bloquear a inativação de ponto com solicitações abertas (RN05) é da API-14.

### 1.9 Ponto inativo continua consultável; `Location` do `POST` aponta para um `GET` existente
- **Tipo:** Escolha.
- **O que o contrato diz:** a consulta pública é da API-08 (decisão 1.3). Aqui só existe a consulta administrativa.
- **Escolhido:** `GET /pontos/{id}` devolve o ponto mesmo inativo, e o `Location` do `POST` aponta para `/api/v1/pontos/{id}`.
- **Por quê:** o administrador precisa ver o ponto inativo para poder reativá-lo. Diferente do endereço da API-04 (decisão 1.12), o caminho do `Location` aqui responde `GET`.
- **Consequência:** o que aparece na consulta pública, inclusive se esconde inativos, fica a cargo da API-08.

### 1.10 Nome do ponto sem unicidade
- **Tipo:** Escolha (confirmada pelo autor em 2026-11-11).
- **O que o PDF diz:** pede nome único para o material (RF03) e para o morador, o e-mail. Para o ponto (RF04 e modelo de dados) não pede unicidade.
- **Situação encontrada:** a V3 não define `UNIQUE` em `nome`.
- **Escolhido:** a API não cria uma regra que o banco não tem. Dois pontos podem ter o mesmo nome.
- **Por quê:** o PDF não pede nome único, e uma regra a mais exigiria migração nova ou checagem no Service, o que seria "inventar demais".
- **Consequência:** pontos homônimos são aceitos. Se a equipe quiser nomes únicos, é preciso uma migração nova (nunca editar `V1` a `V6`) e um `409` no Service.

### 1.11 Verificação de disponibilidade do ponto para solicitação
- **Tipo:** Necessário (RN01 e critério da issue: "domiciliar desabilitado impede nova solicitação").
- **O que o PDF diz:** RN01 exige ponto ativo e com recolhimento domiciliar habilitado em toda nova solicitação.
- **Opções:** (a) `PontoService.buscarDisponivelParaSolicitacao(id)`, que a API-10 chama ao criar a solicitação; (b) deixar a regra para a API-10 escrever sozinha.
- **Escolhido:** (a). Devolve o `PontoModel` quando o ponto está ativo e com recolhimento domiciliar; senão responde `409 REGRA_NEGOCIO`. Segue o `MaterialService.buscarAtivoParaSolicitacao`.
- **Por quê:** a regra do ponto fica no módulo do ponto, e a API-10 só chama. Usa um código de erro que já existe, porque o contrato de erros é compartilhado.
- **Consequência:** nada chama o método até a API-10 existir. O bloqueio da inativação com solicitações abertas (RN05) não foi feito aqui: é da API-14 (#17), que também define a estratégia de locks.

---

## 2. Validação

**Ambiente:** PostgreSQL 17 descartável (container `postgres:17`, sem volume, só em `127.0.0.1`, porta 55432, senha descartável), Spring Boot 4.1.2, Java 21.0.13. Nenhum volume nem dado do usuário foi tocado, e o container foi parado ao fim.

**Comandos:**
1. Banco: `docker run --rm -d --name coleta-tmp-pg-9 --tmpfs /var/lib/postgresql/data -e POSTGRES_PASSWORD=<senha descartável> -e POSTGRES_DB=coleta_teste -p 127.0.0.1:55432:5432 postgres:17`.
2. Testes: `./mvnw -B -ntp test`, com `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD` apontando para o banco.
3. API: `./mvnw spring-boot:run` com as mesmas variáveis, e chamadas com `curl` em `http://localhost:8080`.
4. Limpeza: a API foi encerrada e `docker stop coleta-tmp-pg-9` parou o banco. O `--rm` remove o container.

**Resultado:** `./mvnw test` terminou com **105 testes, 0 falhas, 0 erros** e `BUILD SUCCESS`. Desses, 17 são desta issue. Como o contexto subiu com `spring.flyway.enabled=true` e `ddl-auto=validate` num banco vazio, as migrações foram aplicadas e o Hibernate aceitou o `PontoModel` contra `ponto_coleta` (decisão 1.4).

**Testes desta issue (enxutos, um por regra):**
- `PontoServiceTest` (9 execuções, com Mockito): cadastro com UF em maiúsculas, número sem espaços e ponto ativo; `semNumero` grava número nulo; rejeita número preenchido com `semNumero`; rejeita número vazio sem `semNumero`; buscar ponto inexistente; inativar e reativar; ponto inativo ou sem recolhimento domiciliar não recebe solicitação (2 casos); ponto ativo com domiciliar recebe.
- `PontoIntegracaoTest` (8, contra o banco real, com os dados desfeitos ao fim de cada teste): listagem ordenada por nome, com filtro `ativo` e paginação; cadastro persiste e é consultado; ponto sem nome nem endereço é rejeitado (campos `nome`, `logradouro`, `bairro`, `municipio`, `uf` e `cep`); `recolhimentoDomiciliar` ausente é rejeitado; inativar e reativar preserva o ponto; `PUT` altera os dados sem mudar a situação; `PATCH` sem `ativo` é rejeitado sem inativar; `404` no `GET` e no `PATCH` de ponto inexistente.

**Critérios de aceite da issue:**

| Critério | Teste que prova |
| --- | --- |
| Ponto sem nome ou endereço é rejeitado | `pontoSemNomeNemEnderecoERejeitado` |
| Cadastro completo persiste | `cadastroValidoPersisteEPodeSerConsultado` |
| Ponto é preservado ao inativar | `inativarEReativarPreservaOPonto` |
| Domiciliar desabilitado (ou ponto inativo) impede nova solicitação | `pontoInativoOuSemRecolhimentoDomiciliarNaoRecebeSolicitacao` (a API-10 chama o método) |
| Filtragem de ponto inativo | `listaPontosPorNomeComFiltroDeSituacaoEPaginacao` (`?ativo=true`) |

**O que a issue pede e depende de outras issues** (já está escrito nelas, não é preciso criar issue nova):

| Item | Issue que cobre |
| --- | --- |
| Consulta pública só com ponto ativo, domiciliar identificado e DTO público (item 4 da #9) | #11 (API-08) |
| Rejeitar solicitação em ponto indisponível, usando o método da decisão 1.11 | #13 (API-10) |
| Bloquear a inativação com solicitações abertas (`409`) e tratar a corrida com a criação | #17 (API-14) e #13 |
| Versão otimista das solicitações | #18 (API-15) |

**Arquivos removidos:** os marcadores de exemplo `TestePontoRequest`, `TestePontoResponse` e `TestePontoService`, substituídos pelas classes reais. Os marcadores dos outros módulos (operador, indicador, solicitação e os compartilhados) não foram tocados.

**Chamadas reais com `curl` (API rodando, banco vazio, dados fictícios):** os ids começam em 9 porque a rodada de testes, no mesmo banco, já consumiu a sequência de identidade (o rollback dos testes não devolve ids).

| # | Chamada | Esperado | Obtido |
| --- | --- | --- | --- |
| 01 | `POST /pontos` válido, UF `se` | `201` com `Location`, UF em maiúsculas, ativo | `201`, `Location: .../pontos/9`, `uf: "SE"`, `ativo: true` |
| 02 | `GET /pontos/9` | `200` | `200` |
| 03 | `POST` sem nome nem endereço | `400` com os campos | `400 ENTRADA_INVALIDA`, campos `bairro`, `municipio`, `uf`, `cep`, `nome` e `logradouro` |
| 04 | `POST` sem `recolhimentoDomiciliar` | `400` no campo | `400`, campo `recolhimentoDomiciliar` |
| 05 | `POST` com `numero` e `semNumero` verdadeiro | `400` no campo `numero` | `400`, campo `numero` |
| 06 | `POST` sem `numero`, `semNumero` verdadeiro | `201`, `numero` nulo | `201`, `numero: null`, id 10 |
| 07 | `PUT /pontos/9` muda o nome | `200` | `200`, `nome: "Ecoponto Norte"` |
| 08 | `PATCH /pontos/9/situacao` com `{"ativo":false}` | `200`, inativo | `200`, `ativo: false` |
| 09 | `PATCH` com `{}` | `400` no campo `ativo` | `400`, campo `ativo` |
| 10 | `PUT /pontos/9` com o ponto inativo | `200`, continua inativo | `200`, `ativo: false` |
| 11 | `GET /pontos?ativo=false` | só o ponto 9 | `totalElements: 1`, id 9 |
| 12 | `GET /pontos?ativo=true` | só o ponto 10 | `totalElements: 1`, id 10 |
| 13 | `GET /pontos?size=1` | 1 de 2, ordem por nome | `totalElements: 2`, `totalPages: 2`, primeiro "Ecoponto Centro" (id 10) |
| 14 | `GET /pontos?page=5&size=1` | `200` com `content` vazio | `200`, `content: []`, `totalElements: 2` |
| 15 | `GET /pontos?size=0` | `400` no campo `size` | `400`, campo `size` |
| 16 | `GET /pontos/999` | `404` | `404 REGISTRO_NAO_ENCONTRADO` |
| 17 | `PATCH /pontos/999/situacao` | `404` | `404 REGISTRO_NAO_ENCONTRADO` |
| 18 | `GET /pontos/abc` | `400` no campo `id` | `400`, campo `id` |

O OpenAPI (`/v3/api-docs`) lista os caminhos `/api/v1/pontos` (`GET`, `POST`), `/api/v1/pontos/{id}` (`GET`, `PUT`) e `/api/v1/pontos/{id}/situacao` (`PATCH`).

---

## 3. Pendentes

- Confirmar com a equipe (Herbert) a inclusão do `GET /pontos` (decisão 1.2) e, se confirmada, atualizar `CONTRATO_API.md` com o novo caminho.
- Revisão do Herbert sobre a decisão 1.3 (DTO público só na API-08).
- As decisões 1.7 (`recolhimentoDomiciliar` obrigatório) e 1.8 (ponto nasce ativo, `PATCH` com `{"ativo": ...}`) estavam aqui como "assumidas" e passaram a decisões porque o código e os testes que as aplicam foram aprovados. A 1.10 (nome sem unicidade) foi confirmada pelo autor.
- Integração com a API-14 (bloquear inativação de ponto com solicitações abertas, RN05, com estratégia de locks), com a API-10 (chamar `buscarDisponivelParaSolicitacao`) e com a API-08 (consulta pública e DTO público). Ver a tabela de critérios na seção 2.
