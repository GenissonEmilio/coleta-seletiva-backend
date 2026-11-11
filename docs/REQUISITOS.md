# Rastreabilidade dos requisitos

Resumo de planejamento baseado no `TrabalhoWeb01.pdf`, não substitui a especificação. Todos os RF, RN e RNF são obrigatórios no projeto básico; extensões são opcionais. Os códigos PLAN/API identificam tarefas no [backlog](BACKLOG.md). Estado inicial de todas as implementações: **pendente**.

## Requisitos funcionais

| ID | Comportamento a entregar | Tarefas principais | Evidência mínima |
| --- | --- | --- | --- |
| RF01 | Criar, consultar e atualizar morador; nome/e-mail obrigatórios; e-mail normalizado único; telefone no cadastro | API-04 | Repetição de e-mail rejeitada inclusive após normalização |
| RF02 | Endereço completo de um único morador; número ou indicação de sem número; complemento opcional | API-04 | Rejeitar endereço incompleto e uso de endereço de outro morador |
| RF03 | Material com nome único, descrição, unidade, preparo e inativação | API-05 | Material inativo não entra em nova solicitação |
| RF04 | Ponto com nome, endereço, contato, horários, situação e disponibilidade domiciliar | API-06, API-14 | Ponto inválido rejeitado; inativo fora da disponibilidade pública |
| RF05 | Vínculo único ponto/material; observações e limite opcional positivo na unidade correta | API-07 | Duplicata e limite zero/negativo rejeitados |
| RF06 | Filtrar pontos ativos por nome, município, bairro e material aceito; paginação e orientações | API-08 | Filtro vidro só retorna pontos ativos que o aceitam |
| RF07 | Cadastro de operadores e vínculo com pontos | API-09 | Resolver contexto e vínculos; se houver autenticação, testar autorização entre pontos |
| RF08 | Criar solicitação com pelo menos um item, protocolo único, data e SOLICITADA | API-10 | Dois materiais válidos persistem com histórico; incompatibilidade reverte tudo |
| RF09 | Detalhes, itens, protocolo, situação, agendamento e histórico; filtros de período/situação/ponto e paginação | API-11 | Resultado respeita filtros e contexto do ator |
| RF10 | Agendar data/hora futura e passar a AGENDADA com histórico | API-12 | Passado rejeitado sem mudar a solicitação |
| RF11 | Recusar somente SOLICITADA com motivo | API-12 | Motivo obrigatório e evento visível no histórico |
| RF12 | Cancelar somente SOLICITADA ou AGENDADA com motivo | API-12 | Iniciada/concluída não podem ser canceladas |
| RF13 | Iniciar AGENDADA; concluir com data, responsável e efetivos de todos os itens | API-13 | Zero permitido em um item, mas rejeitar conclusão sem nenhum positivo |
| RF14 | Reagendar AGENDADA; falha em EM_RECOLHIMENTO volta a AGENDADA com motivo e nova data futura | API-13 | Preservar tentativas e todas as alterações |
| RF15 | Contagem por situação e efetivos recolhidos por material/período | API-16 | Totais só de CONCLUIDA; separar unidades e ignorar estimativas |

## Regras de negócio

| ID | Regra | Tarefas responsáveis |
| --- | --- | --- |
| RN01 | Ponto ativo/domiciliar, materiais ativos/aceitos, sem repetição e limites respeitados | API-07, API-10, API-14, API-17 |
| RN02 | Decimal para peso/volume; peças integrais; estimativa > 0; efetivo >= 0 | API-05, API-07, API-10, API-13, API-17 |
| RN03 | Preservar cadastros vinculados e registrar anterior/nova situação, data, responsável e motivo | API-02, API-10, API-12, API-13, API-14 |
| RN04 | Cópia de endereço/unidade; corrigir itens/endereço apenas antes de agendar com revalidação | API-02, API-10, API-17 |
| RN05 | Bloquear inativação de ponto e retirada de material se houver solicitações abertas afetadas | API-14, API-17 |
| RN06 | Transação única e rejeição de segunda escrita usando a mesma versão | API-10, API-12, API-13, API-15, API-17 |

Aberta = SOLICITADA, AGENDADA ou EM_RECOLHIMENTO. Estados finais = CONCLUIDA, RECUSADA e CANCELADA. Regras de concorrência entre disponibilidade/vínculos e criação também devem ser verificadas; o versionamento da solicitação não cobre sozinho essas disputas.

## Requisitos não funcionais

| ID | Exigência | Tarefas / momento |
| --- | --- | --- |
| RNF01 | Java 21, Spring Boot, REST, PostgreSQL, JPA, camadas e Bean Validation | API-01 e todos os módulos |
| RNF02 | Interface em 360/1366 px; obrigatoriedade, confirmação de cancelamento e erros locais | PLAN-03 e frontend na Unidade 2 |
| RNF03 | JSON; criação 201; entrada inválida 400; ausente 404; regra/versão 409; erro global sem stack trace | API-03 e todos os endpoints |
| RNF04 | Máximo de 100 por página; índices; p95 <= 2 s com 1.000 solicitações e evidências | API-02, API-03, API-08, API-11, API-18 |
| RNF05 | PK/FK/uniqueness/transações; eventos com fuso; persistência após reinício | API-02, API-10, API-12, API-13, API-18 |
| RNF06 | Dados fictícios; ambiente para segredos; sem dados pessoais públicos; login opcional com hash e autorização se adotado | API-01, API-08, API-09, API-18 |
| RNF07 | OpenAPI ou Postman, README, configuração, execução, migrações e demonstração | API-03, cada PR e API-18 |
| RNF08 | Testar compatibilidade, quantidades, transições, limites, concorrência; integração/rollback com itens e histórico | Testes de cada módulo e API-17 |

## Modelagem e protótipos

`PLAN-02` deverá representar morador, endereço, material, ponto, vínculo ponto-material, operador, vínculo operador-ponto, solicitação, item e histórico. A tabela de vínculos operador-ponto é uma proposta para explicitar a relação, não uma entidade extra exigida nominalmente no PDF. O DER deve mostrar cópias históricas e restrições, sem excluir a documentação das decisões.

`PLAN-03` deve cobrir consulta pública; morador/endereço; administração de materiais/pontos; vínculo de materiais; solicitação com vários itens; lista/detalhe/histórico; painel de operador e resumo gerencial. Também prever vazio, erro, confirmação e larguras mínimas.

## Oito cenários para a apresentação

1. Cadastro e pesquisa de ponto que aceita papel/plástico por município e material.
2. Solicitação válida com dois itens, protocolo e histórico inicial.
3. Material incompatível com erro e nenhuma gravação parcial.
4. Agendamento passado rejeitado mantendo SOLICITADA.
5. Agendar, iniciar e concluir com quantidades efetivas e histórico completo.
6. Cancelar agendada e rejeitar reabertura/conclusão de situação final.
7. Falha de execução e reagendamento preservando a tentativa anterior.
8. Reiniciar, confirmar persistência e conferir indicadores sem misturar unidades.

O roteiro executável e os resultados dos oito cenários estão registrados na
[evidência de aceite da API-17](evidencias/api-17-aceite.md).
