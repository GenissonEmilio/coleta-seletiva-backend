# Contrato proposto da API

**Rascunho para revisão em PLAN-01 e API-03. Não existem endpoints implementados neste momento.** Os nomes, verbos de ações, envelope de página, parâmetros e contexto de demonstração são propostas da equipe, não imposições literais do PDF. Publicar o contrato definitivo em OpenAPI ou coleção Postman durante o desenvolvimento.

## Convenções

- Prefixo `/api/v1`; JSON UTF-8; DTOs de entrada/saída. Nunca serializar o grafo JPA diretamente.
- IDs estáveis; protocolo único da solicitação não substitui necessariamente a chave interna.
- `201` e `Location` na criação; `200` com representação ou `204` sem corpo para atualizações conforme contrato; `400` para entrada inválida; `404` para registro inexistente; `409` para conflito de regra, unicidade ou versão.
- Listagens: `page` a partir de zero e `size` entre 1 e 100 (padrão proposto 20); entrada fora do intervalo retorna 400. Ordenação estável com ID como desempate.
- Envelope de página proposto: `content`, `page`, `size`, `totalElements`, `totalPages`.
- Eventos e agendamento usam ISO 8601 com offset; filtros por período precisam definir limites inclusivo/exclusivo e fuso em sua documentação. Proposta: início inclusivo, fim exclusivo; solicitações por criação, efetivos por conclusão.
- Quantidades decimais exatas no backend e banco; validar quantidade integral para unidade de peça. Fechar representação JSON e precisão/escala em API-02/API-03.

Exemplo de erro proposto:

```json
{
  "codigo": "ENTRADA_INVALIDA",
  "mensagem": "Revise os campos informados.",
  "errosCampos": [{"campo": "itens[0].quantidadeEstimada", "mensagem": "Deve ser positiva."}]
}
```

Erros de regra podem ter `errosCampos: []`; nunca expor stack trace, SQL ou credenciais. Para uma referência existente e incompatível, usar 409; campos malformados são 400.

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

Listagens administrativas adicionais necessárias ao uso das telas devem seguir as mesmas regras de paginação e privacidade. Os caminhos abreviados da linha de ações mantêm o prefixo `/solicitacoes/{id}`. No vínculo via PUT, devolver 201 ao criar e 200/204 ao atualizar; repetição não gera uma segunda linha. A equipe deve documentar a política de duplicidade compatível com RF05.

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
