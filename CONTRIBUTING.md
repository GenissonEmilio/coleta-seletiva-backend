# Como contribuir

1. Escolha uma issue cujas dependências estejam concluídas. Atualize seu andamento e combine mudanças de contrato com os outros dois integrantes.
2. Atualize `main` e crie uma branch curta, como `feat/12-cadastro-materiais`, `fix/18-quantidade-invalida` ou `docs/2-modelo-dados`. Use o número real da issue.
3. Implemente uma funcionalidade completa: entrada/DTO, serviço/regras, persistência, endpoint, documentação e testes relevantes.
4. Faça commits com seu próprio nome e e-mail Git, descrevendo o comportamento alterado. Não use uma conta compartilhada: o histórico individual faz parte da avaliação.
5. Abra PR para `main`, explique o comportamento, informe requisitos atendidos e validação realizada. Use `Closes #N` para a issue concluída.
6. Peça revisão de outro integrante. Ajuste o que for necessário e integre sem deixar `main` quebrada. Revisão por PR é um acordo da equipe; proteção automática de branch ainda não foi configurada.

## Definição de pronto

- Critérios de aceite da issue atendidos e requisitos rastreados.
- Regras no Service, Bean Validation na entrada e DTOs sem exposição acidental de entidades ou dados pessoais.
- Migrações revisadas e aplicáveis a banco novo; não reescrever migrações já compartilhadas.
- Testes relevantes passando; exemplo de chamada e erro documentados.
- Nenhum segredo ou dado pessoal real no commit.
- PR revisado por alguém além do autor e evidência registrada na issue.

## Acordos técnicos

- Seguir a [organização por camadas e as convenções da equipe](docs/GUIA_IMPLEMENTACAO.md#4-organização-do-código): serviços em `services/<funcionalidade>`, DTOs em `dto/<funcionalidade>` e DTOs compartilhados em `dto/common`.
- Nomear entradas com o sufixo `Request` e saídas com `Response`. Usar `PaginaResponse<T>` como envelope comum de paginação quando implementado na API-03. Ao implementar o módulo, substituir ou remover os marcadores vazios `Teste*.java` correspondentes.
- JSON e caminhos de API em português, sem acentos; proposta inicial em [CONTRATO_API.md](docs/CONTRATO_API.md).
- Use `BigDecimal` para quantidades; não use `double` para peso/volume. Unidade de peça exige valor integral.
- Use datas de eventos com offset, relógio injetável nos testes e comparação por instante.
- Uma transação cobre solicitação, itens e histórico. Toda alteração de situação verifica a versão recebida.
- Não excluir cadastros com vínculo histórico; revisar as restrições antes de inativar ou remover vínculos.
- Mantenha dados fictícios e isole a seleção de ator de demonstração em um perfil local. Selecionar um ator não autentica ninguém.

## Validações neste notebook

Antes de builds e testes pesados, consulte `/proc/meminfo`, `/proc/pressure/memory` e processos ativos. Reserve pelo menos 3 GiB para o desktop; abaixo de 4 GiB disponíveis, serialize tarefas pesadas.

Use `/home/genissonemilio/.local/bin/codex-build -- comando argumentos` para builds e testes locais pesados de processos filhos, com no máximo dois workers. Não execute o mesmo build sem limites se o executor recusar. Esses caminhos são específicos do notebook de Genisson; os demais integrantes devem definir limites equivalentes adequados às suas máquinas.

Ao encerrar a validação, pare servidores temporários criados para ela e remova somente resíduos comprovadamente regeneráveis do trabalho. Preserve banco, volumes, instalações e arquivos pessoais. Confira espaço livre antes e depois; não faça limpeza global de caches.
