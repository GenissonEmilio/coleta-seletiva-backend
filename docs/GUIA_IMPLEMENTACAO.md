# Guia de implementação - equipe de três pessoas

## 1. Objetivo e limite desta preparação

Construir manualmente um sistema de coleta seletiva, começando pela API completa da primeira unidade. Esta preparação entrega organização, decisões propostas e issues; as funcionalidades serão programadas pelos integrantes.

Fonte: `TrabalhoWeb01.pdf`, fornecido pela equipe. Os identificadores RF, RN e RNF abaixo são os da especificação. O PDF define os requisitos do produto; escolhas como nomes dos repositórios, divisão de trabalho, formato de branches e ordem das tarefas são propostas de organização da equipe.

**Atenção à nomenclatura:** Etapa 1 do PDF é requisitos, protótipos e modelo de dados; Etapa 2 é banco, API e regras. Por decisão da equipe, este backlog cobre ambas para a **primeira unidade**. Interface funcional e integração ficam para a Unidade 2. Não há prazo confirmado pelo professor neste guia.

## 2. Integrantes e responsabilidades

| Integrante | GitHub | Frente principal na API | Na interface da Unidade 2 | Revisão principal |
| --- | --- | --- | --- | --- |
| Genisson Emilio dos Santos | [@GenissonEmilio](https://github.com/GenissonEmilio) | Fundação Java; materiais; agendar, recusar e cancelar; proteção de cadastros; concorrência; testes essenciais | Administração de materiais e painel de agendamento/cancelamento | Revisa os PRs de Herbert |
| Felipe Menezes de Jesus | [@felipmz](https://github.com/felipmz) | Modelo relacional e migrações; moradores e endereços; pontos e vínculos; execução e reagendamento; reprodução e desempenho | Cadastros de morador/endereço/ponto, vínculos e execução do operador | Revisa os PRs de Genisson |
| Jośe Herbert Andrade silva | [@Jose-Herbert-13](https://github.com/Jose-Herbert-13) | Contrato e erros; consulta pública; operadores/contexto; criar e consultar solicitações; indicadores | Busca pública, formulário e histórico de solicitações e resumo gerencial | Revisa os PRs de Felipe |

Os perfis foram encontrados nas relações de seguindo/seguidores de Genisson. Cada frente inclui **Entity/Model, DTOs Request/Response, Repository, Service, Controller, testes e documentação** dos módulos correspondentes; ninguém fica restrito a uma camada. Revisão cruzada e sessões curtas de explicação ajudam os três a conhecer todo o sistema para a avaliação oral.

Cada integrante tem sete tarefas principais: uma de planejamento e seis de API. Isso equilibra a quantidade inicial; complexidade deve ser revista nas reuniões. Criação de solicitação, execução e concorrência devem ter revisão conjunta.

## 3. Fluxo recomendado

### Rodada 0 - planejar antes do código de negócio

1. Genisson conduz a leitura dos requisitos e registra decisões e dúvidas com os outros dois (`PLAN-01`).
2. Felipe desenha o DER com entidades, cardinalidades, restrições, histórico, cópias de endereço/unidade e versão (`PLAN-02`).
3. Herbert prepara protótipos simples das telas mínimas e confirma os campos de entrada, erros e retornos com a equipe (`PLAN-03`). Protótipos não exigem uma aplicação frontend pronta.
4. Os três revisam o modelo, o fluxo de estados e os contratos propostos antes de iniciar as migrações definitivas.

**Saída:** requisitos rastreados, DER versionado, protótipos acessíveis, decisões aceitas e critérios de aceite entendidos. Rascunhos deste repositório precisam de revisão; não contam como entrega concluída dessas issues.

### Rodada 1 - fundação executável

- Genisson: projeto Java 21/Spring Boot e execução mínima (`API-01`).
- Felipe: migrações PostgreSQL e restrições conforme o DER aprovado (`API-02`).
- Herbert: DTOs de erro, validação, paginação e convenções do contrato (`API-03`).

Fixar versões compatíveis, usar Maven Wrapper, configuração por ambiente e migrações. Não instalar Spring Security/JWT apenas para iniciar: autenticação é extensão opcional. A seleção de ator de demonstração precisa ficar explícita e isolada no ambiente local.

**Saída:** cada integrante consegue subir a mesma base em sua máquina, sem credenciais versionadas e com contrato comum. A migração e os serviços devem convergir antes da integração de funcionalidades dependentes.

### Rodada 2 - primeira fatia completa de uso

1. Implementar moradores/endereços (`API-04`), materiais (`API-05`), pontos (`API-06`) e materiais aceitos (`API-07`).
2. Entregar busca pública paginada (`API-08`) e cadastro/vínculos de operadores e contexto de demonstração (`API-09`).
3. Criar uma solicitação com dois materiais, cópias históricas, protocolo e histórico inicial, sem gravação parcial em caso de erro (`API-10`).
4. Consultar a solicitação com seus itens e histórico (`API-11`).

**Demonstração desta fatia:** cadastrar morador, endereço, papel, plástico e um ponto; encontrar o ponto; solicitar os dois materiais e obter protocolo; tentar material incompatível e comprovar rollback. Esta é uma entrega intermediária; a API da unidade ainda precisa das próximas rodadas.

### Rodada 3 - atendimento e consistência

- Genisson entrega agendamento/recusa/cancelamento (`API-12`), proteção de cadastros e vínculos (`API-14`) e conflitos de versão (`API-15`).
- Felipe entrega início, conclusão, reagendamento e tentativa sem sucesso (`API-13`).
- Herbert entrega resumo gerencial (`API-16`).

Revisar transações e a mesma política de versão em todas as operações. A integração dessas tarefas é necessária antes de declarar concluídos os módulos que dependem delas.

### Rodada 4 - validar e fechar a API da unidade

- Genisson coordena os testes automatizados essenciais e a evidência de rollback/concorrência (`API-17`), com contribuição de testes em cada PR dos colegas.
- Felipe coordena README executável, dados fictícios, documentação de endpoints, reinício/persistência e medição de desempenho (`API-18`). Herbert revisa o contrato e todos executam a reprodução.
- Os três ensaiam os oito cenários da seção 13 do PDF e explicam o vínculo entre código, requisitos e testes.

**Saída:** RF01-RF15, RN01-RN06 e RNFs aplicáveis à API demonstrados, com evidências. RNF02 e as telas funcionais serão validados na Unidade 2; protótipos são preparados agora. Não deixar todos os testes para o fim: esta rodada consolida a integração.

## 4. Organização do código

**Padrão adotado pela equipe:** organização por camadas, com serviços e DTOs agrupados por funcionalidade. O projeto Java fica em `coleta-seletiva-api/`, e o pacote base é `br.edu.ifs.web1.coletaseletiva`.

A árvore abaixo mostra os nomes finais esperados, usando materiais como exemplo de uma funcionalidade completa. As demais funcionalidades seguem a mesma distribuição entre Controller, Service, Repository, Model e DTOs.

```text
coleta-seletiva-api/
  src/main/java/br/edu/ifs/web1/coletaseletiva/
    ColetaSeletivaApiApplication.java
    config/
      OpenApiConfig.java
    controller/
      MaterialController.java
    services/
      morador/
      endereco/
      material/
        MaterialService.java
      ponto/
      operador/
      solicitacao/
      indicador/
      paginacao/
        PaginacaoService.java
    repository/
      MaterialRepository.java
    model/
      MaterialModel.java
    dto/
      common/
        PaginaResponse.java
        PaginacaoRequest.java
        ErroResponse.java
        ErroCampoResponse.java
      morador/
      endereco/
      material/
        MaterialRequest.java
        MaterialResponse.java
      ponto/
      operador/
      solicitacao/
      indicador/
    exception/
      GlobalExceptionHandler.java
      RegraNegocioException.java
  src/main/resources/
    application.properties
    db/migration/
  src/test/java/br/edu/ifs/web1/coletaseletiva/
```

### Responsabilidade de cada pacote

| Pacote | Responsabilidade e convenção |
| --- | --- |
| `config` | Configurações da aplicação, como OpenAPI. |
| `controller` | Endpoints REST, parâmetros HTTP, validação da entrada e encaminhamento ao Service. Nome: `<Funcionalidade>Controller`. |
| `services/<funcionalidade>` | Regras de negócio, transações e conversão entre Request, Model e Response. Nome: `<Funcionalidade>Service`. |
| `repository` | Acesso ao banco, consultas, filtros e paginação. Nome: `<Funcionalidade>Repository`. |
| `model` | Entidades persistidas com JPA. Nome: `<Funcionalidade>Model`; relacionamentos e restrições seguem o DER. |
| `dto/<funcionalidade>` | Objetos de entrada e saída daquela funcionalidade, com sufixos `Request` e `Response`. |
| `dto/common` | DTOs reutilizados por funcionalidades diferentes, como a resposta de paginação. |
| `exception` | Exceções da aplicação e tratamento global de erros. |

Os nomes dos pacotes usam letras minúsculas e não têm acentos. Manter `services`, conforme a estrutura iniciada pela equipe. Controllers, Repositories e Models permanecem em suas camadas; o agrupamento por funcionalidade fica nos serviços e DTOs.

O padrão inicial é uma **classe concreta com `@Service` por funcionalidade**. A separação entre interface e implementação pode ser introduzida se houver uma necessidade definida ou orientação do professor; não é uma exigência deste padrão inicial. A aplicação permanece uma API monolítica. Ponto-material e operador-ponto precisam de vínculos explícitos quando houver atributos/restrições próprios.

### Convenção Request/Response

- **`<Funcionalidade>Request`** representa dados recebidos pela API. Exemplo: `MaterialRequest`. Colocar nele as validações de entrada aplicáveis, como `@NotBlank`, `@Positive` e `@Size`.
- **`<Funcionalidade>Response`** representa os dados devolvidos pela API. Exemplo: `MaterialResponse`, com os campos previstos no contrato e adequados ao contexto público ou restrito.
- Quando operações tiverem campos ou regras de entrada diferentes, criar Requests específicos, como `SolicitacaoAgendamentoRequest` e `SolicitacaoConclusaoRequest`.
- Quando telas ou contextos precisarem de representações diferentes, usar nomes que indiquem o conteúdo, como `SolicitacaoResumoResponse` e `SolicitacaoDetalheResponse`.
- `Form` e `DTO`, usados como sufixos no exemplo do professor, correspondem respectivamente a entrada e saída. **Nesta equipe, usar `Request` e `Response` como sufixos** para manter um padrão único.
- Controllers recebem Requests e devolvem Responses. A conversão entre DTOs e Models fica no Service; as regras de negócio também permanecem no Service. O contrato HTTP é definido pelos DTOs, sem devolver diretamente o grafo de entidades JPA.
- DTOs não são entidades do banco e não recebem Repository próprio. Podem ser classes ou records, conforme os dados e validações necessários.

### DTO comum de paginação

A implementação de `PaginaResponse<T>` pertence a `dto/common` e contém `content`, `page`, `size`, `totalElements` e `totalPages`, conforme [CONTRATO_API.md](CONTRATO_API.md). `PaginacaoRequest`, no mesmo pacote, representa os parâmetros de entrada e suas restrições.

Exemplos de uso: `PaginaResponse<MaterialResponse>`, `PaginaResponse<PontoResponse>` e `PaginaResponse<SolicitacaoResumoResponse>`. Cada módulo deve reutilizar essa estrutura, em vez de criar um envelope de paginação próprio. O Controller valida `page` e `size`; o Service monta a consulta e a resposta; o Repository aplica filtros e paginação no banco. Filtros e contexto do ator precisam participar da consulta antes da paginação.

`PaginacaoService`, em `services/paginacao`, transforma o Request em `Pageable`, rejeita valores inválidos e acrescenta o ID como desempate da ordenação. Os serviços dos módulos podem recebê-lo por construtor e chamar `criarPageable(entrada, ordem)` antes de consultar o Repository. O método `PaginaResponse.de(pagina, conversor)` permite converter Models para Responses preservando os metadados da página.

As respostas de erro também ficam em `dto/common`: `ErroResponse` contém `codigo`, `mensagem` e `errosCampos`, com cada campo representado por `ErroCampoResponse`. As exceções, seus códigos e o `GlobalExceptionHandler` ficam em `exception`. A configuração OpenAPI fica em `config/OpenApiConfig.java`.

### Arquivos temporários para manter as pastas no Git

O Git registra arquivos. Os pacotes ainda sem implementação usam **marcadores vazios `Teste*.java`**:

- `services/<funcionalidade>/Teste<Funcionalidade>Service.java`;
- `dto/<funcionalidade>/Teste<Funcionalidade>Request.java` e `Teste<Funcionalidade>Response.java`;

O antigo `dto/common/TestePaginaResponse.java` e o marcador `exception/TesteException.java` foram substituídos pelas implementações reais de paginação e erros da API-03.

Esses arquivos têm apenas a declaração do pacote e da classe. Não são serviços, endpoints, entidades ou testes automatizados; não possuem anotações do Spring/JPA, validações ou regras de negócio. Os marcadores existentes em `controller`, `repository` e `model` seguem a mesma finalidade. O marcador de serviço foi movido para `services/material`, seguindo o agrupamento adotado.

Ao implementar uma funcionalidade, substituir ou remover os marcadores do pacote correspondente e criar as classes com os nomes finais. **Esta preparação da estrutura não conclui as issues das funcionalidades.** Os testes automatizados ficam em `src/test/java`, acompanhando os pacotes das classes testadas, com nomes como `MaterialServiceTest` e `MaterialControllerTest`. As migrações SQL em `src/main/resources/db/migration` serão criadas nas tarefas de banco correspondentes.

### Validação automática da API

O workflow `.github/workflows/api-tests.yml` executa os testes das PRs que alteram a API, com Java 21 e PostgreSQL 17 em ambiente descartável. O processo de build fica em um container limitado a 2 GiB de RAM e duas CPUs; o banco tem limite próprio de 512 MiB e uma CPU. As credenciais são exclusivas da execução de teste, sem utilizar o `.env` dos integrantes. Os relatórios ficam nos artefatos da execução por sete dias.

## 5. Decisões técnicas propostas para revisão

| Assunto | Proposta | Por que importa |
| --- | --- | --- |
| Persistência | PostgreSQL e Flyway; JPA com esquema validado após migração | Banco reproduzível; sem depender de criação automática de tabelas |
| Contrato | JSON; prefixo `/api/v1`; DTOs Request/Response; documentação OpenAPI ou Postman | Evita divergências entre os repositórios |
| Quantidades | `BigDecimal`; unidade no material e cópia no item; valor integral para peças | Não misturar kg/litro/peça nem perder precisão |
| Histórico | Registro relacionado à solicitação; incluir criação e cada mudança/tentativa | Rastreabilidade e demonstração dos cenários |
| Concorrência | Versão otimista na solicitação; versão esperada em todas as mutações; conflito 409 | Evita sobrescrever atendimento com dados antigos |
| Tempo | Eventos com offset e relógio injetável; comparar instantes | Agendamento futuro e testes determinísticos |
| Ator no protótipo | Seleção de atores fictícios apenas em perfil local; contexto no backend | Demonstra os fluxos sem alegar autenticação |
| Segurança | DTO público só com dados de pontos/materiais; variáveis de ambiente | Sem vazar dados pessoais ou segredos |
| Frontend | Proposta: HTML, CSS e JavaScript modular, sujeitos à orientação do professor | Permite escrever a interface manualmente; framework não é exigido pelo PDF |

As versões de Spring Boot e bibliotecas ainda não foram escolhidas. A compatibilidade com Java 21 deve ser verificada e registrada em `API-01`. Não adicionar mapas, login JWT, notificações, upload, roteirização, pagamentos ou aplicativo nativo antes de atender o obrigatório.

## 6. Regras que não podem ficar para depois

- A solicitação exige ponto ativo com recolhimento domiciliar e todos os materiais ativos, aceitos, sem repetição, com quantidades positivas e limites respeitados.
- Endereço deve pertencer ao morador. Guardar cópia do endereço e da unidade no atendimento, preservando histórico após mudanças cadastrais.
- Correção de itens/endereço somente em `SOLICITADA`, com nova validação; o contrato proposto inclui essa operação.
- Criar solicitação com itens/histórico e alterar situação com histórico exige uma única transação.
- Não excluir fisicamente material, ponto ou morador ligado a solicitação. Bloquear inativação do ponto e remoção de vínculo enquanto houver atendimento aberto afetado.
- Concorrência também importa entre criar solicitação e alterar disponibilidade/vínculos. Definir ordem de bloqueio ou isolamento na revisão das regras; a versão da solicitação, sozinha, não protege cadastros relacionados.
- Na conclusão, informar efetivo de todos os itens, com valores não negativos e pelo menos um positivo; peças continuam inteiras.
- Histórico guarda situação anterior/nova, data, responsável, motivo e tentativa quando aplicável.
- Resumo agrupa quantidades efetivas por material/unidade e período de conclusão; somente atendimentos concluídos contribuem para os totais recolhidos.

Transições: `SOLICITADA -> AGENDADA | RECUSADA | CANCELADA`; `AGENDADA -> EM_RECOLHIMENTO | CANCELADA`; reagendamento mantém `AGENDADA`; `EM_RECOLHIMENTO -> CONCLUIDA | AGENDADA` (falha justificada e nova data futura). Estados finais não admitem novas transições. `CONCLUIDA` é a proposta de enum JSON sem acento para o estado “CONCLUÍDA” do PDF.

## 7. Rotina de equipe e GitHub

- Usar uma issue por resultado verificável e uma branch curta por issue. As dependências estão no [backlog](BACKLOG.md).
- Fazer uma reunião inicial e encontros curtos de integração a cada rodada; cada pessoa informa o que entrega, o que depende de alguém e o próximo PR.
- Não reservar todos os Controllers para uma pessoa ou todos os testes para outra. Autor entrega seus próprios testes; coordenador consolida cobertura.
- Alterações em DTO, enum, filtro ou erro exigem atualização do contrato no mesmo PR. Na Unidade 2, registrar a versão/commit do backend usada pelo frontend.
- Abrir PRs pequenos e revisados no ciclo Felipe -> Genisson -> Herbert -> Felipe. Se o revisor estiver bloqueado, o terceiro pode revisar.
- A responsabilidade indicada por label/corpo da issue vale mesmo quando o GitHub ainda não permite atribuição formal por convite pendente. Após aceite, atualizar o assignee sem mudar a divisão.
- Datas de milestone devem ser preenchidas após orientação do professor; não foram inventadas datas neste planejamento.

## 8. Próxima unidade: interface e integração

Manter o frontend em repositório separado. Usar os protótipos e o contrato da API como entrada. Divisão por telas está na tabela dos integrantes; todos continuam responsáveis por estados de carregamento, vazio, erro e sucesso dos seus fluxos.

Validar 360 px e 1366 px sem rolagem horizontal; indicar obrigatoriedade; erros próximos aos campos; confirmação de cancelamento. Testar ponta a ponta: consulta pública -> cadastro/endereço -> solicitação com itens -> acompanhamento -> operador agenda/inicia/conclui -> resumo. Credenciais e regras continuam no backend.

## 9. Checklist de entrega da primeira unidade

- [ ] Requisitos revistos, DER e protótipos versionados.
- [ ] Java 21/Spring Boot/PostgreSQL configurados e reproduzidos pelos três.
- [ ] RF01-RF15 e RN01-RN06 cobertos e rastreados.
- [ ] Respostas HTTP 201/400/404/409 e paginação máxima de 100 documentadas.
- [ ] Migrações, dados fictícios e instruções sem segredos.
- [ ] Testes essenciais, integração, rollback e conflito concorrente automatizados.
- [ ] Reinício preserva dados e medição com 1.000 solicitações registra máquina, ferramenta e resultados (meta p95 <= 2 s).
- [ ] OpenAPI ou coleção Postman utilizável e exemplos de sucesso/erro.
- [ ] Oito cenários do PDF demonstrados por roteiro e evidências.
- [ ] Histórico de participação individual e explicação cruzada do código.

Todos os itens estão inicialmente pendentes de desenvolvimento/revisão. A criação do repositório ou das issues não os conclui.
