# API-17 — Roteiro e evidência de aceite

O roteiro cobre os oito cenários da seção 13 do enunciado. Ele chama a API por HTTP com dados fictícios e usa um PostgreSQL 17 descartável. A suíte automatizada roda primeiro. O aceite usa um segundo banco no mesmo container, com a aplicação empacotada em JAR.

Execute a suíte na raiz do repositório com JDK 21, Python 3 e Docker em execução.

~~~bash
python3 coleta-seletiva-api/scripts/validar_api.py
~~~

Para executar também os cenários, use o comando abaixo em um checkout com as APIs 13 e 16 integradas.

~~~bash
python3 coleta-seletiva-api/scripts/validar_api.py --aceite
~~~

O roteiro cria credenciais temporárias, escolhe portas em 127.0.0.1 e encerra os processos e o container ao final. Os relatórios Maven ficam em coleta-seletiva-api/target/surefire-reports. A saída do roteiro informa o resultado de cada cenário.

## Execução registrada em 11/11/2026

Ensaio local no Linux com JDK 25.0.4.1 compilando para Java 21 e PostgreSQL 17.11. O banco rodou com 512 MiB, uma CPU e dados em tmpfs. O executor limitou o grupo da validação a 1.536 MiB e a JVM usou dois processadores disponíveis. O Maven usou uma thread e um fork.

A execução reuniu a main em 08a1cbe, a API-13 em b137307, a API-16 em 7ddb765 e o roteiro desta issue em uma cópia temporária. As duas PRs de dependência ainda estavam abertas durante o ensaio. [API-13](https://github.com/GenissonEmilio/coleta-seletiva-backend/pull/43) · [API-16](https://github.com/GenissonEmilio/coleta-seletiva-backend/pull/44)

~~~bash
codex-build --memory-mib 1536 -- python3 coleta-seletiva-api/scripts/validar_api.py --aceite
~~~

A suíte integrada terminou com **207 testes, zero falhas, zero erros e zero ignorados**. O Maven gerou o JAR com BUILD SUCCESS e o roteiro confirmou **8 de 8 cenários**.

O comando da suíte, sem a opção de aceite, também foi executado na branch da API-17 baseada na main. Foram **188 testes aprovados** com PostgreSQL descartável.

| Cenário | Passos do roteiro | Resultado observado |
| --- | --- | --- |
| 1. Cadastro e pesquisa | Cadastrar ponto em Aracaju, vincular papel e plástico e pesquisar cada material por município. | As duas buscas retornaram o ponto. A busca por outro município retornou vazia. |
| 2. Solicitação válida | Criar duas solicitações com os dois materiais. Reler a primeira e contar as linhas do banco. | HTTP 201 com protocolos distintos. Foram gravadas 2 solicitações, 4 itens e 2 eventos iniciais. |
| 3. Material incompatível | Enviar papel aceito seguido de vidro sem vínculo. Conferir as três tabelas antes e depois. | HTTP 409 MATERIAL_INCOMPATIVEL. As contagens de solicitações, itens e histórico ficaram em 2, 4 e 2. |
| 4. Agendamento inválido | Agendar a primeira solicitação para o dia anterior. Reler o detalhe. | HTTP 400 com erro no campo agendadaPara. A solicitação ficou SOLICITADA na versão 0 com o histórico inicial. |
| 5. Execução completa | Agendar, iniciar e concluir. Reler os itens e os eventos. | CONCLUIDA na versão 3 com 2,5 KG de papel, 3 UNIDADE de plástico, data de conclusão e quatro eventos com o operador na conclusão. |
| 6. Cancelamento e situação final | Agendar a segunda solicitação, cancelar como morador e tentar iniciar e concluir como operador. | CANCELADA na versão 2. Início e conclusão retornaram 409 TRANSICAO_INVALIDA. O histórico permaneceu com três eventos. |
| 7. Tentativa sem sucesso | Alterar a unidade atual do papel para LITRO, criar a terceira solicitação, agendar, iniciar, registrar falha e reagendar. | AGENDADA na versão 4 com três horários no histórico, motivo da falha e operador responsável. |
| 8. Persistência e indicadores | Concluir a nova tentativa, encerrar o servidor e iniciar outro processo no mesmo banco. Reler o detalhe das três solicitações e consultar os indicadores do período. | PID 39361 passou para 39509. Os três detalhes persistiram. O painel mostrou papel 2,5 KG e 1,25 LITRO, plástico 3 UNIDADE, duas solicitações concluídas e uma cancelada. |

## Testes essenciais consolidados

A suíte aproveita os testes dos módulos e o roteiro valida o fluxo encadeado pelo servidor HTTP. Os principais casos de regressão estão nestas classes.

| Regra | Cobertura automatizada |
| --- | --- |
| Compatibilidade, quantidades, unidades e limites | SolicitacaoIntegracaoTest, PontoMaterialIntegracaoTest e QuantidadeMaterialServiceTest cobrem ponto indisponível, material incompatível, limite, peça integral, efetivos e rejeição de entradas inválidas. |
| Cópia histórica e correção antes do agendamento | SolicitacaoIntegracaoTest confere a unidade e o endereço copiados após alterar os cadastros. Também verifica substituição de itens, histórico da correção e recusa depois do agendamento. |
| Transições e tentativas | SolicitacaoIntegracaoTest verifica agendamento, recusa, cancelamento, execução, estados finais e nova tentativa. |
| Atomicidade real | SolicitacaoTransacaoIntegracaoTest confere as tabelas após rejeitar o segundo item. Também injeta falha no histórico e verifica o rollback da criação e da conclusão no PostgreSQL. |
| Concorrência e proteção de cadastros | SolicitacaoTransacaoIntegracaoTest coordena duas transações com a mesma versão e confirma apenas uma escrita. Testa a corrida entre criação, inativação do ponto e remoção de vínculo nas duas ordens de confirmação. |
| Contexto, privacidade e indicadores | ConsultaPublicaIntegracaoTest, SolicitacaoConsultaIntegracaoTest e IndicadorIntegracaoTest verificam as permissões, filtros, dados expostos e totais por unidade copiada. |

A medição de desempenho com 1.000 solicitações está no escopo da [API-18](https://github.com/GenissonEmilio/coleta-seletiva-backend/issues/21).
