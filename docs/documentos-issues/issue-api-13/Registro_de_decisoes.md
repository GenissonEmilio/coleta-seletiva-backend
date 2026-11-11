# API-13 — Execução, conclusão e reagendamento

A issue #16 implementa RF13 e RF14 nos endpoints da solicitação. O operador vinculado ao ponto pode iniciar um recolhimento agendado, concluir um em execução, reagendar um agendado ou registrar uma tentativa sem sucesso. As permissões e o formato das respostas seguem o contexto e os DTOs do projeto.

## Conclusão

O cliente informa uma quantidade efetiva por material. O serviço valida a lista completa antes de escrever. A unidade vem da cópia de cada item, inclusive se o cadastro mudou depois da solicitação. Um item pode ter efetivo zero se algum outro tiver efetivo positivo. O relógio do servidor fornece a data de conclusão e o contexto fornece o operador. O evento grava os dois junto com as situações anterior e nova.

## Tentativas e versão

Reagendamento e tentativa sem sucesso exigem justificativa e agendamento futuro. A falha de uma execução muda a situação para AGENDADA e guarda o motivo, o operador, o instante e o horário da próxima tentativa. A sequência do histórico mantém o horário e o início das tentativas anteriores.

Cada ação valida a versão do cliente. O serviço confirma a mudança na linha versionada antes de gravar itens ou histórico. Reagendar para o mesmo horário futuro também registra a justificativa e incrementa a versão pela atualização condicional da API-15. Em conflito, só a primeira alteração confirma. Situação, data, versão, itens e histórico confirmam na mesma transação.

## Validação

A suíte local executou 200 testes com PostgreSQL 17 descartável. Os 12 novos casos verificam o fluxo de execução, o histórico de tentativas, a unidade copiada, as entradas inválidas, o contexto, o rollback após falha no histórico e dois reagendamentos concorrentes. No teste de concorrência, o PostgreSQL confirma que a segunda transação aguarda a primeira, e apenas um evento é persistido.

~~~bash
MAVEN_OPTS="-Xmx384m -XX:ActiveProcessorCount=2" \
  /home/genissonemilio/.local/bin/codex-build --memory-mib 1536 -- \
  sh ./mvnw -B -ntp -T 1 test \
  -DargLine="-Xmx448m -XX:ActiveProcessorCount=2" \
  -DforkCount=1 -DreuseForks=true
~~~
