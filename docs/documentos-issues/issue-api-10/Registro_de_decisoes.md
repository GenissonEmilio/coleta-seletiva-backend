# Registro de decisões - Criar e corrigir solicitação com itens e histórico (issue #13, API-10)

Registro das decisões da criação e da correção de solicitações (issue #13, API-10), com justificativa e consequência de cada uma. Modelo de dados: [DER.md](../../DER.md). Contrato: [CONTRATO_API.md](../../CONTRATO_API.md#criação-e-correção-de-solicitação-api-10). Padrão seguido: [Registro da API-07](../issue-api-07/Registro_de_decisoes.md) (chave composta) e [Registro da API-09](../issue-api-09/Registro_de_decisoes.md) (contexto do ator).

**Base:** `TrabalhoWeb01.pdf` (RF08, RN01 a RN04, RN06, RNF05 e RNF08), o DER (tabelas da migração V5) e o contrato da API. Cada decisão indica se veio do PDF ou se foi escolha do autor.

Legenda:
- **PDF**: exigido pelo enunciado.
- **Necessário**: o PDF não cita, mas sem isso uma exigência do PDF não funciona.
- **Escolha**: o PDF deixa em aberto; foi escolhido entre opções.

---

## 0. O que foi feito

| Arquivo | Papel |
| --- | --- |
| `model/SolicitacaoModel.java` | Novo. Tabela `solicitacao`, com a cópia do endereço e `@Version` |
| `model/ItemSolicitacaoModel.java`, `ItemSolicitacaoId.java` | Novos. Tabela `item_solicitacao` (chave composta) com a cópia da unidade |
| `model/HistoricoSolicitacaoModel.java` | Novo. Tabela `historico_solicitacao` |
| `model/SituacaoSolicitacao.java`, `ResponsavelTipo.java` | Novos. Enums iguais aos `CHECK` do banco |
| `repository/SolicitacaoRepository.java`, `HistoricoSolicitacaoRepository.java` | Novos |
| `dto/solicitacao/` (4 arquivos) | Novos. Entrada da criação, do item e da correção; saída completa |
| `services/solicitacao/SolicitacaoService.java` | Novo. Todas as regras, em uma transação por operação |
| `controller/SolicitacaoController.java` | Novo. `POST /solicitacoes` e `PUT /solicitacoes/{id}/dados` |
| `repository/PontoRepository.java` | Método acrescentado: `travarParaSolicitacao` |
| `services/contexto/ContextoService.java` | Método acrescentado: `exigirMorador` |
| `test/.../SolicitacaoIntegracaoTest.java` (13) e `SolicitacaoTransacaoIntegracaoTest.java` (5) | Novos testes |
| `docs/CONTRATO_API.md` | Seção "Criação e correção de solicitação" reescrita com o que foi implementado |

Removidas as três classes vazias `TesteSolicitacaoRequest`, `TesteSolicitacaoResponse` e `TesteSolicitacaoService`. Nenhuma migração nova: as tabelas já existiam na V5. Reaproveitados sem alteração: `PontoService.buscarDisponivelParaSolicitacao` (API-06), `MaterialService.buscarAtivoParaSolicitacao` e `QuantidadeMaterialService.validarPositiva` (API-05).

---

## 1. Decisões tomadas

### 1.1 Só o morador do contexto cria e corrige; o corpo não tem `moradorId`
- **Tipo:** PDF (seção 3: "Morador: solicitar recolhimento") + contrato ("o contexto de demonstração resolve o morador").
- **Escolhido:** o controller resolve o ator pelos cabeçalhos da API-09 e exige `MORADOR` (`ContextoService.exigirMorador`). O `moradorId` gravado é o do contexto.
- **Consequência:** operador, administrador ou ator ausente recebem `403 ACESSO_NEGADO`. Continua sendo simulação, não autenticação (decisão 1.5 da API-09).

### 1.2 Validar tudo antes de gravar; uma transação por operação
- **Tipo:** PDF (RN06: "criação com itens e mudanças de situação com histórico devem ocorrer em uma única transação").
- **Escolhido:** `criar` e `corrigir` são `@Transactional`. Primeiro conferem endereço, ponto e todos os itens; só depois gravam solicitação, itens e histórico. Qualquer exceção desfaz tudo.
- **Por quê:** validar antes evita gravar para depois desfazer; a transação cobre o que a validação não prevê (falha de banco no meio).
- **Consequência:** provado com transações reais, sem a proteção do teste (`SolicitacaoTransacaoIntegracaoTest`): item incompatível e falha simulada ao gravar o histórico deixam 0 linhas.

### 1.3 Regras da RN01 e da RN02 e seus erros
- **Tipo:** PDF (RN01, RN02) + tabela de erros do contrato.
- **Escolhido:**

| Regra | Como é conferida | Erro |
| --- | --- | --- |
| Ponto ativo e com recolhimento domiciliar | `PontoService.buscarDisponivelParaSolicitacao` | `409 REGRA_NEGOCIO` |
| Material ativo | `MaterialService.buscarAtivoParaSolicitacao` | `409 MATERIAL_INCOMPATIVEL` |
| Material aceito pelo ponto | existe linha em `ponto_material` | `409 MATERIAL_INCOMPATIVEL` |
| Limite respeitado | estimada ≤ `limite_por_solicitacao` (igual ao limite passa) | `409 MATERIAL_INCOMPATIVEL` |
| Material sem repetição | conjunto de ids já vistos | `400`, campo `itens[i].materialId` |
| Estimada positiva; peça inteira; até 3 casas | `QuantidadeMaterialService.validarPositiva` | `400`, campo `itens[i].quantidadeEstimada` |
| Endereço do próprio morador | `findByIdAndMoradorId` | `404` (como na API-04) |

- **Por quê repetição é `400` e não `409`:** é um erro do próprio corpo enviado, e o `400` aponta o item exato para o front marcar.
- **Consequência:** endereço inativo também é rejeitado (`409 REGRA_NEGOCIO`). O banco repete as proteções (chave primária do item, `CHECK` de quantidade e de peça inteira).

### 1.4 Protocolo `CS-AAAAMMDD-XXXXXX`
- **Tipo:** Escolha (o PDF só exige que seja único; estava pendente no DER), decidida pelo responsável da issue.
- **Opções:** (a) data + 6 caracteres sorteados; (b) ano + número sequencial (`CS-2026-000042`).
- **Escolhido:** (a). 18 caracteres (a coluna aceita 20). O código usa letras e números sem os que se confundem (`O`/`0`, `I`/`1`, `L`).
- **Por quê:** (b) pediria uma sequência nova no banco (migração V8) e revelaria quantas solicitações existem.
- **Consequência:** são cerca de 887 milhões de códigos por dia (31 caracteres em 6 posições). O serviço confere se o protocolo já existe e sorteia outro; o `UNIQUE` do banco é a garantia final. A data do protocolo é a data local do servidor.

### 1.5 Cópias do endereço e da unidade (RN04)
- **Tipo:** PDF (RN04).
- **Escolhido:** `SolicitacaoModel.copiarEndereco` copia os 8 campos e guarda o `endereco_id` de origem. O construtor do `ItemSolicitacaoModel` copia a unidade do material.
- **Consequência:** alterar o endereço cadastrado ou a unidade do material não muda a solicitação (teste `alterarCadastroDeEnderecoOuUnidadeNaoMudaAsCopias`). A resposta mostra sempre a cópia, não o cadastro.

### 1.6 Itens: salvar a solicitação primeiro, depois os itens
- **Tipo:** Necessário.
- **Situação:** a chave do item é `(solicitacao_id, material_id)`, e o id da solicitação só existe depois do `INSERT`.
- **Escolhido:** `saveAndFlush` da solicitação, depois os itens são adicionados à lista (`cascade = ALL`, `orphanRemoval = true`), tudo na mesma transação.
- **Consequência:** na correção, a lista é esvaziada e gravada (`flush`) antes de receber os itens novos; sem isso, manter um mesmo material tentaria inserir a chave antes de apagar a antiga.

### 1.7 Correção só em `SOLICITADA`, com a lista completa de itens
- **Tipo:** PDF (RN04: "itens e endereço podem ser corrigidos apenas antes do agendamento, com nova validação").
- **Escolhido:** `PUT /solicitacoes/{id}/dados` recebe `versao`, `enderecoId` e `itens`. A lista substitui todos os itens. Refaz todas as validações da criação, inclusive ponto ainda ativo e com domiciliar. Fora de `SOLICITADA`: `409 TRANSICAO_INVALIDA`.
- **Consequência:** ponto e observações não mudam na correção (o PDF só fala de itens e endereço). Para trocar de ponto, o morador cancela (API-12) e cria outra.

### 1.8 Auditoria da correção numa linha do histórico
- **Tipo:** Escolha, decidida pelo responsável da issue.
- **Situação:** `historico_solicitacao` não tem coluna de "tipo de evento".
- **Opções:** (a) linha `SOLICITADA` → `SOLICITADA` com os dados anteriores no `motivo`; (b) migração V8 com coluna `evento`.
- **Escolhido:** (a). Exemplo real do `motivo`: "Correção de dados. Endereço anterior: Rua das Flores, 200, Centro, Cidade Demonstração/SE, CEP 49000000. Itens anteriores: Garrafa PET 12 UNIDADE; Vidro 2.5 KG."
- **Por quê:** não muda o DER nem afeta as issues #15 e #16, que também escrevem no histórico. O valor antigo fica guardado, e o novo está na própria solicitação.
- **Consequência:** uma correção se reconhece por situação anterior igual à nova. O texto é para leitura humana, não para o sistema interpretar.

### 1.9 Versão: comparar a do cliente e sempre subir na correção
- **Tipo:** PDF (RN06) + coordenação com a #18 (API-15).
- **Escolhido:** `@Version` em `versao`. Na correção, a versão enviada é comparada com a gravada; diferente responde `409 CONFLITO_VERSAO`. Ausente responde `400`.
- **Problema encontrado:** trocar só os itens não altera a linha da solicitação, então o Hibernate não subia a versão. O incremento forçado do Hibernate (`PESSIMISTIC_FORCE_INCREMENT`) não teve efeito no teste.
- **Solução:** `garantirNovaVersao`: se depois de gravar a versão continua a mesma, um `UPDATE` soma 1 e a solicitação é relida. A linha já está travada pela própria transação.
- **Consequência:** toda correção devolve versão nova (0 → 1 → 2). A #18 deve aplicar a mesma comparação às outras ações.

### 1.10 Travas: ponto compartilhado, solicitação exclusiva
- **Tipo:** Escolha, decidida pelo responsável da issue (a issue pede "coordenar locks/isolamento com API-14").
- **Opções:** (a) travar o ponto já na criação; (b) só documentar e deixar para a #17.
- **Escolhido:** (a).
  - Criar e corrigir: `PontoRepository.travarParaSolicitacao` (`SELECT ... FOR SHARE`) antes de conferir o ponto e os vínculos.
  - Corrigir: antes disso, `SolicitacaoRepository.travarParaAlteracao` (`SELECT ... FOR UPDATE`) na solicitação.
- **Por quê compartilhada:** várias solicitações para o mesmo ponto continuam em paralelo; só quem quer **alterar** o ponto espera.
- **Combinado para a #17 (API-14):** inativar ponto ou remover material aceito deve travar a linha do ponto com `FOR UPDATE` **antes** de contar as solicitações abertas. Assim, ou a solicitação confirma primeiro e a inativação a enxerga, ou a inativação confirma primeiro e a solicitação vê o ponto inativo.
- **Consequência:** a ordem das travas é sempre solicitação e depois ponto, o que evita deadlock entre correção e inativação. Inativar **material** não está coberto por esta trava (ver pendentes).

### 1.11 Solicitação de outro morador responde `404`
- **Tipo:** Escolha, decidida pelo responsável da issue.
- **Opções:** (a) `404`, como se não existisse; (b) `403 ACESSO_NEGADO`.
- **Escolhido:** (a), igual ao endereço de outro morador (API-04).
- **Consequência:** a busca já filtra por `morador_id`; não se revela que o id existe.

### 1.12 Datas em UTC na resposta
- **Tipo:** Escolha (RNF05 exige fuso ou offset; o formato exato fica em aberto).
- **Situação:** o banco devolve `timestamptz` em UTC. Gravando com o horário local, a mesma data saía com `-03:00` logo após criar e com `Z` ao ser relida.
- **Escolhido:** gravar e responder sempre em UTC (`2026-11-11T18:44:19.095Z`), com precisão de milissegundos.
- **Consequência:** o front converte para o horário local ao exibir. `Z` é um offset ISO 8601 válido.

### 1.13 `POST` e `PUT` devolvem a solicitação completa
- **Tipo:** Escolha (contrato: "saída: protocolo, situação, data, versão, itens, cópias e histórico inicial").
- **Escolhido:** `SolicitacaoResponse` com itens (por nome do material) e histórico (cronológico).
- **Consequência:** o `Location` do `201` aponta para `/solicitacoes/{id}`, que só passa a responder com a #14 (API-11). A conversão das quantidades (`20.000` → `20`) foi repetida aqui pela terceira vez (ver pendentes).

---

## 2. Validação

**Ambiente:** PostgreSQL 17 descartável (container `postgres:17`, sem volume, só em `127.0.0.1`, porta 55432, senha descartável), Java 21. Container parado, API encerrada e arquivos temporários apagados ao fim.

**Comandos:**
1. Banco: `docker run --rm -d --name coleta-tmp-pg-13 --tmpfs /var/lib/postgresql/data -e POSTGRES_PASSWORD=<senha descartável> -e POSTGRES_DB=coleta_teste -p 127.0.0.1:55432:5432 postgres:17`.
2. Testes: `./mvnw -B -ntp test`, com `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD` apontando para o banco.
3. API real: `./mvnw -B -ntp -q spring-boot:run -Dspring-boot.run.profiles=demo -Dspring-boot.run.arguments=--server.port=18080` e chamadas `curl`.
4. Limpeza: encerrar a API e `docker stop coleta-tmp-pg-13`.

**Resultado dos testes (2026-11-11, banco recém-criado, código final):** **151 testes, 0 falhas, 0 erros.** Desses, 18 são desta issue: 13 em `SolicitacaoIntegracaoTest` (dados desfeitos ao fim) e 5 em `SolicitacaoTransacaoIntegracaoTest` (transações reais; apaga os próprios dados). Antes eram 133. Depois da suíte, as tabelas de solicitação, morador, ponto e material ficaram com 0 linhas. O teste de concorrência foi repetido 3 vezes seguidas, sempre com o mesmo resultado.

**Dois erros encontrados pelos testes e corrigidos:**
- O protocolo passava de 20 caracteres, porque o formatador de data incluía o fuso (`20261111-0300`). Trocado por `yyyyMMdd`.
- A versão não subia quando a correção trocava só os itens (decisão 1.9).

**Chamadas reais** (perfil `demo`; morador 85, ponto 103, vidro 171 com limite 10, garrafa 172, papel 173 sem vínculo):

| # | Chamada | Resposta |
| --- | --- | --- |
| 1 | `POST /solicitacoes` com vidro 2.5 e garrafa 12 | `201`, protocolo `CS-20261111-W9M78B`, `versao 0`, 2 itens, 1 linha de histórico |
| 2 | Vidro + papel (papel não aceito) | `409 MATERIAL_INCOMPATIVEL`, "O ponto não aceita o material Papel." |
| 3 | Vidro 10.5 (limite 10) | `409 MATERIAL_INCOMPATIVEL`, "A quantidade de Vidro excede o limite de 10 por solicitação." |
| 4 | Garrafa 2.5 (peça) | `400`, campo `itens[0].quantidadeEstimada`, "deve ser inteira para UNIDADE" |
| 5 | Vidro duas vezes | `400`, campo `itens[1].materialId`, "material repetido na solicitação" |
| 6 | Criar como `ADMINISTRADOR` | `403`, "Operação permitida apenas ao morador." |
| 7 | `PUT /solicitacoes/29/dados` com `versao 0` | `200`, `versao 1`, 1 item, histórico com 2 linhas e os dados anteriores no `motivo` |
| 8 | Mesma correção de novo com `versao 0` | `409 CONFLITO_VERSAO` |

Banco depois das 8 chamadas: 1 solicitação, 1 item e 2 linhas de histórico (as 5 rejeições não gravaram nada). Essas chamadas foram feitas antes do ajuste da decisão 1.12; na chamada 1 a data ainda saiu com `-03:00`. A suíte completa foi rodada de novo depois do ajuste.

**Critérios de aceite da issue:**

| Critério | Teste que prova |
| --- | --- |
| Dois materiais válidos geram 201, protocolo único, itens e histórico persistidos | `doisMateriaisValidosGeram201ComProtocoloItensEHistorico`; `criacaoConfirmadaPersisteSolicitacaoItemEHistorico` |
| Material não aceito, inativo ou acima do limite: rejeitado sem gravação parcial | `materialNaoAceitoInativoOuAcimaDoLimiteERejeitadoSemGravarNada`; `segundoItemIncompativelNaoDeixaLinhasParciais` |
| Material repetido | `materialRepetidoEListaVaziaSaoRejeitados` |
| Ponto indisponível | `pontoInativoOuSemRecolhimentoDomiciliarERejeitado` |
| Zero, negativo e peça fracionária rejeitados | `quantidadeZeroNegativaOuComCasasDemaisERejeitada` (3 casos), `pecaFracionariaERejeitada` |
| Endereço deve pertencer ao morador | `enderecoDeveSerDoMoradorESoMoradorCria` |
| Alterar cadastro de endereço/unidade não muda cópias | `alterarCadastroDeEnderecoOuUnidadeNaoMudaAsCopias` |
| Correção após agendamento conflita | `correcaoAposAgendamentoConflita` |
| Correção válida atualiza versão e preserva rastreabilidade | `correcaoValidaTrocaItensEEnderecoSobeAVersaoEGuardaOsDadosAnteriores` |
| Correção inválida não altera nada | `correcaoInvalidaNaoAlteraNada` |
| Rollback em caso de falha (RNF08) | `falhaAoGravarHistoricoDesfazSolicitacaoEItens` |
| Concorrência (RN06, RNF08) | `duasCorrecoesSimultaneasComAMesmaVersaoUmaConfirmaEAOutraRecebe409` |
| Política de travas com a API-14 | `travaDoPontoPermiteOutrasSolicitacoesMasBloqueiaQuemQuerAlteraLo` |

---

## 3. Pendentes

- **#17 (API-14):** usar `FOR UPDATE` na linha do ponto ao inativar ponto ou remover material aceito (decisão 1.10). Até lá, a trava desta issue existe, mas o outro lado ainda não trava. Inativar **material** durante a criação de uma solicitação continua sem trava; decidir lá se precisa.
- **#18 (API-15):** aplicar a mesma comparação de versão às demais ações e confirmar a política única.
- **#14 (API-11):** `GET /solicitacoes/{id}` (o `Location` da criação aponta para lá), lista e histórico.
- **#15 (API-12):** o teste da correção após agendamento altera a situação direto no banco; trocar pela ação real quando existir.
- O `DER.md` ainda lista "formato e geração do protocolo" como pendente; a decisão 1.4 resolve. Não alterei o DER, que é de outro integrante.
- A conversão `semZerosFinais` está repetida em `PontoMaterialService`, `ConsultaPublicaService` e `SolicitacaoService`. Sugestão: mover para um lugar comum (por exemplo `QuantidadeMaterialService`) num PR à parte.
- Revisão do Genisson sobre as decisões 1.8 (auditoria no `motivo`), 1.9 (incremento da versão) e 1.10 (travas), que afetam as issues dele.
