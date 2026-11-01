# Backlog da primeira unidade

Inclui três tarefas de planejamento e dezoito tarefas de API. Todas começam abertas. IDs PLAN/API são estáveis para referência entre documentos; o número GitHub será adicionado após publicação. Datas serão definidas pelo professor.

A coluna responsável representa a divisão acordada, mesmo enquanto houver convite de colaboração pendente. Cada integrante tem sete tarefas, incluindo o planejamento. Consulte o [guia da equipe](GUIA_IMPLEMENTACAO.md).

| ID | Issue | Responsável | Fase | Depende de |
| --- | --- | --- | --- | --- |
| PLAN-01 | Revisar requisitos, atores e decisões da primeira unidade | [Genisson](https://github.com/GenissonEmilio) | planejamento | - |
| PLAN-02 | Desenhar e revisar o DER e as restrições do banco | [Felipe](https://github.com/felipmz) | planejamento | PLAN-01 |
| PLAN-03 | Prototipar as telas e revisar os contratos com a equipe | [Herbert](https://github.com/Jose-Herbert-13) | planejamento | PLAN-01 |
| API-01 | Preparar Java 21, Spring Boot e execução reproduzível | [Genisson](https://github.com/GenissonEmilio) | fundacao | PLAN-01 |
| API-02 | Implementar migrações PostgreSQL e integridade relacional | [Felipe](https://github.com/felipmz) | fundacao | PLAN-02, API-01 |
| API-03 | Padronizar DTOs, validação, erros e paginação | [Herbert](https://github.com/Jose-Herbert-13) | fundacao | PLAN-01, API-01 |
| API-04 | Implementar moradores e seus endereços | [Felipe](https://github.com/felipmz) | cadastros | API-02, API-03 |
| API-05 | Implementar materiais, unidades e inativação | [Genisson](https://github.com/GenissonEmilio) | cadastros | API-02, API-03 |
| API-06 | Implementar pontos de coleta e disponibilidade | [Felipe](https://github.com/felipmz) | cadastros | API-02, API-03 |
| API-07 | Vincular materiais aceitos e limites aos pontos | [Felipe](https://github.com/felipmz) | cadastros | API-05, API-06 |
| API-08 | Consultar pontos ativos e materiais com filtros públicos | [Herbert](https://github.com/Jose-Herbert-13) | cadastros | API-03, API-07 |
| API-09 | Cadastrar operadores, vínculos e contexto de demonstração | [Herbert](https://github.com/Jose-Herbert-13) | cadastros | API-03, API-04, API-06 |
| API-10 | Criar e corrigir solicitação com itens e histórico transacionais | [Herbert](https://github.com/Jose-Herbert-13) | fluxo | API-04, API-07, API-09 |
| API-11 | Consultar solicitações, itens e histórico por contexto | [Herbert](https://github.com/Jose-Herbert-13) | fluxo | API-10 |
| API-12 | Agendar, recusar e cancelar com histórico e versão | [Genisson](https://github.com/GenissonEmilio) | fluxo | API-09, API-10 |
| API-13 | Executar, concluir e reagendar preservando tentativas | [Felipe](https://github.com/felipmz) | fluxo | API-12 |
| API-14 | Preservar cadastros e bloquear alterações com solicitações abertas | [Genisson](https://github.com/GenissonEmilio) | fluxo | API-04, API-07, API-10 |
| API-15 | Garantir concorrência otimista em todas as mutações de solicitação | [Genisson](https://github.com/GenissonEmilio) | fluxo | API-10 |
| API-16 | Implementar resumo por situação e recolhidos por material/período | [Herbert](https://github.com/Jose-Herbert-13) | fluxo | API-11, API-13 |
| API-17 | Consolidar testes essenciais, integração e cenários de aceite | [Genisson](https://github.com/GenissonEmilio) | qualidade | API-08, API-11, API-12, API-13, API-14, API-15, API-16 |
| API-18 | Fechar reprodução, dados fictícios, documentação e desempenho | [Felipe](https://github.com/felipmz) | qualidade | API-16, API-17 |

## Caminho inicial

Revisar PLAN-01; produzir DER e protótipos em PLAN-02/PLAN-03; iniciar API-01/API-02/API-03. Depois, cadastros -> consulta pública e operadores -> criar/consultar solicitação -> atendimento/consistência -> validação. API-15 pode começar após API-10, mas só termina depois da integração das ações de API-12/API-13.

A primeira fatia demonstrável termina em API-10/API-11; ela não encerra a primeira unidade. API-17/API-18 consolidam testes, reprodução e evidências da API completa. O desenvolvimento de testes começa em cada módulo, antes da consolidação.

As dependências indicam pré-requisitos principais. Protótipos/contratos precisam ser revisados com a equipe; concluir o DER não dispensa integração posterior entre schema e serviços.
