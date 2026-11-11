# API-15 — Versão das mutações de solicitação

A solicitação existente usa a coluna versao mapeada com @Version. O cliente envia a versão lida. Ausência, valor negativo, fração, texto ou valor fora do intervalo de Long retorna 400. Se o cliente envia uma versão antiga, a operação retorna 409 CONFLITO_VERSAO.

A correção consulta a solicitação do morador e verifica a versão antes de alterar os dados. Quando o endereço muda, o flush usa o controle otimista do JPA. Quando a linha não mudou, o repositório incrementa a versão com uma condição sobre o id e a versão lida. Uma transação que não atualiza nenhuma linha recebe o mesmo 409. Essa disputa acontece antes de substituir os itens e gravar o histórico. Endereço, versão, itens e histórico confirmam ou revertem juntos.

O tratamento global cobre conflitos otimistas traduzidos pelo Spring e exceções otimistas do JPA. A trava compartilhada do ponto continua coordenando a disponibilidade com a API-14.

## Integração com o fluxo

Agendamento, recusa e cancelamento da API-12 já comparam a versão do cliente e usam @Version para gravar a nova situação. A API-13 deve usar a mesma validação e confirmar a versão antes de gravar itens ou histórico. Ações que só adicionam histórico precisam incrementar a versão, mesmo com a situação e os demais campos iguais. A revisão de início, conclusão, reagendamento e tentativa sem sucesso segue pendente até a API-13.

## Validação

Foram executados 41 testes locais com PostgreSQL 17 descartável, usando o executor codex-build e o Maven Wrapper.

~~~bash
sh ./mvnw -B -ntp -T 1 test -Dtest=SolicitacaoIntegracaoTest,SolicitacaoTransacaoIntegracaoTest,GlobalExceptionHandlerTest -DforkCount=1 -DreuseForks=true
~~~

Em duas transações coordenadas, os testes carregam a mesma versão antes de prosseguir. Uma requisição responde 200 e a outra responde 409 CONFLITO_VERSAO. O banco guarda apenas o item e o evento da vencedora. O teste roda com e sem mudança do endereço e usa dois workers. A suíte também cobre entradas inválidas, versão antiga e respostas do tratamento global.
