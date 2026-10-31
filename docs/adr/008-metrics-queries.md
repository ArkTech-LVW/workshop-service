# ADR 008: Consultas de métricas sobre tabelas de outros módulos

- **Status:** Proposto (passa a Aceito com a aprovação do grupo no PR)
- **Data:** 2026-10-31
- **Tarefa:** [KAN-35 (T4.1)](https://arktech-lvw.atlassian.net/browse/KAN-35)

## Contexto

- As métricas de tempo da OS (tempo médio de execução, tempo em cada status) saem do histórico de status (`status_change`), que pertence ao módulo `serviceorder`. A média por serviço cruza esse histórico com os itens da OS (`service_item`, também do `serviceorder`) e com o catálogo (`service_offering`, do `catalog`).
- As métricas ficam no módulo `metrics`, mas o `LayerDependencyTest` impede um módulo de importar classes de outro: o `metrics` não enxerga o `ServiceOrder`, o `StatusChange`, o `ServiceOrderStatus` nem o `ServiceOffering`.
- Os `@Query(nativeQuery = true)` do projeto ficam em interfaces Spring Data (`JpaRepository<Entidade, Long>`), que precisam de uma entidade do próprio módulo. O `metrics` não tem entidade: só lê agregados.
- O cálculo precisa ficar no banco, sem carregar as OS na memória ([ADR 001](001-database.md), motivo 3).

## Opções consideradas

- **`JdbcClient` no `metrics`:** a porta fica na `application` do `metrics`, e a implementação na `infrastructure`, com a SQL num text block e o resultado mapeado direto para um `record`. O `JdbcClient` já vem com o `spring-boot-starter-data-jpa` e participa da mesma transação do JPA.
- **`EntityManager.createNativeQuery` no `metrics`:** continua no JPA, mas devolve `Object[]`, com cast manual de cada coluna.
- **Método nativo no `ServiceOrderJpaRepository`, exposto por um contrato no `shared`** (como `PartDirectory`): respeita o isolamento sem ler tabela alheia, mas a métrica passa a morar no `serviceorder`, e o `metrics` vira uma casca que só repassa a chamada.

## Decisão

**`JdbcClient` no módulo `metrics`, lendo as tabelas do `serviceorder` e do `catalog` direto.**

- Porta `ServiceOrderHistoryReader` (`metrics/application/port`), implementada por `JdbcServiceOrderHistoryReader` (`metrics/infrastructure/persistence`), com três consultas: a contagem das OS do período, o tempo por status e o tempo de execução por serviço. O caso de uso `GetServiceOrderTimeMetricsUseCase` valida e converte o período e monta o resultado.
- O período tem no máximo 366 dias. O custo da consulta cresce com o número de OS do período (ver [Índice](#índice)), e um ano cobre a comparação que a métrica precisa.
- Os status entram na SQL e no caso de uso como literais (`'COMPLETED'`, `'IN_PROGRESS'`), porque o enum é do `serviceorder`.
- O tempo em cada status sai de `LEAD(date_created)` sobre o histórico de cada OS, ordenado por `date_created, id`. A média é convertida em segundos no banco (`round(EXTRACT(EPOCH FROM avg(...)))`) e vira `Duration` no Java. O `interval` do PostgreSQL não chega ao JDBC, porque o driver o entrega como `PGInterval`, separado em meses, dias e horas.

### Custo aceito

- O `metrics` conhece as tabelas e colunas do `serviceorder` e do `catalog` e os nomes dos status. Uma migration que renomeie uma coluna ou um status quebra a métrica sem erro de compilação: quem pega é o teste de integração (`GetServiceOrderTimeMetricsIntegrationTest`), que roda contra o esquema real.
- A leitura é só leitura: o `metrics` nunca grava nas tabelas de outro módulo.

## Índice

A consulta começa pelas OS com entrada em `COMPLETED` no período e depois lê o histórico inteiro de cada uma. Até aqui, `status_change` só tinha índice em `service_order_id`, então o primeiro passo varria a tabela toda.

```sql
CREATE INDEX idx_status_change_completed_date_created
    ON status_change (date_created) INCLUDE (service_order_id)
    WHERE to_status = 'COMPLETED';
```

O índice foi escolhido por um estudo com dados sintéticos realistas: cerca de 100 mil OS em 2 anos e 725 mil linhas de histórico, geradas seguindo a máquina de estados (fluxo direto, complementar, recusa e reabertura, cancelamento). Os candidatos foram `(to_status, date_created)`, o mesmo com `INCLUDE (service_order_id)`, o parcial acima e combinações com `(service_order_id, date_created, id)` no lugar do índice atual, medidos com `EXPLAIN (ANALYZE, BUFFERS)` em períodos de um mês (3,6 mil OS) e de um ano (44 mil OS).

- **Contagem das OS do período:** qualquer índice no filtro troca a varredura da tabela por uma leitura de cerca de 20 blocos, cerca de 80 vezes mais rápida no mês (de 38 ms para 0,4 ms). Os candidatos empatam no tempo, e o parcial é o menor: 2,7 MB, contra 29 a 36 MB dos índices em `(to_status, date_created)`. O `INCLUDE (service_order_id)` dá um index-only scan, sem tocar o heap.
- **Tempo por status:** o ganho é pequeno (de cerca de 140 ms para 100 ms no mês e de 1,1 s para 0,8 s no ano). O índice resolve o filtro, mas o histórico das OS do período continua sendo lido por varredura: com o histórico de cada OS espalhado pela tabela, ler tudo sai mais barato que dezenas de milhares de leituras aleatórias. O resto do tempo é sort, window function e agregação, que nenhum índice evita.
- **Índice de histórico `(service_order_id, date_created, id)`:** fica de fora. O planner não o usa com a configuração padrão. Forçado, ajuda no mês (cerca de 70 ms) e piora no ano (1,1 s). Usá-lo exigiria baixar o `random_page_cost` do servidor, uma decisão de infraestrutura.
- A consulta por serviço usa o mesmo filtro e o mesmo histórico, e chega aos itens pelo índice que o `service_item` já tem em `service_order_id`.
- Uma métrica futura ancorada em outra transição (ex.: OS entregues no período) precisa do próprio índice parcial, ou de um `(to_status, date_created)` geral, se forem várias.

## Consequências

- A rota `GET /admin/metrics/averageExecutionTime` só converte o resultado do `GetServiceOrderTimeMetricsUseCase` em resposta. Agregações novas sobre o mesmo histórico estendem a mesma porta.
- A média por serviço não isola o tempo do serviço: o item não registra quando começou, então cada serviço recebe o tempo de execução da OS inteira. Medir o serviço em si depende de registrar o início de cada item na OS.
- Métricas novas sobre tabelas de outros módulos seguem o mesmo padrão: porta no `metrics/application`, `JdbcClient` no `metrics/infrastructure`, teste de integração com dados montados por SQL.
