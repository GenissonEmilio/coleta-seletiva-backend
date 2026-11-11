# Coleta Seletiva - Backend

API acadêmica de Programação Web I para conectar moradores, pontos de coleta e operadores de recolhimento de recicláveis.

## Versões e reprodução com Docker

| Componente | Versão do projeto |
| --- | --- |
| Java | 21 |
| Spring Boot | 4.1.2 |
| Maven Wrapper | Maven 3.10.0 |
| PostgreSQL | 17 (imagem `postgres:17`) |
| SpringDoc OpenAPI | 3.2.0 |

Para reproduzir no Linux, macOS ou WSL, instale o JDK 21, Git e Docker com o
plugin Compose. Os roteiros de validação usam Python 3. A partir da raiz
do repositório, execute:

```bash
cd coleta-seletiva-api
test -f .env || cp .env.example .env
```

Em uma instalação nova, defina uma senha local no `.env` antes de subir o banco.
As credenciais devem coincidir com as do banco se o volume já estiver inicializado.

```bash
docker compose up -d --wait db
set -a
. ./.env
set +a
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

O Compose disponibiliza o banco na porta local 5433. O Flyway aplica as
migrações de `src/main/resources/db/migration` ao iniciar e o Hibernate valida o
esquema. A API fica disponível na porta 8080. Em outro terminal, consulte os
atores fictícios e teste o contexto administrativo:

```bash
curl --fail-with-body http://localhost:8080/api/v1/demo/atores
curl --fail-with-body -H 'X-Ator-Tipo: ADMINISTRADOR' http://localhost:8080/api/v1/contexto
```

Para usar `MORADOR` ou `OPERADOR`, envie também `X-Ator-Id` com o `id`
retornado para o ator escolhido. O contexto serve para demonstração local.
Para testar um reinício, encerre a aplicação com `Ctrl+C` e repita o comando
do Maven mantendo o mesmo banco. Para parar o banco ao terminar, execute
`docker compose down` na pasta da API. O volume do Compose é preservado.

## Pré-requisitos

- **JDK 21** instalado e configurado no ambiente.
- **Git** instalado para clonar o repositório e contribuir com o projeto.
- **PostgreSQL** instalado e em execução, com:
  - banco de dados criado para a aplicação;
  - usuário e senha de acesso;
  - permissões para criar e alterar tabelas, necessárias às migrações do Flyway.
- **Acesso à internet na primeira execução**, para baixar o Maven e as dependências.
- **IDE ou editor com suporte a Java**, de sua preferência.

O projeto inclui o **Maven Wrapper**, configurado para utilizar o Maven
3.10.0. A instalação global do Maven é opcional.

Utilize `./mvnw` no Linux/macOS ou `.\mvnw.cmd` no Windows.
As dependências declaradas no `pom.xml` são baixadas automaticamente pelo Maven.

## Variáveis de ambiente

Antes de executar a aplicação, crie um banco no PostgreSQL e configure as
variáveis abaixo. O usuário precisa de permissões para criar e alterar
tabelas, utilizadas pelas migrações do Flyway.

| Variável | Descrição |
| --- | --- |
| `SPRING_DATASOURCE_URL` | URL de conexão JDBC com o PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | Senha do usuário |

Exemplo no Linux/macOS:

```bash
export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/coleta_seletiva'
export SPRING_DATASOURCE_USERNAME='seu_usuario'
export SPRING_DATASOURCE_PASSWORD='sua_senha_local'
```

No Windows, utilizando PowerShell:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/coleta_seletiva'
$env:SPRING_DATASOURCE_USERNAME = 'seu_usuario'
$env:SPRING_DATASOURCE_PASSWORD = 'sua_senha_local'
```

Substitua os valores pelos dados do seu ambiente. As variáveis também
podem ser configuradas na IDE. Não versione credenciais reais.

## Execução e testes

Execute os comandos do Maven Wrapper na pasta `coleta-seletiva-api`, onde está o
`pom.xml`, usando o terminal em que configurou as variáveis.

### Conferir o ambiente

```bash
./mvnw --version
```

A saída deve indicar Java 21 e a versão do Maven utilizada pelo projeto.

### Executar os testes

Com JDK 21, Python 3 e Docker em execução, rode a partir da raiz do repositório
no Linux, macOS ou WSL.

```bash
python3 coleta-seletiva-api/scripts/validar_api.py
```

O roteiro cria um PostgreSQL 17 descartável com credenciais temporárias e porta
local escolhida pelo Docker. O Maven executa a suíte e grava os relatórios em
`coleta-seletiva-api/target/surefire-reports`. O container é encerrado ao final.

Para usar seu próprio PostgreSQL, configure as variáveis para um banco reservado
aos testes e rode o Maven Wrapper na pasta da API.

```bash
./mvnw test
```

### Executar os oito cenários de aceite

Com a API-13 e a API-16 integradas ao checkout, rode a partir da raiz.

```bash
python3 coleta-seletiva-api/scripts/validar_api.py --aceite
```

O roteiro roda a suíte, gera o JAR e percorre os oito cenários via HTTP. No
último cenário ele inicia outro processo da aplicação com o mesmo banco
descartável e confere os registros e indicadores. A saída informa o resultado
de cada cenário. O roteiro e a evidência da execução estão em
[Aceite da API-17](docs/evidencias/api-17-aceite.md).

Nos notebooks que usam `codex-build`, execute o roteiro pelo limitador descrito
em [Como contribuir](CONTRIBUTING.md#validações-neste-notebook).

### Iniciar a aplicação

```bash
./mvnw spring-boot:run
```

Aguarde a confirmação de inicialização no terminal. Para encerrar,
pressione `Ctrl+C`.

No Windows, substitua `./mvnw` por `.\mvnw.cmd`, mantendo os argumentos.

### Perfil local de demonstração (atores fictícios)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Com o perfil `demo`, a API cria uma vez um morador, dois pontos e dois operadores
fictícios e habilita `GET /api/v1/demo/atores`, que lista os atores e seus ids.
Para agir como um deles, envie os cabeçalhos `X-Ator-Tipo` e `X-Ator-Id`.
Isso é simulação, não autenticação: use só em ambiente local. Detalhes em
[Contexto de demonstração](docs/CONTRATO_API.md#operadores-e-contexto-de-demonstração-api-09).

## Verificação antes do PR

- [ ] Maven Wrapper executado com Java 21.
- [ ] Testes concluídos com sucesso.
- [ ] Aplicação iniciada e conectada ao PostgreSQL.

## Documentação e evidências da primeira unidade

- [OpenAPI versionado](docs/openapi.yaml) com exemplos HTTP capturados da API.
  Com o servidor rodando, consulte o Swagger UI em
  [localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) e a
  especificação gerada pelo SpringDoc em
  [localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs).
- [Relatório da primeira unidade](docs/evidencias/api-18-entrega.md) com DER,
  decisões, roteiro de aceite, persistência e desempenho.

Com as APIs 13, 16 e 17 integradas, execute da raiz para gerar e medir a carga
de 1.000 solicitações fictícias em um PostgreSQL descartável:

```bash
python3 -B coleta-seletiva-api/scripts/desempenho_api.py
```

O roteiro inicia a aplicação duas vezes no mesmo banco, confere dados e
indicadores e mede o p95 de cinco consultas HTTP. Se já executou o aceite da
API-17 no mesmo checkout e o JAR está atualizado, acrescente
`--usar-jar-existente`. Neste notebook, rode o comando com o limitador indicado
em [Como contribuir](CONTRIBUTING.md#validações-neste-notebook).
