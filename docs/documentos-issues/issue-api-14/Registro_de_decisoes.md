# API-14 — Proteção de cadastros e vínculos

A issue #17 aplica RN03, RN04 e RN05. A inativação do morador usa PATCH com ativo, como nos cadastros de material e ponto. As chaves estrangeiras com ON DELETE RESTRICT já preservam os cadastros referenciados. A situação do cadastro é alterada sem mudar o endereço copiado, a unidade dos itens ou o histórico.

Antes de inativar um ponto ativo, o serviço conta suas solicitações em SOLICITADA, AGENDADA e EM_RECOLHIMENTO. A retirada de um material aceito conta apenas solicitações abertas do ponto com aquele material nos itens. Se a contagem for positiva, a operação retorna 409 PONTO_COM_SOLICITACOES_ABERTAS. As três situações finais liberam a alteração.

A criação e a correção da API-10 usam a trava compartilhada na linha do ponto. A atualização do ponto, a mudança de situação e as escritas dos vínculos usam a trava exclusiva na mesma linha. A contagem e a alteração ocorrem na transação que possui a trava. Assim, a criação confirmada primeiro entra na contagem, e a alteração confirmada primeiro é observada pela validação da criação.

## Validação

Foram executados 55 testes locais com PostgreSQL 17 descartável, usando o executor codex-build e o Maven Wrapper.

~~~bash
sh ./mvnw -B -ntp -T 1 test -Dtest=SolicitacaoTransacaoIntegracaoTest,SolicitacaoIntegracaoTest,PontoServiceTest,PontoMaterialIntegracaoTest,PontoIntegracaoTest -DforkCount=1 -DreuseForks=true
~~~

Os testes cobrem estados abertos e finais, retirada de material que não está no atendimento, preservação dos itens e do histórico, exclusão física rejeitada pelas chaves estrangeiras e morador inativo. Os testes de corrida usam duas transações. A primeira só confirma depois de pg_blocking_pids indicar que a segunda aguarda a trava. Foram verificados os dois resultados possíveis para criação concorrente com inativação e com remoção do vínculo.
