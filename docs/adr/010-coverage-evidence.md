# ADR 010: Regra de cobertura e evidência versionada

- **Status:** Proposto (passa a Aceito com a aprovação do grupo no PR)
- **Data:** 2026-11-01
- **Tarefa:** [KAN-38 (T5.2)](https://arktech-lvw.atlassian.net/browse/KAN-38)

## Contexto

- O JaCoCo já gerava o relatório em todo `./gradlew build`, mas nada falhava quando a cobertura caía.
- A tarefa pede um mínimo nas camadas `domain` e `application` e uma evidência da cobertura versionada no repositório para a entrega.
- A regra vale para todos os módulos.
- Na medição inicial, o escopo da regra estava em 99,9% das linhas, e o projeto inteiro em 99,4%.
- A `main` só aceita merge por PR, com aprovação e com os checks `build` e `Aguardando outro PR`.

## Opções consideradas

### A regra

| | Escolhido | Descartado |
|---|---|---|
| Nível | Um total para `domain` e `application` de todos os módulos | Por pacote: dezenas de pacotes de use case com 2 a 5 linhas, e uma linha sem teste derrubaria o build. Por módulo: o JaCoCo não faz nativamente, e exigiria uma task por módulo |
| Contador | Linhas (LINE) | Instruções: depende do bytecode gerado por Lombok e records. Branches: mais rigoroso, mas fica só no relatório, sem mínimo |
| Mínimo | 90% | 80%, o número do ticket: com o escopo em 99,9%, cerca de 280 linhas novas sem teste passariam sem quebrar o build |
| Exclusões | Nenhuma dentro de `domain` e `application` | Tirar `*Command`, `*Query`, `*Result`, `*Converter` e `*Exception`: o total iria de 99,9% para 99,8%, e a lista de sufixos viraria manutenção sem ganho |

O que não é regra de negócio já fica fora porque mora em `api`, `infrastructure`, configurações e na classe `main`.

### A evidência

| Opção | Por que não |
|---|---|
| Print do relatório HTML | Manual, não se compara com a versão anterior, não se confere e envelhece no dia seguinte |
| Relatório HTML completo no repositório | 1.003 arquivos e 7,4 MB, com um diff enorme a cada atualização |
| Artefato e comentário do CI | Não ficam no repositório, e o artefato expira |
| GitHub Pages com o HTML | Sempre atual, mas não fica versionado |
| Atualizar a evidência em todo PR | Todo PR mexeria nos mesmos arquivos e entraria em conflito com os outros |
| Workflow commitando direto na `main` | Bloqueado pelas regras da `main` |
| Workflow abrindo o PR sozinho | O que o token padrão do Actions cria não dispara outros workflows, então os checks obrigatórios nunca rodariam e o PR ficaria travado |

## Decisão

1. **O `check` roda o `jacocoTestCoverageVerification`**, com mínimo de 90% das linhas no total das classes de `br/com/arktech/workshop/*/domain/**` e `*/application/**`. O mínimo fica em `coverageLineMinimum` no `gradle.properties`.
2. **A task `coverageSummary` gera a tabela por módulo em Markdown** a partir do CSV do JaCoCo. Ela roda sempre depois do relatório, mesmo quando algum teste falha. É a única conta da tabela: o CI e a evidência usam o mesmo arquivo.
3. **O CI publica a tabela** no resumo de toda run e num comentário fixo em cada PR, atualizado a cada push e achado por um marcador oculto.
4. **A evidência versionada sai do workflow manual Coverage evidence.** Ele roda a suíte na branch escolhida e gera em `docs/evidencias/cobertura/` o `README.md` (tabela com data, commit de origem e link da run) e o `jacocoTestReport.csv`. Depois, commita esses arquivos numa branch nova pela API `createCommitOnBranch` e deixa o link do PR no resumo da run. Quem abre o PR é uma pessoa, então o CI e a aprovação seguem como em qualquer PR.

```
./gradlew build
  test ──► jacocoTestReport ──► coverageSummary ──► coverage-summary.md
  check ──► jacocoTestCoverageVerification (< 90% falha)

CI (todo PR e push)          ──► resumo da run + comentário fixo no PR
Coverage evidence (manual)   ──► branch chore/coverage-evidence-* com commit assinado
                                   └─ uma pessoa abre o PR ──► CI e aprovação normais
```

## Consequências

- Uma queda de cobertura no domínio aparece no PR, com o módulo que caiu, antes do merge.
- A evidência é reproduzível e dá para conferir: o commit sai com o selo Verified do GitHub, assinado pelo `github-actions[bot]`, e o README aponta o commit de origem e a run que o gerou. O CSV permite auditar classe por classe, e o diff entre duas evidências mostra o que mudou.
- A evidência mostra o estado do dia em que o workflow rodou. Atualizar é decisão do grupo, não acontece sozinho.
- O workflow de evidência tem permissão de escrita no conteúdo do repositório. O resto continua só com leitura.
- O botão Run workflow só aparece depois que o arquivo do workflow chega à `main`.
- Subir o mínimo é mudar um número no `gradle.properties`.
