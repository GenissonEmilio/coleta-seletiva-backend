# Contrato proposto da API

**Convenções da API-03 (#6), sujeitas à revisão da equipe na PR; endpoints ainda em desenvolvimento.** As convenções de JSON, erros e paginação abaixo valem para todos os módulos. Os nomes de endpoints e ações do mapa continuam propostas da equipe, não imposições literais do PDF. O contrato é publicado em OpenAPI (springdoc, `/v3/api-docs` e `/swagger-ui.html`), gerado a partir dos controllers; cada módulo inclui seus exemplos.

## Convenções

- Prefixo `/api/v1`; JSON UTF-8; DTOs de entrada com sufixo `Request` e de saída com sufixo `Response`, implementados como classes ou records. Nunca serializar o grafo JPA diretamente.
- **IDs:** número inteiro (`Long`, `bigint` no DER). O protocolo da solicitação é um texto único à parte e não substitui o ID.
- **Datas e horas de eventos:** ISO 8601 com offset, por exemplo `"2026-11-12T14:30:00-03:00"` (`OffsetDateTime`). Datas sem hora, quando houver, usam `AAAA-MM-DD`.
- **Quantidades:** número JSON decimal (`BigDecimal`), com no máximo 3 casas decimais e 9 dígitos inteiros (banco `numeric(12,3)`), por exemplo `2.5`. Peças só aceitam valor inteiro.
- **Enums:** texto em maiúsculas sem acento, como `SOLICITADA`, `EM_RECOLHIMENTO` e `CONCLUIDA`. A interface exibe o texto com acento.
- **Booleanos:** `true`/`false`. Campos nomeados em `camelCase`, sem acento.
- **Campos opcionais ausentes:** enviados como `null` ou omitidos na entrada; na saída aparecem como `null`.
- **Códigos HTTP:** `201` e `Location` na criação; `200` com a representação atualizada nas atualizações e ações (`204` só para `DELETE`); `400` entrada inválida; `404` registro inexistente; `409` conflito de regra, unicidade ou versão. Falha inesperada é `500` com mensagem genérica.
- **Referência a outro registro no corpo:** se o ID informado não existe, `404`; se existe mas é incompatível com a regra (ex.: material não aceito pelo ponto), `409`. Campo malformado ou ausente é `400`.
- **Versão:** toda mutação de solicitação recebe `versao` no corpo; versão diferente da gravada retorna `409 CONFLITO_VERSAO`. A resposta de qualquer mutação devolve a nova `versao`.
- **Períodos:** início inclusivo e fim exclusivo, em ISO 8601 com offset. Solicitações filtram pela data de criação; quantidades efetivas pela data de conclusão.

### Erros

Todo erro tem o mesmo formato, e `errosCampos` é sempre uma lista (vazia quando o erro não é de campo):

```json
{
  "codigo": "ENTRADA_INVALIDA",
  "mensagem": "Revise os campos informados.",
  "errosCampos": [{"campo": "itens[0].quantidadeEstimada", "mensagem": "deve ser maior que zero"}]
}
```

`campo` usa o caminho do JSON (`itens[0].quantidadeEstimada`) para a interface marcar o campo certo. Nunca há stack trace, SQL, nome de tabela ou credencial na resposta; o detalhe técnico vai só para o log do servidor.

| Código | HTTP | Quando |
| --- | --- | --- |
| `ENTRADA_INVALIDA` | 400 | Bean Validation, JSON malformado, parâmetro com tipo errado, `page` ou `size` fora do intervalo |
| `REGISTRO_NAO_ENCONTRADO` | 404 | ID do caminho ou do corpo inexistente |
| `ROTA_NAO_ENCONTRADA` | 404 | Caminho que não existe |
| `METODO_NAO_PERMITIDO` | 405 | Verbo HTTP não aceito no caminho |
| `TIPO_NAO_SUPORTADO` | 415 | Corpo que não é `application/json` |
| `DUPLICIDADE` | 409 | E-mail, nome de material ou vínculo já existente |
| `CONFLITO_VERSAO` | 409 | `versao` desatualizada ou escrita concorrente |
| `TRANSICAO_INVALIDA` | 409 | Mudança de situação não permitida |
| `MATERIAL_INCOMPATIVEL` | 409 | Material inativo ou não aceito pelo ponto, ou limite excedido |
| `PONTO_COM_SOLICITACOES_ABERTAS` | 409 | Inativar ponto ou remover vínculo com atendimento aberto (RN05); a mensagem informa a quantidade |
| `REGRA_NEGOCIO` | 409 | Demais regras e violações de integridade do banco |
| `ERRO_INTERNO` | 500 | Falha inesperada |

Mensagens de validação saem em português (locale `pt_BR`).

### Paginação

- Parâmetros `page` (a partir de 0, padrão 0) e `size` (de 1 a 100, padrão 20). Valor fora do intervalo ou não numérico retorna `400 ENTRADA_INVALIDA`, apontando o campo `page` ou `size`; a API nunca corrige o valor em silêncio.
- A ordem de cada listagem é fixa e definida pelo módulo (por exemplo, nome crescente; solicitações da mais recente para a mais antiga), sempre com o ID como desempate. O cliente não escolhe a ordenação nesta versão.
- Envelope de resposta `PaginaResponse<T>`, igual em todas as listagens. Exemplo de página além do fim, com 45 registros e tamanho 20:

```json
{
  "content": [],
  "page": 3,
  "size": 20,
  "totalElements": 45,
  "totalPages": 3
}
```

Página além do fim retorna `200` com `content` vazio.

### Organização das implementações comuns

- `dto/common`: `PaginacaoRequest`, `PaginaResponse`, `ErroResponse` e `ErroCampoResponse`.
- `services/paginacao`: `PaginacaoService`, usado pelos serviços dos módulos para criar o `Pageable` com ordenação estável.
- `exception`: `CodigoErro`, `EntradaInvalidaException`, `RegraNegocioException`, `RecursoNaoEncontradoException` e `GlobalExceptionHandler`.
- `config`: `OpenApiConfig`.

Essa organização segue o [guia da equipe](GUIA_IMPLEMENTACAO.md#4-organização-do-código). Os nomes Java foram padronizados; os campos JSON de erros e paginação continuam os mesmos.

## Mapa de endpoints a desenvolver

| Área | Operações propostas | Tarefa |
| --- | --- | --- |
| Moradores | `POST /moradores`, `GET /moradores/{id}`, `PUT /moradores/{id}` e inativação preservando referências | API-04, API-14 |
| Endereços | `POST /moradores/{id}/enderecos`, `GET /moradores/{id}/enderecos`, `PUT /moradores/{id}/enderecos/{enderecoId}` | API-04 |
| Materiais | `POST /materiais`, `GET /materiais`, `GET /materiais/{id}`, `PUT /materiais/{id}`, `PATCH /materiais/{id}/situacao` | API-05 |
| Pontos | `POST /pontos`, `GET /pontos/{id}`, `PUT /pontos/{id}`, `PATCH /pontos/{id}/situacao` | API-06, API-14 |
| Materiais aceitos | `PUT /pontos/{id}/materiais/{materialId}`, `GET /pontos/{id}/materiais`, `DELETE /pontos/{id}/materiais/{materialId}` | API-07, API-14 |
| Consulta pública | `GET /publico/pontos?nome=&municipio=&bairro=&materialId=&page=&size=`, `GET /publico/pontos/{id}`, `GET /publico/materiais` | API-08 |
| Operadores | `POST /operadores`, `GET /operadores/{id}`, `PUT /operadores/{id}`, vínculo/remoção em `/operadores/{id}/pontos/{pontoId}` | API-09 |
| Solicitações | `POST /solicitacoes`, `GET /solicitacoes`, `GET /solicitacoes/{id}`, `GET /solicitacoes/{id}/historico`, `PUT /solicitacoes/{id}/dados` | API-10, API-11 |
| Agendamento e encerramentos | `POST /solicitacoes/{id}/agendamento`, `/recusa`, `/cancelamento` | API-12 |
| Execução | `POST /solicitacoes/{id}/inicio`, `/conclusao`, `/reagendamento`, `/tentativa-sem-sucesso` | API-13 |
| Resumo | `GET /indicadores/solicitacoes?inicio=&fim=`, `GET /indicadores/recolhimentos?inicio=&fim=` | API-16 |

Listagens administrativas adicionais necessárias ao uso das telas devem seguir as mesmas regras de paginação e privacidade. Propostas vindas da revisão das telas (PLAN-03), ainda a confirmar com a equipe: `GET /pontos` (listagem administrativa, inclui inativos), `GET /operadores`, filtro `domiciliar=true` em `GET /publico/pontos`, e um endpoint de atores de demonstração para o contexto local. Os caminhos abreviados da linha de ações mantêm o prefixo `/solicitacoes/{id}`. No vínculo via PUT, devolver 201 ao criar e 200/204 ao atualizar; repetição não gera uma segunda linha. A equipe deve documentar a política de duplicidade compatível com RF05.

## Campos e invariantes essenciais

| Recurso | Conteúdo mínimo |
| --- | --- |
| Morador | nome, e-mail normalizado único, telefone; situação para preservação por inativação |
| Endereço | proprietário, logradouro, número ou semNumero, bairro, município, UF, CEP, complemento opcional |
| Material | nome único, descrição, unidade, orientação de preparo, situação |
| Ponto | nome, endereço, contato, horários, situação, recolhimentoDomiciliar |
| Material aceito | ponto/material únicos, observações e limite opcional positivo na unidade do material |
| Operador | nome, e-mail e vínculos com pontos; dados de demonstração fictícios |
| Solicitação | protocolo, morador, ponto, cópia do endereço, situação, datas com offset, observações, versão |
| Item | material, cópia da unidade, estimada positiva, efetiva não negativa quando informada; único por material na solicitação |
| Histórico | situação anterior/nova, evento, instante, responsável, motivo e dados da tentativa; valores opcionais apenas quando o evento não os exigir |

## Materiais, unidades e inativação (API-05)

Todos os caminhos abaixo usam o prefixo `/api/v1`. O catálogo administrativo inclui materiais inativos, para permitir sua manutenção e a consulta dos registros anteriores.

| Operação | Sucesso | Erros possíveis |
| --- | --- | --- |
| `POST /materiais` | `201` com `Location` e `MaterialResponse` | `400`, `409 DUPLICIDADE` |
| `GET /materiais?ativo=&page=&size=` | `200`, `PaginaResponse<MaterialResponse>` | `400` |
| `GET /materiais/{id}` | `200`, inclusive se inativo | `400`, `404` |
| `PUT /materiais/{id}` | `200`, sem alterar `ativo` | `400`, `404`, `409 DUPLICIDADE` |
| `PATCH /materiais/{id}/situacao` | `200`, representação atualizada | `400`, `404` |

- `MaterialRequest`: `nome` obrigatório (até 80 caracteres), `unidade` obrigatória, `descricao` e `orientacaoPreparo` opcionais. A criação começa com `ativo: true`.
- O nome é aparado nas pontas, preservando a apresentação. A unicidade ignora caixa e espaços nas pontas: `Papel` e `" PAPEL "` conflitam, mesmo se o material existente estiver inativo. A regra também está no banco, pela migração V7. Acentos e espaços internos continuam significativos.
- Unidades aceitas: `KG` (peso decimal), `LITRO` (volume decimal) e `UNIDADE` (peças inteiras). Outros valores, inclusive `kg`, respondem `400 ENTRADA_INVALIDA`.
- Sem `ativo`, a listagem inclui ambos os estados. `ativo=true` ou `ativo=false` filtra no banco antes da paginação. Ordem por nome, ignorando caixa, e ID como desempate; `page`/`size` seguem o envelope comum.
- A situação recebe `MaterialSituacaoRequest`: `{"ativo": false}` inativa e `{"ativo": true}` reativa. O campo é obrigatório; ausente ou nulo responde `400` apontando `ativo`. Repetir a mesma situação é idempotente.
- A inativação preserva o material, os vínculos e os itens existentes. Não há endpoint `DELETE` de material. Um `PUT` de dados não reativa o material; a troca de unidade não reescreve a unidade copiada em itens anteriores.

Exemplo de cadastro, com dados fictícios:

```http
POST /api/v1/materiais
Content-Type: application/json

{"nome":"Papel","descricao":"Papel e papelão secos","unidade":"KG","orientacaoPreparo":"Separar limpo e seco"}
```

```http
201 Created
Location: http://localhost:8080/api/v1/materiais/1

{"id":1,"nome":"Papel","descricao":"Papel e papelão secos","unidade":"KG","orientacaoPreparo":"Separar limpo e seco","ativo":true}
```

Exemplo de inativação:

```http
PATCH /api/v1/materiais/1/situacao
Content-Type: application/json

{"ativo":false}
```

A resposta é `200`, com os mesmos campos do material e `ativo: false`. Ele continua acessível no detalhe e na listagem administrativa. Um nome repetido responde:

```http
409 Conflict

{"codigo":"DUPLICIDADE","mensagem":"Já existe um material com este nome.","errosCampos":[]}
```

### Integração com as próximas funcionalidades

- A API-10 deve chamar `MaterialService.buscarAtivoParaSolicitacao(id)` dentro da transação de criação/correção. Inexistente responde `404`; inativo, `409 MATERIAL_INCOMPATIVEL`. O método devolve o Model para o uso interno do serviço, e não para serialização HTTP. O item deve receber uma **cópia** da unidade naquele momento.
- `QuantidadeMaterialService.validarPositiva(valor, unidade, campo)` atende estimativas e limites informados; `validarNaoNegativa` atende efetivos. Usar `BigDecimal`, aceitando no máximo 9 dígitos inteiros e 3 casas decimais significativas, sem arredondar; `UNIDADE` rejeita frações. `2.000` é inteiro. O parâmetro `campo` permite apontar, por exemplo, `itens[0].quantidadeEstimada`.
- A API-07 deve pular a validação de limite quando ele for ausente. A API-13 deve usar a unidade **copiada no item**, e validar também que ao menos um efetivo seja positivo ao concluir. As validações de ponto/material aceito, limite e transação única pertencem aos serviços dessas operações.
- Os serviços de solicitação ainda serão implementados na API-10/API-13; estes métodos são o contrato de integração da API-05. A consulta pública apenas de materiais disponíveis pertence à API-08.

Decisões e validação: [registro da API-05](documentos-issues/issue-api-05/Registro_de_decisoes.md).

## Moradores e endereços (API-04)

Todos os caminhos usam o prefixo `/api/v1`. Dados dos exemplos são fictícios. Decisões completas em [docs/documentos-issues/issue-api-04](documentos-issues/issue-api-04/Registro_de_decisoes.md).

| Operação | Sucesso | Erros possíveis |
| --- | --- | --- |
| `POST /moradores` | `201` com `Location` | `400`, `409 DUPLICIDADE` |
| `GET /moradores/{id}` | `200` | `404` |
| `PUT /moradores/{id}` | `200` | `400`, `404`, `409 DUPLICIDADE` |
| `POST /moradores/{moradorId}/enderecos` | `201` com `Location` | `400`, `404` |
| `GET /moradores/{moradorId}/enderecos?page=&size=` | `200` (`PaginaResponse`) | `400`, `404` |
| `PUT /moradores/{moradorId}/enderecos/{enderecoId}` | `200` | `400`, `404` |

**Regras do morador**
- `nome` e `email` são obrigatórios. `telefone` é opcional (até 20 caracteres).
- O e-mail é normalizado (minúsculo, sem espaços nas pontas) **antes** da validação e da checagem de unicidade. `"  MARIA@Exemplo.com "` e `maria@exemplo.com` são o mesmo e-mail e geram `409 DUPLICIDADE`. A unicidade também existe no banco.
- Moradores não são excluídos. A inativação é da API-14.

**Regras do endereço**
- Obrigatórios: `logradouro`, `bairro`, `municipio`, `uf` (2 letras, gravada em maiúsculas) e `cep` (8 dígitos, sem traço). Opcional: `complemento`.
- **Número ou sem número, regra inequívoca:** `semNumero = true` exige `numero` vazio, e `semNumero = false` exige `numero` preenchido. Violações geram `400` apontando o campo `numero`. `semNumero` ausente vale `false`.
- Um endereço pertence a um único morador. Acessar ou atualizar um endereço por um morador que não é o dono responde `404`, como se ele não existisse. O mesmo vale para o uso do endereço numa solicitação.
- A listagem é paginada e ordenada pelo `id` (ordem de cadastro).

**Exemplo: cadastrar morador**

```http
POST /api/v1/moradores
Content-Type: application/json

{"nome": "Maria Souza", "email": "maria@exemplo.com", "telefone": "79999990000"}
```

```http
201 Created
Location: http://localhost:8080/api/v1/moradores/1

{"id": 1, "nome": "Maria Souza", "email": "maria@exemplo.com", "telefone": "79999990000", "ativo": true}
```

**Exemplo: e-mail repetido** (mesmo e-mail com outra caixa e espaços)

```http
409 Conflict

{"codigo": "DUPLICIDADE", "mensagem": "Já existe um morador com este e-mail.", "errosCampos": []}
```

**Exemplo: nome ausente**

```http
400 Bad Request

{"codigo": "ENTRADA_INVALIDA", "mensagem": "Revise os campos informados.",
 "errosCampos": [{"campo": "nome", "mensagem": "não deve estar em branco"}]}
```

**Exemplo: cadastrar endereço**

```http
POST /api/v1/moradores/1/enderecos
Content-Type: application/json

{"logradouro": "Rua das Flores", "numero": "120", "semNumero": false, "bairro": "Centro",
 "municipio": "Cidade Teste", "uf": "SE", "cep": "49000000"}
```

```http
201 Created
Location: http://localhost:8080/api/v1/moradores/1/enderecos/1

{"id": 1, "moradorId": 1, "logradouro": "Rua das Flores", "numero": "120", "semNumero": false,
 "bairro": "Centro", "municipio": "Cidade Teste", "uf": "SE", "cep": "49000000", "complemento": null, "ativo": true}
```

**Exemplo: regra do número violada** (`semNumero` falso e `numero` ausente)

```http
400 Bad Request

{"codigo": "ENTRADA_INVALIDA", "mensagem": "Revise os campos informados.",
 "errosCampos": [{"campo": "numero", "mensagem": "é obrigatório quando semNumero é falso"}]}
```

**Exemplo: endereço de outro morador** (`PUT /moradores/2/enderecos/1`, sendo o endereço 1 do morador 1)

```http
404 Not Found

{"codigo": "REGISTRO_NAO_ENCONTRADO", "mensagem": "Endereço não encontrado(a): 1", "errosCampos": []}
```

**Exemplo: listar endereços** (`GET /moradores/1/enderecos?page=0&size=1`)

```http
200 OK

{"content": [{"id": 1, "moradorId": 1, "logradouro": "Rua das Flores", "numero": "200", "semNumero": false,
              "bairro": "Centro", "municipio": "Cidade Teste", "uf": "SE", "cep": "49000000",
              "complemento": "Apto 2", "ativo": true}],
 "page": 0, "size": 1, "totalElements": 2, "totalPages": 2}
```

## Consulta pública (API-08)

Todos os caminhos usam o prefixo `/api/v1`. Só leitura, sem dados de moradores ou operadores. Decisões em [docs/documentos-issues/issue-api-08](documentos-issues/issue-api-08/Registro_de_decisoes.md).

| Operação | Sucesso | Erros possíveis |
| --- | --- | --- |
| `GET /publico/pontos?nome=&municipio=&bairro=&materialId=&page=&size=` | `200`, `PaginaResponse<PontoPublicoResponse>` | `400` |
| `GET /publico/pontos/{id}` | `200`, `PontoPublicoDetalheResponse` | `404` (inexistente ou inativo) |
| `GET /publico/materiais?page=&size=` | `200`, `PaginaResponse<MaterialResponse>` | `400` |

- Só pontos ativos. Filtros opcionais e combináveis, sem diferenciar maiúsculas: `nome` contém o texto; `municipio` e `bairro` são iguais ao texto; `materialId` exige que o ponto aceite o material e que ele esteja ativo.
- Ordem por nome do ponto, com `id` como desempate. Um ponto com vários materiais aparece uma vez só (filtro por `EXISTS`, sem `JOIN`).
- `recolhimentoDomiciliar: false` identifica o ponto que não faz coleta em casa; a criação da solicitação continua barrando esse ponto (`PontoService.buscarDisponivelParaSolicitacao`).
- O detalhe traz os materiais ativos aceitos (mesmo formato de `GET /pontos/{id}/materiais`), ordenados por nome, sem paginação.

## Criação e correção de solicitação

Entrada: endereço do morador, ponto escolhido, observações e lista não vazia de `{materialId, quantidadeEstimada}`. O contexto de demonstração resolve o morador e a política definitiva sobre `moradorId`; não confiar em um ID enviado pelo navegador como prova de identidade.

Saída: protocolo único, situação `SOLICITADA`, data de criação, versão inicial, itens, cópias históricas e histórico inicial. Validar compatibilidade, situação, disponibilidade domiciliar, duplicidade, quantidades, limites e propriedade do endereço antes de confirmar a transação. Nenhum erro pode deixar cabeçalho, item ou histórico parcial.

`PUT /solicitacoes/{id}/dados` recebe versão esperada e permite corrigir endereço/itens apenas em `SOLICITADA`; refaz todas as validações e registra a correção de forma auditável. Alterar endereço cadastrado ou unidade do material não reescreve cópias históricas de solicitações existentes.

## Ações e versão

Todas as mutações de solicitação, inclusive correção e reagendamento, recebem a versão esperada, por exemplo em `versao` no corpo; `API-15` fecha e testa essa convenção com todos os módulos. `@Version` no banco sem comparar a versão do cliente não basta para detectar um cliente desatualizado. Uma escrita concorrente também deve ser capturada e convertida em 409, com rollback do histórico.

| Ação | Estado de origem | Dados/validações | Resultado |
| --- | --- | --- | --- |
| Agendar | SOLICITADA | data/hora futura, operador e versão | AGENDADA |
| Recusar | SOLICITADA | motivo obrigatório, operador e versão | RECUSADA |
| Cancelar | SOLICITADA ou AGENDADA | motivo, ator permitido no contexto e versão | CANCELADA |
| Iniciar | AGENDADA | operador e versão | EM_RECOLHIMENTO |
| Concluir | EM_RECOLHIMENTO | todos os itens com efetiva >= 0; algum > 0; peças integrais; data/responsável | CONCLUIDA |
| Reagendar | AGENDADA | justificativa, nova data futura e versão | AGENDADA |
| Tentativa sem sucesso | EM_RECOLHIMENTO | justificativa, nova data futura, operador e versão | AGENDADA |

Histórico e mudança devem confirmar juntos. CONCLUIDA, RECUSADA e CANCELADA são finais. Enum JSON sem acento é proposta; no PDF aparece “CONCLUÍDA”.

## Contexto e privacidade

No protótipo sem login, documentar a seleção de ator fictício em perfil local. O backend resolve tipo de ator e vínculos; contexto deve limitar consultas do morador às próprias solicitações e do operador aos próprios pontos. Seleção livre de ator continua sendo simulação, não autenticação ou proteção para produção.

Os endpoints públicos expõem pontos/materiais e orientações, sem nomes de moradores, endereços de atendimento, e-mails de moradores/operadores ou histórico pessoal. Dados de contato do ponto fazem parte da consulta do ponto. Se login for adotado futuramente, autenticar e autorizar de verdade cada operação restrita e testar cruzamento de pontos/usuários.
