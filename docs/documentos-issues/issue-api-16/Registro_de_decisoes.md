# API-16 — Indicadores de solicitações e recolhimentos

A issue #19 implementa RF15 no contexto de demonstração do administrador. O repositório agrega os resultados no PostgreSQL e devolve DTOs com situação e total ou com material, unidade e efetivo somado.

A contagem usa a data de criação e a situação atual. As seis situações aparecem mesmo com total zero. Os recolhimentos usam a data de conclusão e somam efetivos preenchidos de solicitações CONCLUIDA. A chave do grupo é o ID do material e a unidade copiada no item. O grupo inclui zeros registrados e usa o nome cadastrado para exibição.

Os limites são opcionais. Início é inclusivo e fim exclusivo. O serviço exige que o fim seja posterior ao início quando recebe os dois. A lista de recolhimentos segue o envelope e os limites de paginação do projeto. O total da página conta os grupos distintos. A migração V8 cria índices para os filtros por criação e por situação e conclusão.

## Validação

A suíte local executou 195 testes com PostgreSQL 17 descartável. Os sete casos dos indicadores conferem o intervalo com offsets, os limites opcionais, todas as situações, os efetivos de concluídas, as unidades copiadas, a contagem de grupos na paginação, a resposta vazia e as permissões de contexto.

~~~bash
MAVEN_OPTS="-Xmx384m -XX:ActiveProcessorCount=2" \
  /home/genissonemilio/.local/bin/codex-build --memory-mib 1536 -- \
  sh ./mvnw -B -ntp -T 1 test \
  -DargLine="-Xmx448m -XX:ActiveProcessorCount=2" \
  -DforkCount=1 -DreuseForks=true
~~~
