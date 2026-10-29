# ADR 004: Eventos de domínio

- **Status:** Proposto (passa a Aceito com a aprovação do grupo no PR)
- **Data:** 2026-10-29
- **Tarefa:** [KAN-74 (T3.12)](https://arktech-lvw.atlassian.net/browse/KAN-74)

## Contexto

- A ordem de serviço registrava um evento quando o orçamento era enviado, aprovado, rejeitado ou reaberto e quando a OS era concluída ou cancelada. Só a aprovação tem ouvinte: o `inventory` dá baixa no estoque das peças do orçamento (T3.6), e essa baixa precisa acontecer na mesma transação da mudança de status ([ADR 001](001-database.md)).
- A regra anterior do CONTRIBUTING mantinha o domínio sem Spring: o agregado guardava os eventos numa lista `@Transient`, e o caso de uso, depois do `save`, publicava pelo `ApplicationEventPublisher` o que o `pullEvents()` devolvesse.
- Na revisão do [PR #42](https://github.com/ArkTech-LVW/workshop-service/pull/42) (KAN-73), o cancelamento repetia esse código. O grupo combinou deixar a publicação automática, como o [PR #44](https://github.com/ArkTech-LVW/workshop-service/pull/44) (KAN-31) já fazia.
- Os eventos da OS eram records aninhados numa interface selada do `domain/model` (`ServiceOrderEvent`), que outro módulo não pode importar. Por isso, a aprovação publicava dois eventos para o mesmo fato: o `QuoteApproved` do agregado, que ninguém escutava, e o `QuoteApprovedEvent`, que o `ApproveQuoteUseCase` montava com as peças e o usuário e que o `inventory` escuta.

## Opções consideradas

### Quem publica

- **O caso de uso, com o `ApplicationEventPublisher` (regra anterior):** o domínio fica sem nenhuma dependência do Spring, mas todo caso de uso repete o `pullEvents().forEach(...)`, e quem esquece essa linha perde o evento sem nenhum erro.
- **O Spring Data, no `save`, com `AbstractAggregateRoot`:** já vem pronto, mas a entidade já estende a `BaseEntity`, e o Java não tem herança múltipla.
- **O Spring Data, no `save`, com `@DomainEvents` e `@AfterDomainEventPublication`:** é o mesmo mecanismo do `AbstractAggregateRoot`, só que declarado no próprio agregado. Em troca, o domínio passa a depender de duas anotações.

### Evento que outro módulo escuta (`QuoteApprovedEvent`)

- **Manter no caso de uso:** separa os eventos de domínio (internos ao módulo, saem no `save`) dos eventos de integração (contrato no `shared`, montados e publicados pelo caso de uso). Mantém duas formas de publicar e dois eventos para o mesmo fato.
- **Registrar no agregado:** o agregado registra o `QuoteApprovedEvent` no lugar do `QuoteApproved`. Fica uma forma só de publicar, mas o agregado passa a montar o evento com os dados de que o outro módulo precisa (as peças e quem aprovou).

### Quais eventos existem

- **Um evento para cada transição, à espera de ouvintes futuros:** o agregado e os testes carregam código que ninguém usa, e o formato de cada evento é decidido antes de existir quem dependa dele.
- **Só os eventos que têm ouvinte:** o evento nasce junto com o primeiro ouvinte, já com os dados de que ele precisa.

### Onde ficam os eventos

- **Interface selada por agregado no `domain/model` (`ServiceOrderEvent`):** dá uma lista tipada e um `switch` exaustivo, mas nenhum outro módulo enxerga os eventos. Um record de fora do pacote também não pode implementar a interface, porque, fora de um módulo Java nomeado, uma interface selada só aceita implementações do próprio pacote.
- **Os internos no `domain/model` e os escutados por outro módulo no `shared`:** são dois lugares e duas regras para os eventos do mesmo agregado, e um evento precisa mudar de lugar quando ganha o primeiro ouvinte em outro módulo.
- **Um record por evento no `shared/event/<módulo dono>`:** um lugar e uma regra só. Em troca, todo evento fica visível para todos os módulos.

### Tipo da lista do agregado

- **Uma classe base para os eventos:** um record não pode estender classe, e os eventos não têm nada em comum que justifique uma.
- **Uma interface comum (`DomainEvent`), sem métodos:** dá um tipo à lista, mas é um tipo sem conteúdo nenhum, que existe só para isso.
- **`List<Object>`, como no `AbstractAggregateRoot`:** não exige nenhum tipo novo. A lista aceita qualquer objeto, mas só os métodos de negócio do agregado a preenchem.

## Decisão

1. **O agregado registra os eventos, e o Spring Data os publica no `save`.** Os eventos ficam numa lista `@Transient` do agregado (`List<Object>`, como no `AbstractAggregateRoot`), exposta por um método `@DomainEvents` (`domainEvents()`) e limpa por um `@AfterDomainEventPublication` (`clearDomainEvents()`). O Spring Data publica os eventos quando o `save` é chamado, de forma síncrona e dentro da transação do caso de uso. O caso de uso não publica nada à mão.
2. **Exceção: o caso de uso publica quando a publicação automática não é possível.** Isso vale quando o fato não passa pelo `save` de um agregado: por exemplo, uma alteração em lote feita por consulta, sem carregar o agregado, ou um caso de uso que não grava nenhum agregado. Nesses casos, o caso de uso publica o evento pelo `ApplicationEventPublisher`, depois de gravar e dentro da mesma transação. O evento segue as outras regras desta decisão, e o PR explica por que o `save` não resolve. O teste unitário confere a publicação passando ao caso de uso um publicador que guarda os eventos (ex.: `publishedEvents::add`).
3. **`@DomainEvents` e `@AfterDomainEventPublication` são a única exceção de Spring no `domain`**, e o `LayerDependencyTest` libera só essas duas anotações.
4. **Cada evento é um `record` próprio no `shared/event/<módulo dono>`, sem classe nem interface comum.** O nome termina em `Event` (ex.: `QuoteApprovedEvent`). Ele leva, em tipos simples (ids e quantidades), o que os ouvintes precisam. O `shared/event` não pode ficar no módulo dono, porque um módulo não importa o outro, e fica fora das camadas do `shared`, porque o `domain` do módulo dono também o importa. Não existe interface selada por agregado.
5. **Um evento só existe quando tem ouvinte.** Ele é criado junto com o primeiro `@EventListener` que o escuta e removido quando o último ouvinte deixa de existir. Por isso, os eventos de envio, rejeição e reabertura do orçamento e de conclusão e cancelamento da OS, que ninguém escutava, foram removidos.
6. **O `QuoteApprovedEvent` sai do agregado.** O `approveQuote` o registra com o id da OS, as peças e quem aprovou, e o `ApproveQuoteUseCase` deixa de injetar o `ApplicationEventPublisher`. O `QuoteApproved` deixa de existir. Hoje ele é o único evento da OS.
7. **Nos testes unitários, o repositório em memória imita o Spring Data:** recebe quem publica e solta os eventos no `save` (ver `InMemoryServiceOrderRepository`). O teste do caso de uso que publica um evento confere o que foi publicado.

## Consequências

- O caso de uso **precisa** chamar `save` para os eventos saírem. Dentro da transação, o Hibernate gravaria a alteração mesmo sem o `save`, mas o evento se perderia sem nenhum aviso. Chamar o `save` explicitamente já era regra do CONTRIBUTING.
- Um ouvinte síncrono que falha desfaz a transação inteira. Na aprovação, isso é o que a T3.6 quer: sem estoque, o status não muda. Um efeito que não pode desfazer o caso de uso (ex.: mandar um e-mail) vai precisar de `@TransactionalEventListener(phase = AFTER_COMMIT)` ou de uma fila.
- Os testes unitários dependem de o repositório em memória imitar a publicação. Se ele divergir do Spring Data, o teste unitário passa e a aplicação falha. Os testes de integração (`@RecordApplicationEvents`) conferem a publicação real.
- A exceção do item 2 traz de volta o risco da regra anterior: se o caso de uso esquecer de publicar, o evento se perde sem nenhum erro. Por isso, ela vale só quando o `save` do agregado não resolve.
- O domínio depende de duas anotações do Spring Data. Trocar de framework obrigaria a trocar também o mecanismo de publicação.
- Todo evento é um contrato que qualquer módulo pode escutar. Mudar os campos de um evento pode quebrar ouvintes em outros módulos: uma mudança no `QuoteApprovedEvent`, por exemplo, mexe no `serviceorder` e no `inventory` ao mesmo tempo.
- Sem interface selada, não há `switch` exaustivo sobre os eventos de um agregado. Cada ouvinte escuta o tipo do evento que lhe interessa.
- Quem precisar reagir a um fato que ainda não tem evento (ex.: avisar o cliente quando a OS for concluída) cria, na mesma tarefa, o `record`, o registro no método de negócio do agregado e o ouvinte.
