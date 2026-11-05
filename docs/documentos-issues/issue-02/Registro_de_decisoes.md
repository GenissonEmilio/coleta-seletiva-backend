# Registro de decisões - DER (issue #2)

Registro das decisões do modelo de dados (issue #2, PLAN-02), com justificativa e consequência de cada uma. Modelo correspondente: [DER.md](../../DER.md).

**Base:** `TrabalhoWeb01.pdf`. Cada decisão indica se veio do PDF ou se foi escolha da equipe. Escolhas da equipe só existem quando o PDF não define o assunto e o sistema precisa de uma resposta.

Legenda:
- **PDF**: exigido pelo enunciado.
- **Necessário**: o PDF não cita, mas sem isso uma exigência do PDF não funciona.
- **Escolha**: o PDF deixa em aberto; a equipe escolheu entre opções.

---

## 1. Escolhas feitas pelo Felipe

### 1.1 Solicitação guarda a cópia do endereço e também o `endereco_id`
- **Tipo:** Escolha.
- **O que o PDF diz:** RN04 e seção 10 exigem a **cópia** do endereço na solicitação. Não citam vínculo com a tabela de endereços.
- **Opções:** (a) só a cópia; (b) cópia + FK `endereco_id`.
- **Escolhido:** (b).
- **Por quê:** permite saber de qual endereço cadastrado a cópia veio e validar que ele pertence ao morador.
- **Consequência:**
  - O endereço não pode ser excluído depois de usado, porque a FK impede. Por isso o endereço ganhou o campo `ativo`.
  - A cópia continua obrigatória: o `endereco_id` não substitui a RN04.

### 1.2 Chave primária composta nas tabelas de vínculo e no item
- **Tipo:** Escolha.
- **Tabelas:** `ponto_material (ponto_id, material_id)`, `operador_ponto (operador_id, ponto_id)`, `item_solicitacao (solicitacao_id, material_id)`.
- **O que o PDF diz:** RF05 ("não repetir o mesmo vínculo"), RN01 ("o mesmo material não pode aparecer duas vezes na solicitação"), seção 10 ("vínculo único").
- **Opções:** (a) PK composta pelo par; (b) `id` próprio + UNIQUE no par.
- **Escolhido:** (a).
- **Por quê:** usa só os campos que o PDF descreve, e a própria PK já impede repetição.
- **Consequência:** no JPA essas entidades precisam de chave composta (`@EmbeddedId` ou `@IdClass`). É um pouco mais trabalhoso que um `@Id` simples.

### 1.3 Responsável no histórico como `responsavel_tipo` + `responsavel_id`
- **Tipo:** Escolha.
- **O que o PDF diz:** RN03 manda gravar o "responsável" de cada alteração. Os atores que alteram são morador (cancela), operador (agenda, recusa, executa) e administrador.
- **Opções:** (a) tipo + id; (b) FKs opcionais `morador_id` e `operador_id`; (c) nome em texto.
- **Escolhido:** (a).
- **Por quê:** cobre os três atores com duas colunas, inclusive o administrador, que não tem tabela própria.
- **Consequência:** `responsavel_id` não tem FK, então o banco não garante que o id existe. O Service precisa garantir isso.

### 1.4 Informação da tentativa: só `agendamento_novo`
- **Tipo:** Escolha.
- **O que o PDF diz:** seção 10 ("informações da tentativa"); RF14 (reagendar ou registrar falha com justificativa e nova data futura; "cada tentativa e alteração preserva o histórico"). Não diz quais campos.
- **Opções:** (a) só a nova data; (b) nova data + número da tentativa; (c) data anterior + nova + número.
- **Escolhido:** (a).
- **Por quê:**
  - A justificativa já vai em `motivo`.
  - A data anterior está na linha anterior do histórico.
  - O número da tentativa pode ser contado. É o mínimo que a RF14 exige.
- **Consequência:** para mostrar "tentativa 2" ou "data anterior", a consulta precisa olhar as linhas anteriores do histórico.

### 1.5 E-mail do operador único
- **Tipo:** Escolha.
- **O que o PDF diz:** seção 10 ("Operador: ID, nome, e-mail"). A unicidade só é exigida para o morador (RF01).
- **Escolhido:** UNIQUE.
- **Por quê:** mesmo raciocínio do morador: e-mail identifica uma pessoa.
- **Consequência:** cadastrar dois operadores com o mesmo e-mail retorna erro (409).

### 1.6 Operador com campo `ativo`
- **Tipo:** Necessário.
- **O que o PDF diz:** RN03 manda inativar em vez de excluir "materiais, pontos ou moradores". Não cita operadores.
- **Escolhido:** incluir `ativo`.
- **Por quê:** o histórico guarda o id do operador como responsável. Se ele fosse excluído, o histórico apontaria para alguém que não existe.
- **Consequência:** operador nunca é excluído, só inativado.

---

## 2. Decisões técnicas do modelo

### 2.1 `id` e `protocolo` na solicitação
- **Tipo:** PDF (seção 10: "ID, protocolo único"; RF08: "gerar protocolo único").
- **Por quê os dois:** o `id` liga as tabelas (FKs pequenas e estáveis). O `protocolo` é o número que o morador vê.
- **Consequência:** o formato do protocolo pode mudar sem mexer nas FKs. O formato ainda está pendente.

### 2.2 Cópia do endereço em colunas separadas (`end_*`)
- **Tipo:** PDF (a cópia) + Escolha (o formato).
- **Campos:** os mesmos da RF02 (logradouro, número, sem número, bairro, município, UF, CEP, complemento).
- **Opções:** colunas separadas, um texto único, `jsonb` ou tabela separada.
- **Por quê colunas:** mantém o modelo relacional e permite filtrar por bairro ou município.
- **Consequência:** a solicitação tem 8 colunas a mais. Se o morador editar o endereço, as solicitações antigas não mudam (RN04).

### 2.3 Cópia da unidade no item
- **Tipo:** PDF (RN04: "guardar uma cópia [...] da unidade dos itens").
- **Consequência:** se a unidade do material mudar, os itens antigos continuam com a unidade original. O resumo (RF15) não mistura unidades.

### 2.4 Unidades: KG, LITRO, UNIDADE
- **Tipo:** Necessário.
- **O que o PDF diz:** RN02 fala em "peso ou volume" (decimal) e "unidade de peça" (inteiro). RF15 cita "kg com unidades".
- **Por quê:** um valor para cada caso da RN02: peso (KG), volume (LITRO) e peça (UNIDADE).
- **Consequência:** outra unidade (ex.: m³) exige mudar o CHECK com uma nova migração.

### 2.5 Quantidades em `numeric(12,3)`
- **Tipo:** Necessário (RN02: "decimal para peso ou volume").
- **Por quê não `double`:** `double` tem erro de arredondamento (0,1 + 0,2 ≠ 0,3). `numeric` é exato.
- **Consequência:** no Java o tipo é `BigDecimal`, como o CONTRIBUTING já pede.

### 2.6 Peça só com valor inteiro (CHECK no item)
- **Tipo:** PDF (RN02: "valor inteiro para unidade de peça").
- **Como:** se `unidade = 'UNIDADE'`, a quantidade precisa ser igual a `trunc(quantidade)`.
- **Consequência:** o banco rejeita 2,5 garrafas mesmo que o Service falhe.

### 2.7 Quantidade efetiva nula até a conclusão
- **Tipo:** Necessário (RF13 admite zero para item não recolhido. RF15 não pode tratar estimado como efetivo).
- **Por quê:** nulo significa "ainda não informado". Zero significa "informado e não recolhido". São coisas diferentes.
- **Consequência:** o resumo (RF15) soma só itens de solicitações CONCLUIDA.

### 2.8 Datas em `timestamptz`
- **Tipo:** PDF (RNF05: "datas de eventos devem incluir fuso ou offset").
- **Por quê:** `timestamp` guarda o horário do relógio. `timestamptz` guarda o instante exato.
- **Consequência:** no Java usar `OffsetDateTime` ou `Instant`, nunca `LocalDateTime`. Comparar "data futura" (RF10) fica correto em qualquer fuso.

### 2.9 Datas da solicitação: `criada_em`, `agendada_para`, `concluida_em`
- **Tipo:** PDF (RF08 data de criação; RF10 data e hora do agendamento; RF13 data da conclusão).
- **Removido:** `atualizada_em`, porque o PDF não pede. As alterações já ficam no histórico.

### 2.10 Versão na solicitação
- **Tipo:** PDF (seção 10: "versão"; RN06: conflito em atualização concorrente).
- **Como:** coluna `versao` + `@Version` no JPA (lock otimista).
- **Consequência:** se duas pessoas alterarem a mesma solicitação ao mesmo tempo, a segunda recebe 409.

### 2.11 Situação como `varchar` + CHECK
- **Tipo:** Escolha (o PDF define os valores, não o tipo).
- **Opções:** `varchar` + CHECK ou enum nativo do PostgreSQL.
- **Por quê `varchar`:** funciona direto com `@Enumerated(EnumType.STRING)` no JPA. Adicionar um valor é só alterar o CHECK.
- **Valores:** SOLICITADA, AGENDADA, RECUSADA, CANCELADA, EM_RECOLHIMENTO, CONCLUIDA (sem acento, conforme o guia da equipe).

### 2.12 Endereço do ponto embutido
- **Tipo:** PDF permite (seção 10: "endereço do ponto pode ser embutido no cadastro").
- **Por quê:** evita uma tabela extra. Colunas separadas porque a RF06 filtra por município e bairro.
- **Consequência:** se o ponto mudar de endereço, edita-se o próprio ponto.

### 2.13 `ativo` em morador, material, ponto e endereço
- **Tipo:** PDF (RN03 e RF03: inativar em vez de excluir; seção 10: "situação" em material e ponto). Endereço: Necessário (ver 1.1).
- **Consequência:**
  - material inativo não entra em nova solicitação (RF03);
  - ponto inativo não aparece como disponível (RF04).

### 2.14 FKs com `ON DELETE RESTRICT`
- **Tipo:** Necessário (RN03: não excluir fisicamente cadastros vinculados; RNF05: chaves estrangeiras).
- **Consequência:** o banco bloqueia o DELETE de qualquer registro referenciado, mesmo que o código tente.

### 2.15 Índices
- **Tipo:** PDF (RNF04: "índices coerentes com os filtros").
- **Base:** filtros do RF06 (nome, município, bairro, material) e do RF09 (período, situação, ponto). Também o RF15 (por material).
- **Por quê listar FKs:** o PostgreSQL não cria índice automaticamente em FK.
- **Consequência:** consultas mais rápidas e inserções um pouco mais lentas, aceitável para o volume do projeto.

### 2.16 Regras que ficam no Service
- **Tipo:** PDF (RNF01: "manter as regras de negócio no serviço").
- **Exemplos:** compatibilidade ponto/material (RN01), transições, data futura, ao menos uma quantidade efetiva positiva, bloqueios da RN05.
- **Por quê:** essas regras dependem de outras tabelas ou do momento atual, e um CHECK não resolve.
- **Consequência:** essas regras precisam de testes automatizados (RNF08).

---

## 3. Removido por não estar no PDF

| Campo | Motivo da remoção |
| --- | --- |
| `morador.criado_em` | O PDF não pede data de cadastro do morador |
| `solicitacao.atualizada_em` | O PDF não pede. As mudanças ficam no histórico |
| `id` em ponto_material, operador_ponto e item | Substituído pela PK composta (1.2) |
| `historico.numero_tentativa` e `agendamento_anterior` | Ver 1.4 |

---

## 4. Pendentes

- [ ] `sem_numero` no endereço do ponto: incluído para ficar igual ao endereço do morador, mas o PDF não exige. Manter ou remover?
- [ ] Tamanhos dos `varchar`: valores propostos no DER, sem base no PDF.
- [ ] Formato e geração do protocolo: o PDF só exige que seja único.
- [ ] Aprovação do modelo pelos três integrantes antes da migração V1.
