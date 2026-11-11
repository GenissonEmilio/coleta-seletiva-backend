# Coleta Seletiva - Backend

API acadêmica de Programação Web I para conectar moradores, pontos de coleta e operadores de recolhimento de recicláveis.

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
