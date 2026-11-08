# Registro de decisões - Moradores e endereços (issue #7, API-04)

Registro das decisões da implementação de moradores e endereços (issue #7, API-04), com justificativa e consequência de cada uma. Modelo de dados: [DER.md](../../DER.md). Migrações: [Registro da API-02](../issue-api-02/Registro_de_decisoes.md).

**Base:** `TrabalhoWeb01.pdf`, o DER e o contrato da API. Cada decisão indica se veio do PDF ou se foi escolha do autor.

Legenda:
- **PDF**: exigido pelo enunciado.
- **Necessário**: o PDF não cita, mas sem isso uma exigência do PDF não funciona.
- **Escolha**: o PDF deixa em aberto; foi escolhido entre opções.

---

## 1. Decisões tomadas

### 1.1 Entidades com getters e setters escritos, sem Lombok
- **Tipo:** Escolha.
- **O que o PDF diz:** nada sobre estilo de código.
- **Opções:** (a) getters e setters escritos (ou gerados pelo IntelliJ); (b) Lombok (`@Getter` e `@Setter`).
- **Escolhido:** (a).
- **Por quê:** o Lombok está no `pom.xml`, mas nenhum arquivo do projeto o usa. Escrever os acessores deixa cada linha visível, o que ajuda a explicar o código.
- **Consequência:** as entidades ficam com mais linhas. Se a equipe adotar o Lombok depois, a troca é mecânica.

### 1.2 `uf` e `cep` mapeados como `CHAR` no Hibernate
- **Tipo:** Necessário.
- **O que o PDF diz:** RF02 exige UF e CEP no endereço. O DER define `CHAR(2)` e `CHAR(8)`.
- **Problema encontrado:** com `ddl-auto=validate`, o Hibernate recusou subir: `wrong column type encountered in column [cep] in table [endereco]; found [bpchar (Types#CHAR)], but expecting [varchar(8) (Types#VARCHAR)]`.
- **Opções:** (a) `@JdbcTypeCode(SqlTypes.CHAR)` nos dois campos; (b) nova migração trocando as colunas para `VARCHAR`; (c) desligar o `validate`.
- **Escolhido:** (a).
- **Por quê:** mantém o banco como o DER define e as migrações publicadas intactas. (b) mudaria o modelo aprovado, e (c) tiraria a garantia de que as entidades batem com o banco.
- **Consequência:** a anotação é específica do Hibernate. Qualquer novo campo `CHAR` precisa da mesma anotação.

### 1.3 Um único `MoradorRequest` para cadastrar e atualizar
- **Tipo:** Escolha.
- **O que o PDF diz:** RF01 pede criar, consultar e atualizar morador, com os mesmos campos.
- **Opções:** (a) um `MoradorRequest` para `POST` e `PUT`; (b) dois DTOs separados.
- **Escolhido:** (a).
- **Por quê:** os campos e as validações são idênticos, e dois DTOs repetiriam o código.
- **Consequência:** se algum dia o `PUT` precisar de um campo a mais, o DTO deverá ser dividido.

### 1.4 Formato de UF e CEP na entrada
- **Tipo:** Escolha.
- **O que o PDF diz:** RF02 exige UF e CEP. O contrato diz "CEP como texto".
- **Escolhido:** UF com 2 letras (o Service converte para maiúsculas) e CEP com 8 dígitos, sem traço.
- **Por quê:** o banco guarda `CHAR(2)` e `CHAR(8)`, e uma validação clara evita erro de banco.
- **Consequência:** `49000-000`, com traço, é rejeitado com `400`. Aceitar o traço exigiria normalizar no Service.

### 1.5 Listagem de endereços paginada
- **Tipo:** Necessário (convenção do contrato).
- **O que o contrato diz:** toda listagem usa `PaginaResponse<T>`, com `PaginacaoRequest`.
- **Escolhido:** `Page<EnderecoModel> findByMoradorId(Long, Pageable)` no repository.
- **Por quê:** segue a convenção comum a todos os módulos, mesmo que um morador tenha poucos endereços.
- **Consequência:** `GET /moradores/{id}/enderecos` aceita `page` e `size`.

### 1.6 Regra "número ou sem número" estrita, aplicada no Service
- **Tipo:** Escolha.
- **O que o PDF diz:** RF02 pede "número ou indicação de sem número". A issue exige uma regra inequívoca e documentada.
- **O que o banco garante:** só `CHECK (sem_numero OR numero IS NOT NULL)`, que ainda aceita os dois juntos.
- **Opções:** (a) estrita: `semNumero = true` exige `numero` vazio, e `semNumero = false` exige `numero` preenchido; (b) permissiva, como o banco; (c) estrita, validada no DTO com `@AssertTrue`.
- **Escolhido:** (a), no Service (`EnderecoService`, fase 4).
- **Por quê:** existe uma só interpretação para cada endereço. No Service, a `EntradaInvalidaException(campo, mensagem)` devolve `400` apontando o campo `numero`. No DTO, o erro apareceria com um nome de campo que não existe no JSON. O guia também pede regras no Service.
- **Consequência:** o banco aceita mais do que a API aceita. A regra precisa de teste no Service.

### 1.7 Normalização do e-mail no construtor do `MoradorRequest`
- **Tipo:** Escolha (ajusta o que o registro da API-02 dizia).
- **O que o PDF diz:** RF01 exige e-mail único após normalização. O critério da issue pede que e-mail repetido com caixa ou espaços diferentes gere conflito (`409`).
- **Problema encontrado:** o `@Email` rejeita espaços nas pontas. Se a normalização ficasse só no Service, `" Maria@Exemplo.com "` seria recusado com `400` antes de o Service rodar, e nunca chegaria ao `409`.
- **Opções:** (a) normalizar no construtor compacto do `MoradorRequest` (`trim` e minúsculas), antes da validação; (b) normalizar no Service e tirar o `@Email` do DTO; (c) manter só no Service e aceitar o `400`.
- **Escolhido:** (a).
- **Por quê:** o Bean Validation passa a validar o valor já normalizado, e o conflito de unicidade ocorre sobre o mesmo valor que é gravado. Mantém o `@Email` no DTO.
- **Consequência:** o `MoradorService` recebe o e-mail pronto e não normaliza de novo. A decisão 1.2 da API-02 ("a normalização é do Service") fica ajustada: ela é feita ao criar o `MoradorRequest`.

### 1.8 Unicidade do e-mail: checagem no Service e tradução do erro do banco
- **Tipo:** Necessário (a issue pede que a unicidade exista também no banco).
- **Escolhido:** o Service consulta antes (`existsByEmail` ao cadastrar, `findByEmail` ao atualizar, ignorando o próprio morador) e, na gravação (`saveAndFlush`), converte `DataIntegrityViolationException` em `DUPLICIDADE` (`409`).
- **Por quê:** a consulta dá a resposta normal. Se dois cadastros iguais chegarem ao mesmo tempo, o `UNIQUE` do banco barra o segundo, e a tradução evita devolver o erro genérico `REGRA_NEGOCIO`.
- **Consequência:** o `saveAndFlush` força a gravação dentro do método, para o erro aparecer ali e não só no fim da transação.

### 1.9 Endereço de outro morador responde `404`
- **Tipo:** Escolha.
- **O que o PDF diz:** RF02 exige rejeitar o uso de endereço de outro morador. O contrato deixa em aberto se a resposta é `404` ou `409`.
- **Opções:** (a) `404 REGISTRO_NAO_ENCONTRADO`; (b) `409 REGRA_NEGOCIO`.
- **Escolhido:** (a). O Service busca o endereço por `id` **e** dono (`findByIdAndMoradorId`); se não achar, responde como se o endereço não existisse.
- **Por quê:** não revela a quem consulta que o endereço existe e pertence a outra pessoa. O caminho `/moradores/{id}/enderecos/{enderecoId}` identifica o endereço dentro daquele morador, então um endereço de outro morador simplesmente não está ali.
- **Consequência:** o cliente não distingue "não existe" de "é de outro". Vale também para o uso do endereço numa solicitação (API-10).

### 1.10 Ordem da listagem de endereços e normalização dos campos
- **Tipo:** Escolha.
- **Ordem:** por `id` crescente (ordem de cadastro). O `PaginacaoService` já acrescenta o `id` como desempate, então o Service não pede outra ordenação.
- **Normalização ao gravar:** `uf` em maiúsculas; `numero` sem espaços nas pontas e `null` quando `semNumero` é verdadeiro.
- **Por quê:** uma ordem fixa e previsível, como o contrato manda, e dados gravados sempre no mesmo formato.
- **Consequência:** o cliente não escolhe a ordenação nesta versão.

### 1.11 `semNumero` ausente no JSON vale falso
- **Tipo:** Necessário (defeito encontrado pelo teste do Controller).
- **Problema:** com `boolean semNumero` no `EnderecoRequest`, o Jackson 3 (Spring Boot 4) recusa o JSON que omite o campo, e a API responde `400 "Corpo da requisição ausente ou malformado"` com `errosCampos` vazio, sem dizer o que está errado. O contrato diz que campos opcionais podem ser omitidos.
- **Opções:** (a) `Boolean` no record, com valor `false` no construtor quando vier nulo; (b) manter `boolean` e exigir o campo; (c) configurar o Jackson para aceitar primitivos ausentes.
- **Escolhido:** (a).
- **Por quê:** é uma mudança local e não altera o comportamento global do Jackson para os outros módulos (c). Exigir o campo (b) obrigaria todo cliente a enviar `semNumero: false`.
- **Consequência:** o `semNumero` explícito `null` também vira `false`. Vale para qualquer campo booleano opcional que a equipe criar: usar `Boolean` com valor padrão.

### 1.12 Local de criação do endereço (`Location`)
- **Tipo:** Escolha.
- **O que o contrato diz:** a criação devolve `201` com `Location`. O contrato só define `POST`, `GET` (lista) e `PUT` para endereços, sem `GET` de um endereço isolado.
- **Escolhido:** `Location` aponta para `/api/v1/moradores/{moradorId}/enderecos/{enderecoId}`, o mesmo caminho do `PUT`.
- **Por quê:** o caminho identifica o recurso criado, e é o que o `PUT` usa para atualizar.
- **Consequência:** um `GET` nesse caminho responde `405` até a equipe decidir se cria a consulta individual.

---

## 2. Validação

**Ambiente:** PostgreSQL 17 descartável (container `postgres:17`, sem volume, porta 5435, 512 MB, senha gerada na hora e descartada), Spring Boot 4.1.2. O JDK usado na execução foi o 25.0.1, e o projeto compila para o Java 21. Nenhum volume nem dado do usuário foi tocado, e o container foi parado ao fim.

**Comandos:**
1. Banco: `docker run --rm -d --name coleta-teste-pg -p 127.0.0.1:5435:5432 --memory 512m --tmpfs /var/lib/postgresql/data -e POSTGRES_DB=coleta_teste -e POSTGRES_USER=coleta_teste -e POSTGRES_PASSWORD=<senha descartável> postgres:17`.
2. Testes: `./mvnw test`, com `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD` apontando para o banco.
3. API: `./mvnw spring-boot:run` com as mesmas variáveis, e chamadas com `curl` em `http://localhost:8080`. O Flyway aplicou `V1` a `V6` do zero (`Successfully applied 6 migrations`).

**Testes automatizados:** `./mvnw test` terminou com 51 testes, 0 falhas e `BUILD SUCCESS`. Desses, 21 são desta issue, enxutos de propósito (um teste por regra, sem repetir a mesma regra em várias camadas):
- `MoradorServiceTest` (4): cadastro válido, e-mail repetido com caixa e espaços, atualizar sem trocar o e-mail, atualizar com o e-mail de outro morador.
- `EnderecoServiceTest` (4): UF em maiúsculas, regra do número, listagem em página, endereço de outro morador.
- `MoradorRequestTest` (3): cadastro válido, nome em branco e e-mail inválido, normalização do e-mail.
- `MoradorControllerTest` (3) e `EnderecoControllerTest` (3): `201` com `Location`, `400` por campo, `409` e a listagem paginada.
- `MoradorEnderecoIntegracaoTest` (4, contra o banco real): cadastro persiste e é consultado, e-mail repetido com caixa e espaços conflita, o banco rejeita e-mail repetido sem passar pelo Service, endereço de outro morador não pode ser atualizado.

Os demais casos (404, paginação fora do intervalo, regra do número, endereço incompleto e outros) ficam provados pelas 24 chamadas reais abaixo.

**Chamadas reais com `curl` (API rodando):**

| # | Chamada | Esperado | Obtido |
| --- | --- | --- | --- |
| 01 | `POST /moradores` válido | `201` com `Location` | `201`, `Location: .../moradores/1` |
| 02 | `POST /moradores` com `"  MARIA@Exemplo.com "` | `409` | `409 DUPLICIDADE` |
| 03 | `POST /moradores` sem nome | `400` no campo `nome` | `400`, campo `nome` |
| 04 | `POST /moradores` com e-mail inválido | `400` no campo `email` | `400`, campo `email` |
| 05 | `GET /moradores/1` | `200` | `200` |
| 06 | `GET /moradores/999` | `404` | `404 REGISTRO_NAO_ENCONTRADO` |
| 07 | `GET /moradores/abc` | `400` | `400`, campo `id` |
| 08 | `PUT /moradores/1` (telefone novo) | `200` | `200` |
| 09 | `POST /moradores` (segundo morador) | `201` | `201`, id 2 |
| 10 | `PUT /moradores/2` com o e-mail do morador 1 | `409` | `409 DUPLICIDADE` |
| 11 | `POST /moradores/1/enderecos` válido | `201` com `Location` | `201`, `Location: .../enderecos/1` |
| 12 | endereço sem número e `semNumero` falso | `400` no campo `numero` | `400`, campo `numero` |
| 13 | endereço com número e `semNumero` verdadeiro | `400` no campo `numero` | `400`, campo `numero` |
| 14 | endereço incompleto (sem logradouro e município, UF e CEP inválidos) | `400` com os 4 campos | `400`, campos `uf`, `cep`, `municipio` e `logradouro` |
| 15 | endereço sem número, `semNumero` verdadeiro, UF `se` | `201`, `numero` nulo, UF `SE` | `201`, `numero: null`, `uf: "SE"` |
| 16 | endereço para morador 999 | `404` | `404` |
| 17 | `PUT /moradores/2/enderecos/1` (endereço do morador 1) | `404` | `404 REGISTRO_NAO_ENCONTRADO` |
| 18 | `PUT /moradores/1/enderecos/1` (próprio endereço) | `200` | `200`, número e complemento atualizados |
| 19 | `GET /moradores/1/enderecos?page=0&size=1` | página com 1 de 2 | `totalElements: 2`, `totalPages: 2` |
| 20 | `GET ...?page=5&size=1` | `200` com `content` vazio | `200`, `content: []` |
| 21 | `GET ...?page=abc` | `400` no campo `page` | `400`, campo `page` |
| 22 | `GET ...?size=0` | `400` no campo `size` | `400`, campo `size` |
| 23 | `GET /moradores/2/enderecos` | lista vazia | `200`, `totalElements: 0` |
| 24 | `GET /moradores/999/enderecos` | `404` | `404` |

O OpenAPI (`/v3/api-docs`) lista os quatro caminhos: `/api/v1/moradores`, `/api/v1/moradores/{id}`, `/api/v1/moradores/{moradorId}/enderecos` e `/api/v1/moradores/{moradorId}/enderecos/{enderecoId}`.

---

## 3. Pendentes

- Decidir com a equipe se existe `GET /moradores/{id}/enderecos/{enderecoId}` (hoje o `Location` do `POST` aponta para um caminho que só aceita `PUT`).
- Coordenar com a API-14 (inativação preservando referências) e com o Genisson.
- Revisão do Herbert sobre as decisões 1.7 (normalização do e-mail no DTO) e 1.9 (endereço de outro morador responde `404`).
