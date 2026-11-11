# API-18 — Entrega da primeira unidade

## Reprodução e documentação

- O [README](../../README.md#versões-e-reprodução-com-docker) registra versões, banco, variáveis, migrações, execução, testes e escolha de atores. Em ambiente limpo, o Compose subiu PostgreSQL 17 e o Maven Wrapper iniciou o perfil `demo` com JDK 21. Foram conferidos os quatro atores fictícios e o contexto administrativo via HTTP. O ensaio usou um banco descartável em tmpfs.
- O [OpenAPI versionado](../openapi.yaml) cobre as 45 operações expostas no perfil `demo`. Os exemplos de requisição e resposta foram capturados via HTTP com dados fictícios. Cada operação tem um sucesso e um erro verificados, incluindo os códigos 201/200 nos vínculos e 204 sem corpo nas remoções. Foram conferidos os caminhos e métodos contra `/v3/api-docs` e a estrutura dos exemplos contra os esquemas publicados.
- Para reproduzir uma solicitação no Swagger, obtenha os IDs em `GET /api/v1/demo/atores`. Cadastre um endereço para a moradora, crie um material e vincule-o ao ponto de uma operadora. Crie a solicitação com esse endereço, ponto e material no contexto da moradora. Com o contexto da operadora, agende uma data futura e envie a versão retornada a cada nova ação. O [roteiro da API-17](api-17-aceite.md) automatiza o fluxo de ponta a ponta.

## Persistência e desempenho

Medição de 11/11/2026 em um notebook Linux x86_64 (kernel 7.2.13), Intel Core i5-13420H com 8 núcleos e 12 threads e 15,3 GiB de RAM. Ambiente com Temurin 21.0.12.1, Spring Boot 4.1.2, Maven 3.10.0, Python 3.14.8, Docker 29.8.2 e PostgreSQL 17.11. Banco limitado a 1 CPU e 512 MiB e JVM da API a heap de 448 MiB e 2 processadores ativos.

Da raiz do repositório, com as APIs 13, 16 e 17 integradas:

```bash
python3 -B coleta-seletiva-api/scripts/validar_api.py --aceite
python3 -B coleta-seletiva-api/scripts/desempenho_api.py --usar-jar-existente
```

Neste notebook, os comandos foram executados com `codex-build`. A suíte executou 207 testes sem falhas e os oito cenários de aceite passaram. O roteiro de desempenho carregou [1.000 solicitações](../../coleta-seletiva-api/scripts/dados_desempenho.sql), 2.000 itens e 2.331 eventos, distribuídos entre seis situações, 20 moradores fictícios e dois pontos.

O roteiro encerrou a JVM de PID 46534 e iniciou a de PID 46686 contra o mesmo banco. A API manteve a solicitação `DEMO-0006` na versão 3 com quatro eventos, as 1.000 solicitações nas listagens, os quatro atores do perfil `demo` e os totais:

| Situação atual | Solicitações |
| --- | ---: |
| SOLICITADA | 167 |
| AGENDADA | 167 |
| RECUSADA | 167 |
| CANCELADA | 167 |
| EM_RECOLHIMENTO | 166 |
| CONCLUIDA | 166 |

O indicador de recolhimentos manteve 415 KG de papel, 166 UNIDADE de plástico e 103,75 LITRO de óleo.

O [roteiro de medição](../../coleta-seletiva-api/scripts/desempenho_api.py) usou um cliente HTTP local sequencial, com cinco aquecimentos por consulta fora da amostra. As 150 amostras incluem HTTP e leitura do JSON. O p95 é o valor na posição `ceil(0,95 × n)` da amostra ordenada. As consultas por período usam 08/09/2026 inclusive a 01/10/2026 exclusive, em UTC.

| Consulta | Amostras | p95 (ms) | Máxima (ms) |
| --- | ---: | ---: | ---: |
| Pontos por município e material, página 1, tamanho 1 | 30 | 6,02 | 9,92 |
| Solicitações por período, situação e ponto, página 1, tamanho 20 | 30 | 9,36 | 10,66 |
| Detalhe com itens e histórico | 30 | 7,08 | 7,96 |
| Indicador de situações por período | 30 | 4,69 | 5,16 |
| Recolhimentos por período, página 1, tamanho 2 | 30 | 5,25 | 5,68 |
| Total | 150 | 7,94 | 10,66 |

A meta acadêmica RNF04 de p95 ≤ 2.000 ms foi atingida no cenário local. A medição usou os índices existentes e as migrações integradas da API-16.

## Material para revisão

- [DER, restrições e índices](../DER.md), [contrato da API](../CONTRATO_API.md), [requisitos](../REQUISITOS.md) e [registros de decisões](../documentos-issues).
- [Aceite e integração](api-17-aceite.md), [prova de persistência e desempenho](#persistência-e-desempenho) e [OpenAPI com exemplos](../openapi.yaml).
- A validação integrou localmente as PRs [#43](https://github.com/GenissonEmilio/coleta-seletiva-backend/pull/43), [#44](https://github.com/GenissonEmilio/coleta-seletiva-backend/pull/44) e [#45](https://github.com/GenissonEmilio/coleta-seletiva-backend/pull/45). A reprodução pelos três integrantes e a revisão da PR fazem parte do aceite da equipe.
- A interface responsiva em 360 e 1.366 px e os demais itens de RNF02 estão registrados para a Unidade 2 no [mapa de requisitos](../REQUISITOS.md).
