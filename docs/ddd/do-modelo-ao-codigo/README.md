# Do modelo ao código

Esta pasta liga cada elemento das abas de Event Storming do [`workshop-ddd.drawio`](../workshop-ddd.drawio) ao código que o implementa. É a resposta ao item "Relação entre a documentação e a implementação realizada no código" do enunciado. As abas ficam sem termos de código, e a ligação com o código fica toda aqui. Os termos do domínio, com o nome de cada um no código, estão no [glossário](../linguagem-ubiqua.md).

## Fluxos

Um arquivo por aba de Event Storming:

- [Criação da OS](criacao-da-os.md): do cliente chegando na oficina até o orçamento enviado para aprovação.
- [Acompanhamento da OS](acompanhamento-da-os.md): do orçamento enviado até a entrega do veículo, com a rejeição, a reabertura, o cancelamento e o orçamento complementar.
- [Peças e insumos](pecas-e-insumos.md): do cadastro da peça ou insumo à entrada no estoque, com o ajuste direto ou pedido e aprovado.
- [Estoque da OS](estoque-da-os.md): o estoque reagindo à OS, com a reserva, a baixa, a liberação e a devolução.

## Contextos delimitados e pacotes

Cada contexto delimitado do mapa de contextos é um módulo, num pacote abaixo de [`br.com.arktech.workshop`](../../../src/main/java/br/com/arktech/workshop).

| Contexto delimitado | Pacote |
|---|---|
| Ordem de Serviço | [`serviceorder`](../../../src/main/java/br/com/arktech/workshop/serviceorder) |
| Cadastro | [`registry`](../../../src/main/java/br/com/arktech/workshop/registry) |
| Catálogo de Serviços | [`catalog`](../../../src/main/java/br/com/arktech/workshop/catalog) |
| Estoque | [`inventory`](../../../src/main/java/br/com/arktech/workshop/inventory) |
| Notificações | [`notification`](../../../src/main/java/br/com/arktech/workshop/notification) |
| Indicadores Operacionais | [`metrics`](../../../src/main/java/br/com/arktech/workshop/metrics) |
| Identidade e Acesso | [`auth`](../../../src/main/java/br/com/arktech/workshop/auth) |
| Núcleo compartilhado | [`shared`](../../../src/main/java/br/com/arktech/workshop/shared) |

Os eventos que passam de um contexto para outro ficam em [`shared/event/serviceorder`](../../../src/main/java/br/com/arktech/workshop/shared/event/serviceorder), e os contratos entre contextos, em [`shared/application`](../../../src/main/java/br/com/arktech/workshop/shared/application).

## Como ler as tabelas

Cada fluxo tem uma tabela, com os elementos agrupados por tipo e, dentro do tipo, na ordem da linha do tempo. O elemento que aparece mais de uma vez na aba, como na linha do tempo e no bloco de agregados, entra uma vez.

O que a coluna Código traz, por tipo:

- **Comando:** a rota e o caso de uso. O comando que uma política dispara não tem rota: o listener chama o caso de uso.
- **Evento:** a classe em [`shared/event/serviceorder`](../../../src/main/java/br/com/arktech/workshop/shared/event/serviceorder) e o método do agregado que a registra. Quando o evento não tem classe, só o método.
- **Política:** o listener, ou o job agendado quando a política é de tempo.
- **Ator:** o papel que as rotas aceitam no [`@AllowedRoles`](../../../src/main/java/br/com/arktech/workshop/shared/api/security/AllowedRoles.java), ou as rotas públicas do cliente, com [`@PublicEndpoint`](../../../src/main/java/br/com/arktech/workshop/shared/api/security/PublicEndpoint.java). O administrador entra em todas as rotas da oficina, por isso só aparece onde a aba o cita.
- **Agregado:** a classe raiz.

Marcações na coluna Observação, para o que não tem correspondente direto no código:

- **Sem classe:** evento sem classe própria. O fato fica registrado no agregado, e o código citado é o método que o registra ([ADR 004](../../adr/004-domain-events.md)).
- **Recusa:** a aba desenha a recusa como evento, mas no código ela é uma exceção da regra de negócio. Nada fica registrado e a transação é desfeita.
- **Sem caso de uso:** comando sem caso de uso próprio. A observação diz o que o executa.
- **Sem listener:** política que roda dentro do agregado, na mesma chamada do comando.
- **Divergência:** a aba e o código não dizem a mesma coisa. A observação explica a diferença.

Modelos de leitura e pontos de atenção ficam fora das tabelas. Os modelos de leitura são as consultas, nas rotas GET de cada recurso, e os pontos de atenção são perguntas em aberto, sem código.
