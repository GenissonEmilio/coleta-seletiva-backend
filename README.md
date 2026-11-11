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

Execute os comandos na pasta `coleta-seletiva-api`, onde está o `pom.xml`,
usando o mesmo terminal em que configurou as variáveis.

### Conferir o ambiente

```bash
./mvnw --version
```

A saída deve indicar Java 21 e a versão do Maven utilizada pelo projeto.

### Executar os testes

```bash
./mvnw test
```

O comando deve concluir com `BUILD SUCCESS`. Os testes que carregam a
aplicação dependem do PostgreSQL disponível e das variáveis configuradas.

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
