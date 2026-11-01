# Coleta Seletiva - Backend

API acadêmica de Programação Web I para conectar moradores, pontos de coleta e operadores de recolhimento de recicláveis.

**Estado inicial:** planejamento e backlog. O código da aplicação será escrito pela equipe; ainda não há endpoints executáveis, banco provisionado ou testes implementados.

## Comece aqui

1. Leia o [guia de implementação da equipe](docs/GUIA_IMPLEMENTACAO.md).
2. Confira a [rastreabilidade dos requisitos](docs/REQUISITOS.md) e o [contrato proposto da API](docs/CONTRATO_API.md).
3. Escolha uma tarefa no [backlog com responsáveis e dependências](docs/BACKLOG.md).
4. Siga o [fluxo de contribuição](CONTRIBUTING.md) e abra um PR pequeno por issue.

## Tecnologias exigidas

Java 21, Spring Boot, Spring Data JPA, Bean Validation, API REST e PostgreSQL. Maven com wrapper e migrações Flyway são escolhas propostas para reprodução; as versões exatas deverão ser fixadas e validadas na issue de bootstrap.

A arquitetura deve separar Entity, DTO, Repository, Service e Controller. As regras de negócio ficam nos serviços.

## Repositórios

- Backend: <https://github.com/GenissonEmilio/coleta-seletiva-backend>
- Frontend: <https://github.com/GenissonEmilio/coleta-seletiva-frontend>

Ambos começam privados. O frontend entra na Unidade 2; seus protótipos são preparados antes da implementação da API.

## Execução e testes

O bootstrap ainda será desenvolvido. Sua issue deve entregar o Maven Wrapper, configuração por variáveis de ambiente, comandos de execução e testes e configuração do PostgreSQL. Este README deve ser atualizado com os comandos realmente verificados nesse PR.

Não há configuração de banco, senha de demonstração ou credencial de produção neste commit. Não use dados pessoais reais nas demonstrações.

## Referência acadêmica

Planejamento derivado de **TrabalhoWeb01.pdf**, “Sistema de Coleta Seletiva”, fornecido pela equipe em 01/11/2026. A seção 14 distingue Etapa 1 (requisitos, protótipos e modelo), Etapa 2 (banco, API e regras), Etapa 3 (interface na Unidade 2) e Etapa 4 (testes e entrega). Neste projeto, **primeira unidade** abrange o planejamento e a API completa. Datas dependem do professor.
