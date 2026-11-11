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
| `ACESSO_NEGADO` | 403 | Ator de demonstração ausente, desconhecido, inativo ou sem permissão para a operação |
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
| Moradores | `POST /moradores`, `GET /moradores/{id}`, `PUT /moradores/{id}`, `PATCH /moradores/{id}/situacao` | API-04, API-14 |
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
- A API-14 altera a situação do morador preservando seus registros.

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

## Proteção de cadastros e vínculos (API-14)

`PATCH /api/v1/moradores/{id}/situacao` recebe `{"ativo":false}` para inativar e `{"ativo":true}` para reativar. O campo é obrigatório. O retorno é `200` com o morador atualizado. O contexto de demonstração rejeita moradores inativos em novas operações.

`PATCH /api/v1/pontos/{id}/situacao` bloqueia a inativação se o ponto tiver solicitações abertas. `DELETE /api/v1/pontos/{id}/materiais/{materialId}` bloqueia a remoção se uma solicitação aberta do ponto contiver aquele material. Estados abertos são `SOLICITADA`, `AGENDADA` e `EM_RECOLHIMENTO`. O bloqueio devolve `409 PONTO_COM_SOLICITACOES_ABERTAS` com o número de solicitações afetadas.

Estados finais deixam de bloquear a alteração. As chaves estrangeiras protegem os cadastros vinculados e os itens mantêm a unidade copiada. Endereço copiado e histórico continuam consultáveis. Criação e correção seguram uma trava compartilhada no ponto. Inativação e remoção usam a trava exclusiva na mesma linha antes de contar as solicitações afetadas.

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

## Operadores e contexto de demonstração (API-09)

Todos os caminhos usam o prefixo `/api/v1`. Decisões em [docs/documentos-issues/issue-api-09](documentos-issues/issue-api-09/Registro_de_decisoes.md).

**Seleção de ator (simulação, não autenticação).** O cliente informa o ator fictício em dois cabeçalhos:

| Cabeçalho | Valor |
| --- | --- |
| `X-Ator-Tipo` | `MORADOR`, `OPERADOR` ou `ADMINISTRADOR` (sem diferenciar maiúsculas) |
| `X-Ator-Id` | id do morador ou do operador; dispensado para `ADMINISTRADOR` |

O backend confere no banco se o ator existe e está ativo e carrega os pontos do operador. Ator ausente, de tipo inválido, inexistente ou inativo responde `403 ACESSO_NEGADO`. **Qualquer cliente pode enviar qualquer id: o id não prova identidade.** Não usar em produção; se houver login, a identidade passa a vir da autenticação.

| Operação | Quem pode | Sucesso | Erros possíveis |
| --- | --- | --- | --- |
| `GET /contexto` | qualquer ator válido | `200`, `ContextoResponse` (tipo, id, nome, pontoIds) | `403` |
| `POST /operadores` | administrador | `201` com `Location` | `400`, `403`, `409 DUPLICIDADE` |
| `GET /operadores?page=&size=` | administrador | `200`, `PaginaResponse<OperadorResponse>` | `400`, `403` |
| `GET /operadores/{id}` | administrador ou o próprio operador | `200`, com a lista `pontos` | `403`, `404` |
| `PUT /operadores/{id}` | administrador | `200` | `400`, `403`, `404`, `409 DUPLICIDADE` |
| `PUT /operadores/{id}/pontos/{pontoId}` | administrador | `201` ao criar, `200` se já existia | `403`, `404` |
| `DELETE /operadores/{id}/pontos/{pontoId}` | administrador | `204` | `403`, `404` |
| `GET /demo/atores` | só existe no perfil `demo` | `200`, lista de `ContextoResponse` | `404` fora do perfil |

- `nome` e `email` obrigatórios; o e-mail é normalizado (minúsculo, sem espaços) e único entre operadores.
- Repetir o vínculo nunca cria segunda linha. Operadores não são excluídos.
- As demais rotas (moradores, materiais, pontos, consulta pública) ainda não exigem os cabeçalhos. As issues de solicitação usam `ContextoService.exigirOperadorDoPonto` e `exigirMoradorDono` para limitar o escopo.

**Exemplo: operador do ponto A consultando outro operador**

```http
GET /api/v1/operadores/9
X-Ator-Tipo: OPERADOR
X-Ator-Id: 8

403 Forbidden

{"codigo": "ACESSO_NEGADO", "mensagem": "Operador só pode consultar os próprios dados.", "errosCampos": []}
```

## Criação e correção de solicitação (API-10)

Todos os caminhos usam o prefixo `/api/v1`. Só o morador cria e corrige: enviar `X-Ator-Tipo: MORADOR` e `X-Ator-Id` (ver [contexto de demonstração](#operadores-e-contexto-de-demonstração-api-09)). O morador vem do contexto; o corpo não tem `moradorId`. Decisões em [docs/documentos-issues/issue-api-10](documentos-issues/issue-api-10/Registro_de_decisoes.md).

| Operação | Sucesso | Erros possíveis |
| --- | --- | --- |
| `POST /solicitacoes` | `201` com `Location` e `SolicitacaoResponse` | `400`, `403`, `404`, `409` |
| `PUT /solicitacoes/{id}/dados` | `200` com `SolicitacaoResponse` | `400`, `403`, `404`, `409` |

**Entrada da criação:** `enderecoId`, `pontoId`, `observacoes` (opcional) e `itens` não vazio de `{materialId, quantidadeEstimada}`. **Entrada da correção:** `versao` (a que o cliente leu), `enderecoId` e `itens` (a lista substitui todos os itens). O ponto e as observações não mudam na correção.

**Saída (`SolicitacaoResponse`):** `id`, `protocolo` (`CS-AAAAMMDD-XXXXXX`, único), `situacao`, `versao`, `criadaEm`, `agendadaPara`, `concluidaEm`, `moradorId`, `pontoId`, `endereco` (cópia), `observacoes`, `itens` (com `unidade` copiada e `quantidadeEfetiva` nula até a conclusão) e `historico` em ordem cronológica. Datas saem em UTC (`...Z`).

**Validações (criação e correção).** Qualquer erro não grava nada.

| Situação | Resposta |
| --- | --- |
| Ator ausente, desconhecido ou que não é morador | `403 ACESSO_NEGADO` |
| Campo obrigatório ausente, `itens` vazio | `400 ENTRADA_INVALIDA` apontando o campo |
| Solicitação inexistente ou de outro morador (correção) | `404 REGISTRO_NAO_ENCONTRADO` |
| `versao` diferente da gravada (correção) | `409 CONFLITO_VERSAO` |
| Situação diferente de `SOLICITADA` (correção) | `409 TRANSICAO_INVALIDA` |
| Endereço inexistente ou de outro morador | `404 REGISTRO_NAO_ENCONTRADO` |
| Endereço inativo; ponto inativo ou sem recolhimento domiciliar | `409 REGRA_NEGOCIO` |
| Ponto ou material inexistente | `404 REGISTRO_NAO_ENCONTRADO` |
| Material repetido na lista | `400`, campo `itens[i].materialId` |
| Quantidade zero, negativa, com mais de 3 casas ou fracionária para `UNIDADE` | `400`, campo `itens[i].quantidadeEstimada` |
| Material inativo, não aceito pelo ponto ou acima do limite | `409 MATERIAL_INCOMPATIVEL` |

**Cópias (RN04).** A solicitação guarda a cópia do endereço e cada item guarda a cópia da unidade. Alterar o cadastro do endereço ou a unidade do material depois não muda solicitações existentes.

**Histórico.** A criação grava uma linha (`situacaoAnterior` nula, `situacaoNova` `SOLICITADA`). Cada correção grava uma linha `SOLICITADA` para `SOLICITADA` cujo `motivo` guarda o endereço e os itens **anteriores**. Responsável: `MORADOR` e o id dele.

**Versão.** Começa em `0` e sobe 1 a cada correção. O cliente deve reenviar a `versao` da última resposta. Duas correções simultâneas com a mesma versão: uma confirma, a outra recebe `409 CONFLITO_VERSAO`.

**Travas (combinado com a API-14).** Criar e corrigir travam a linha do ponto em modo compartilhado (`FOR SHARE`). Inativar o ponto ou remover um material aceito deve travar a mesma linha com `FOR UPDATE` **antes** de conferir as solicitações abertas. A correção trava antes a linha da solicitação (`FOR UPDATE`); a ordem é sempre solicitação e depois ponto.

**Exemplo: criar**

```http
POST /api/v1/solicitacoes
X-Ator-Tipo: MORADOR
X-Ator-Id: 85

{"enderecoId": 76, "pontoId": 103, "observacoes": "Portão azul",
 "itens": [{"materialId": 171, "quantidadeEstimada": 2.5}, {"materialId": 172, "quantidadeEstimada": 12}]}
```

```http
201 Created
Location: /api/v1/solicitacoes/29

{"id": 29, "protocolo": "CS-20261111-W9M78B", "situacao": "SOLICITADA", "versao": 0,
 "criadaEm": "2026-11-11T18:44:19.095Z", "moradorId": 85, "pontoId": 103,
 "endereco": {"enderecoId": 76, "logradouro": "Rua das Flores", "numero": "200", "semNumero": false,
              "bairro": "Centro", "municipio": "Cidade Demonstração", "uf": "SE", "cep": "49000000",
              "complemento": null},
 "observacoes": "Portão azul",
 "itens": [{"materialId": 172, "nomeMaterial": "Garrafa PET", "unidade": "UNIDADE", "quantidadeEstimada": 12,
            "quantidadeEfetiva": null},
           {"materialId": 171, "nomeMaterial": "Vidro", "unidade": "KG", "quantidadeEstimada": 2.5,
            "quantidadeEfetiva": null}],
 "historico": [{"situacaoAnterior": null, "situacaoNova": "SOLICITADA", "ocorridoEm": "2026-11-11T18:44:19.095Z",
                "responsavelTipo": "MORADOR", "responsavelId": 85, "motivo": null}]}
```

**Exemplo: material não aceito pelo ponto**

```http
409 Conflict

{"codigo": "MATERIAL_INCOMPATIVEL", "mensagem": "O ponto não aceita o material Papel.", "errosCampos": []}
```

## Consulta de solicitações (API-11)

Todos os caminhos usam o prefixo `/api/v1` e exigem um ator do [contexto de demonstração](#operadores-e-contexto-de-demonstração-api-09). Decisões em [docs/documentos-issues/issue-api-11](documentos-issues/issue-api-11/Registro_de_decisoes.md).

| Operação | Sucesso | Erros possíveis |
| --- | --- | --- |
| `GET /solicitacoes?inicio=&fim=&situacao=&pontoId=&page=&size=` | `200`, `PaginaResponse<SolicitacaoResumoResponse>` | `400`, `403` |
| `GET /solicitacoes/{id}` | `200`, `SolicitacaoResponse` (o mesmo da criação) | `403`, `404` |
| `GET /solicitacoes/{id}/historico` | `200`, lista de linhas do histórico | `403`, `404` |

**Contexto (RF09).** O backend decide o que cada ator vê; os filtros nunca ampliam esse limite.

| Ator | Vê |
| --- | --- |
| `MORADOR` | só as próprias solicitações |
| `OPERADOR` | só as solicitações dos pontos aos quais está vinculado |
| `ADMINISTRADOR` | todas |

Detalhe ou histórico fora do contexto responde `404 REGISTRO_NAO_ENCONTRADO`, igual a um id que não existe: a resposta não revela nada da solicitação. Na lista, o que está fora do contexto não aparece (operador filtrando por ponto que não é dele recebe lista vazia). Ator ausente ou desconhecido responde `403 ACESSO_NEGADO`.

**Filtros da lista**, todos opcionais e combináveis:

- `inicio` e `fim`: período pela **data de criação**, com início **inclusivo** e fim **exclusivo**, em ISO 8601 **com offset obrigatório** (`2026-11-01T00:00:00Z` ou `2026-11-01T00:00:00-03:00`). Sem offset, ou com `inicio` igual ou posterior a `fim`: `400` apontando o campo. Na URL, o sinal `+` de um offset positivo deve ser codificado como `%2B`.
- `situacao`: um dos seis valores (`SOLICITADA`, `AGENDADA`, `RECUSADA`, `CANCELADA`, `EM_RECOLHIMENTO`, `CONCLUIDA`). Valor desconhecido: `400`.
- `pontoId`: solicitações daquele ponto.

**Ordem e paginação.** Da mais recente para a mais antiga pela data de criação, com o `id` como desempate; `size` de 1 a 100.

**Conteúdo.** A lista traz `id`, `protocolo`, `situacao`, `criadaEm`, `agendadaPara`, `concluidaEm`, `moradorId`, `pontoId` e `nomePonto`. O detalhe traz também `versao`, o endereço e as unidades **copiados** (RN04), `observacoes`, os itens e o histórico. As respostas não trazem nome, e-mail nem telefone do morador. Datas em UTC.

**Histórico.** Em ordem cronológica, com o `id` como desempate. Cada linha: `situacaoAnterior` (nula na criação), `situacaoNova`, `ocorridoEm`, `responsavelTipo` (`MORADOR`, `OPERADOR` ou `ADMIN`), `responsavelId`, `motivo` e `agendamentoNovo`. Não é paginado.

**Exemplo: operador do ponto B pedindo o detalhe de uma solicitação do ponto A**

```http
GET /api/v1/solicitacoes/39
X-Ator-Tipo: OPERADOR
X-Ator-Id: 28

404 Not Found

{"codigo": "REGISTRO_NAO_ENCONTRADO", "mensagem": "Solicitação não encontrado(a): 39", "errosCampos": []}
```

## Ações e versão

Envie a `versao` da última resposta ao alterar uma solicitação existente. O campo é um inteiro JSON não negativo. Valor ausente ou inválido retorna `400 ENTRADA_INVALIDA`. Versão antiga ou disputa entre duas escritas retorna `409 CONFLITO_VERSAO`. Após o conflito, consulte `GET /api/v1/solicitacoes/{id}` para obter os dados e a versão atuais.

A API-15 mantém `@Version` para alterações na linha da solicitação e compara a versão no momento de gravar a correção que só troca itens. A transação vencedora incrementa a versão e grava a mudança com seu histórico. Na API-13, início, conclusão, reagendamento e tentativa sem sucesso devem seguir a mesma política, inclusive quando a ação gerar histórico sem mudar a situação.

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

### Agendamento, recusa e cancelamento (API-12)

As três ações usam `POST /api/v1/solicitacoes/{id}/<ação>`, recebem a versão inteira não negativa da última resposta e devolvem `200` com a solicitação atualizada, a nova versão e o histórico.

| Ação | Ator | Exemplo de corpo |
| --- | --- | --- |
| `agendamento` | Operador vinculado ao ponto | `{"versao":0,"agendadaPara":"2026-12-01T14:30:00-03:00"}` |
| `recusa` | Operador vinculado ao ponto | `{"versao":0,"motivo":"Sem equipe disponível"}` |
| `cancelamento` | Morador dono ou operador vinculado ao ponto | `{"versao":1,"motivo":"Solicitação duplicada"}` |

A data de agendamento deve ter offset e representar um instante posterior ao atual. O histórico guarda o novo agendamento em `agendamentoNovo`. A recusa e o cancelamento exigem motivo com texto. Campos inválidos retornam `400`, tipo de ator sem permissão retorna `403` e solicitação inexistente ou fora do contexto retorna `404`. A versão antiga retorna `409 CONFLITO_VERSAO` e a transição proibida retorna `409 TRANSICAO_INVALIDA`.

## Contexto e privacidade

No protótipo sem login, documentar a seleção de ator fictício em perfil local. O backend resolve tipo de ator e vínculos; contexto deve limitar consultas do morador às próprias solicitações e do operador aos próprios pontos. Seleção livre de ator continua sendo simulação, não autenticação ou proteção para produção.

Os endpoints públicos expõem pontos/materiais e orientações, sem nomes de moradores, endereços de atendimento, e-mails de moradores/operadores ou histórico pessoal. Dados de contato do ponto fazem parte da consulta do ponto. Se login for adotado futuramente, autenticar e autorizar de verdade cada operação restrita e testar cruzamento de pontos/usuários.
