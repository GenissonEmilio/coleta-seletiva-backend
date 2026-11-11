# API-12 — Agendamento, recusa e cancelamento

A issue #15 segue RF10 a RF12, RN03 e RN06. Agendamento e recusa exigem operador vinculado ao ponto. O cancelamento admite o morador dono (seção 3 do enunciado) e o operador vinculado (RN05). O contexto usa os cabeçalhos da API-09. Ações de um tipo não autorizado respondem 403 e solicitações fora do contexto respondem 404.

As ações reutilizam SolicitacaoService, SolicitacaoResponse e HistoricoSolicitacaoModel. O agendamento compara instantes com um Clock injetável e retorna 400 para datas no passado ou no instante atual. Recusa e cancelamento exigem motivo. Agendamento, recusa e cancelamento com situação incompatível retornam 409 TRANSICAO_INVALIDA.

Cada ação confere a versão enviada e grava a mudança com @Version. A resposta devolve a nova versão. O histórico é registrado na mesma transação, com situações, instante, ator, motivo quando exigido e agendamentoNovo ao agendar. O horário agendado continua visível após o cancelamento.

## Validação

Foram executados 30 testes das classes SolicitacaoIntegracaoTest e SolicitacaoTransacaoIntegracaoTest com PostgreSQL 17 descartável. A suíte local usou o Maven Wrapper com o executor codex-build, um fork e dois processadores disponíveis para a JVM.

```bash
sh ./mvnw -B -ntp -T 1 test -Dtest=SolicitacaoIntegracaoTest,SolicitacaoTransacaoIntegracaoTest -DforkCount=1 -DreuseForks=true
```

Os testes cobrem data futura, passado e limite com relógio fixo, permissões, motivo em branco, situações finais, versão ausente, negativa, fracionária ou antiga, histórico e rollback da situação e da versão em falha de gravação do histórico. A suíte completa é executada no CI da PR.
