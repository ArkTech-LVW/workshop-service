# ADR 001: Banco de dados e persistência

- **Status:** Proposto (passa a Aceito com a aprovação do grupo no PR)
- **Data:** 2026-10-20
- **Tarefa:** [KAN-12 (T0.3)](https://arktech-lvw.atlassian.net/browse/KAN-12)

## Contexto

A API gerencia clientes, veículos, catálogo de serviços, peças e insumos com estoque, ordens de serviço (OS) com máquina de estados e métricas. Três pontos pesam na escolha do banco:

- **Aprovar orçamento e baixar estoque (T3.6):** mudar o status da OS, gravar o histórico e baixar o estoque de cada peça precisa ser atômico. Duas aprovações concorrentes não podem deixar saldo negativo.
- **Integridade referencial:** a OS aponta para cliente e veículo, os itens apontam para serviços e peças e o veículo pertence a um cliente. CPF/CNPJ e placa são únicos.
- **Consultas de relatório (E4):** tempo médio de execução (diferença entre datas do histórico de status) e agregações por status e período.

Além disso, o esquema precisa ser versionado, testável contra o banco real e rodar em Docker (T0.4).

## Opções consideradas

| | PostgreSQL | MySQL | MongoDB |
|---|---|---|---|
| Transação multi-tabela / multi-documento | ACID completo, `SELECT ... FOR UPDATE` | ACID (InnoDB) | Só com replica set, e o agregado da OS deixa de ser o único documento envolvido |
| Integridade (FK, UNIQUE, CHECK) | Sim | Sim (CHECK só a partir da 8.0.16) | Não: fica a cargo da aplicação |
| Relatórios | CTEs, window functions, `interval` com `avg()` | Funções analíticas mais limitadas, sem `interval` | Aggregation pipeline, sem SQL |
| DDL transacional (migration que falha é revertida inteira) | Sim | Não | N/A |
| Encaixe na stack do épico (Spring Data JPA, Flyway, Testcontainers) | Total | Total | Exigiria Spring Data MongoDB e outra ferramenta de migration |

## Decisão

**PostgreSQL 18**, acessado via **Spring Data JPA** (Hibernate), com o esquema versionado pelo **Flyway**.

### Motivos

1. **Aprovação + estoque:** uma única transação (`@Transactional` na camada `application`) cobre status, histórico e baixa de todas as peças. Se qualquer passo falhar, nada é gravado. A concorrência no saldo é tratada com lock pessimista (`SELECT ... FOR UPDATE`) ou otimista (`@Version`), a definir na T2.5/T3.6, e um `CHECK (quantity >= 0)` garante no banco que o saldo nunca fica negativo.
2. **Integridade:** FKs, `UNIQUE` (CPF/CNPJ, placa) e `CHECK` protegem os dados mesmo contra bugs da aplicação.
3. **Relatórios:** `avg(finished_at - started_at)` devolve um `interval` direto, sem cálculo na aplicação. `GROUP BY` e window functions cobrem as agregações. `numeric` casa com `BigDecimal` para valores monetários.
4. **Migrations seguras:** no PostgreSQL, o Flyway roda cada migration dentro de uma transação, inclusive o DDL. Uma migration com erro não deixa o esquema pela metade.

### Como fica configurado

- Migrations em `src/main/resources/db/migration`, no padrão `V<n>__<description>.sql`, aplicadas pelo Flyway na subida da aplicação. `spring.jpa.hibernate.ddl-auto=validate`: o Hibernate apenas confere o mapeamento, e o esquema vem só das migrations.
- `V1__baseline.sql` marca o início com o esquema vazio. Cada módulo cria as próprias tabelas na sua tarefa, a partir da V2.
- Tabelas e colunas em inglês e snake_case, tabelas no plural (`service_orders`, `stock_movements`).
- Conexão pelas variáveis `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`, com padrões para `localhost` em desenvolvimento.
- `spring.jpa.open-in-view=false`: nenhum acesso lazy fora da transação da `application`.
- Testes de integração (`@IntegrationTest`) contra PostgreSQL real via Testcontainers (`postgres:18-alpine`). A mesma configuração de container é usada por `./gradlew bootTestRun`. Testes unitários não sobem banco.

### Persistência nas entidades de domínio

- As entidades de domínio usam anotações **`jakarta.persistence`** diretamente. Não há entidades espelho na infraestrutura.
  - **Por quê:** evita duplicar cada entidade e escrever mapeadores em todos os CRUDs. `jakarta.persistence` é uma especificação e não traz Spring para o domínio.
  - **Custo aceito:** o domínio conhece nomes de tabelas e colunas e precisa de um construtor sem argumentos `protected` (exigência do JPA).
- O domínio continua sem Spring e também não pode usar APIs específicas do Hibernate (`org.hibernate..`). O `LayerDependencyTest` (ArchUnit) verifica isso.
- **Repositórios:** a interface fica no domínio, em Java puro. Na infraestrutura, uma interface Spring Data estende a do domínio e `JpaRepository`, e o Spring Data gera a implementação. A `application` injeta só a interface do domínio. Repositórios Spring Data só podem existir na infraestrutura (ArchUnit).
- **Chave primária:** `BIGINT GENERATED ALWAYS AS IDENTITY`, mapeada como `Long` com `GenerationType.IDENTITY`. É simples, legível e compacta nos índices. O número legível da OS (`SO-2026-000123`) é um campo à parte.

## Consequências

- Docker passa a ser pré-requisito para `./gradlew build` (os testes de integração sobem o PostgreSQL), tanto no dev quanto no CI (T0.6).
- **T0.4:** o `docker-compose.yml` deve usar `postgres:18-alpine` e as variáveis `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`. A partir do PostgreSQL 18, o volume da imagem oficial fica em `/var/lib/postgresql`, e não mais em `/var/lib/postgresql/data`.
- IDs sequenciais são enumeráveis. A API do cliente (T3.8) precisa autorizar pelo dono do recurso, nunca pela dificuldade de adivinhar o ID.
- Como o ID só existe depois do `persist`, `equals`/`hashCode` seguem o padrão descrito no `CONTRIBUTING.md`.
- Dois PRs em paralelo podem criar a mesma versão de migration. Quem mergear por último renumera a sua antes do merge (o Flyway recusa versões duplicadas e o build quebra).
- Em aberto: a interface de repositório do domínio não pode usar `Pageable`/`Page` do Spring Data. A primeira listagem paginada (ex.: T3.9) define o tipo de paginação a adotar.
