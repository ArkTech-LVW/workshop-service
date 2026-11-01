# Linguagem ubíqua e glossário

Termos do domínio da oficina mecânica, por contexto delimitado, com o nome que cada um recebe no código. O glossário documenta o que já está implementado. A versão visual resumida, usada na apresentação, está na aba **Linguagem ubíqua e glossário** do [`workshop-ddd.drawio`](workshop-ddd.drawio). Este arquivo tem as definições completas.

Convenções:

- **Nome no código** é como o termo é chamado no código, em inglês.
- Nomes em `PascalCase` são classes ou enums, e em `UPPER_CASE`, valores de enum. Nomes em `camelCase` são conceitos sem classe própria: aparecem no código como atributo, operação ou status.

## Sumário

- [Ordem de Serviço](#ordem-de-serviço)
- [Cadastro](#cadastro)
- [Catálogo de Serviços](#catálogo-de-serviços)
- [Estoque](#estoque)
- [Notificações](#notificações)
- [Indicadores Operacionais](#indicadores-operacionais)
- [Identidade e Acesso](#identidade-e-acesso)
- [Núcleo compartilhado](#núcleo-compartilhado)
- [Eventos de domínio](#eventos-de-domínio)
- [Termos que mudam de significado entre contextos](#termos-que-mudam-de-significado-entre-contextos)

## Ordem de Serviço

Subdomínio principal · Bounded context: `serviceorder`

| Termo | Nome no código | Definição |
|---|---|---|
| Ordem de Serviço (OS) | `ServiceOrder` | Registro do atendimento de um veículo, com itens, orçamento, status e histórico. |
| Número da OS | `ServiceOrderNumber` | Identificador legível da OS, como `SO-2026-000042`. |
| Código de acompanhamento | `publicId` | Código que o cliente usa para consultar e decidir a OS. |
| Descrição do problema | `problemDescription` | Relato do cliente na abertura da OS. |
| Diagnóstico | `diagnosis` | Avaliação técnica do veículo, quando os itens do orçamento são montados. |
| Mecânico responsável | `mechanic` | Mecânico que assumiu a OS no diagnóstico. |
| Item da OS | `ServiceOrderItem` | Serviço ou peça incluído na OS, com quantidade e preço unitário. |
| Item de serviço | `ServiceItem` | Serviço do catálogo incluído na OS, com início e conclusão próprios. |
| Item de peça | `PartItem` | Peça ou insumo do estoque incluído na OS. |
| Preço unitário | `unitPrice` | Preço copiado do catálogo ou do estoque na inclusão do item. |
| Valor total da OS | `totalPrice` | Soma dos itens de serviço e de peça. |
| Orçamento | `quote` | Proposta de serviços e peças com o valor total, enviada ao cliente. |
| Item aprovado | `approvedItem` | Item que o cliente já aceitou. |
| Etapa do item no orçamento | `QuoteItemStage` | Como o cliente vê cada item: já feito, já aprovado ou novo. |
| Orçamento complementar | `supplementaryQuote` | Novo pedido de aprovação durante a execução, com itens novos ou trocados. |
| Pode seguir sem as mudanças | `canProceedWithoutChanges` | Indica se o serviço original pode continuar sem os itens novos do complementar. |
| Motivo | `reason` | Justificativa da rejeição ou do cancelamento. |
| Histórico de status | `StatusChange` | Registro de cada mudança de status, com quem mudou e quando. |
| Cliente (ator) | `customer` | Dono do veículo, que acompanha e decide a OS sem login. |

### Status da OS

| Termo | Nome no código | Quando |
|---|---|---|
| Recebida | `RECEIVED` | OS aberta. |
| Em diagnóstico | `IN_DIAGNOSIS` | Mecânico avaliando; itens sendo montados. Também é para onde volta um orçamento retirado ou um rejeitado que foi reaberto. |
| Aguardando aprovação | `AWAITING_APPROVAL` | Orçamento enviado ao cliente. |
| Orçamento aprovado | `QUOTE_APPROVED` | Aprovado, na fila para começar. |
| Orçamento rejeitado | `QUOTE_REJECTED` | O cliente recusou; a oficina reabre o orçamento ou cancela a OS. |
| Em execução | `IN_PROGRESS` | Serviços sendo feitos. |
| Aguardando aprovação do complementar | `AWAITING_SUPPLEMENTARY_APPROVAL` | Orçamento complementar enviado durante a execução. |
| Complementar aprovado | `SUPPLEMENTARY_APPROVED` | Complementar (ou só o original) aprovado; a execução retoma. |
| Complementar rejeitado | `SUPPLEMENTARY_REJECTED` | O cliente recusou o complementar; a oficina refaz e reenvia, ou cancela. |
| Finalizada | `COMPLETED` | Todos os serviços concluídos. |
| Entregue | `DELIVERED` | Veículo retirado. |
| Cancelada | `CANCELED` | Encerrada antes da execução ou durante a espera do complementar. |

### Regras de negócio

O cliente aprova ou rejeita o orçamento, pelo código de acompanhamento e pelo CPF/CNPJ ([ADR 005](../adr/005-customer-access.md)). Antes da decisão, a oficina pode retirar o orçamento para mudar os itens. Quando a execução revela serviços ou peças adicionais, a oficina apresenta um orçamento complementar, que passa por nova aprovação: o cliente aprova tudo, só o original ou rejeita ([ADR 006](../adr/006-supplementary-quote.md)). O preço de cada item é copiado na inclusão e não muda depois. A OS só é finalizada com todos os serviços concluídos e nada pendente de aprovação.

### Termos relacionados

`ServiceOrder`, `ServiceOrderStatus`, `ServiceItem`, `PartItem`, `StatusChange`, `QuoteItemStage`, `QuoteSentEvent`, `QuoteApprovedEvent`, `QuoteRejectedEvent`, `QuoteWithdrawnEvent`, `ServiceOrderCompletedEvent`, `ServiceOrderCanceledEvent`.

## Cadastro

Subdomínio de suporte · Bounded context: `registry`

| Termo | Nome no código | Definição |
|---|---|---|
| Cliente | `Customer` | Pessoa física ou jurídica dona de veículos. |
| Documento do cliente | `document` | CPF ou CNPJ que identifica o cliente. |
| Tipo de pessoa | `PersonType` | Física ou jurídica, deduzida do documento. |
| Contato do cliente | `email`, `phone` | E-mail e telefone usados nos avisos. |
| Veículo | `Vehicle` | Veículo de um cliente, identificado pela placa. |
| Placa | `LicensePlate` | Placa no padrão antigo ou Mercosul. |
| Chassi | `Chassis` | Número de identificação do veículo, com 17 caracteres. |
| Marca, modelo, ano e cor | `brand`, `model`, `year`, `color` | Dados descritivos do veículo. |
| Dono do veículo | `customer` | Cliente a quem o veículo pertence. |

### Regras de negócio

Documento e placa não se repetem no cadastro, e o documento é validado pelos dígitos verificadores. O e-mail é obrigatório e o telefone, opcional. O chassi é opcional. O ano do veículo vai de 1981 até o ano seguinte ao atual.

### Termos relacionados

`Customer`, `Vehicle`, `PersonType`, `CPFCNPJ`, `LicensePlate`, `Chassis`.

## Catálogo de Serviços

Subdomínio de suporte · Bounded context: `catalog`

| Termo | Nome no código | Definição |
|---|---|---|
| Serviço (do catálogo) | `ServiceOffering` | Serviço que a oficina oferece. |
| Preço de mão de obra | `laborPrice` | Valor do serviço. |
| Tempo estimado | `estimatedMinutes` | Duração esperada de uma unidade do serviço, em minutos. |
| Serviço ativo / desativado | `active` | Indica se o serviço pode entrar em OS nova. |

### Regras de negócio

O preço de mão de obra e o tempo estimado são maiores que zero. O preço é copiado para a OS na inclusão do serviço. Um serviço desativado continua no histórico das OS, mas não entra em OS nova.

### Termos relacionados

`ServiceOffering`, `Money`.

## Estoque

Subdomínio de suporte · Bounded context: `inventory`

| Termo | Nome no código | Definição |
|---|---|---|
| Peça (do estoque) | `Part` | Item controlado em estoque: peça ou insumo. |
| Tipo do item de estoque | `PartType` | Diferencia peça de insumo. |
| Peça | `PART` | Componente instalado no veículo, como um filtro de óleo. |
| Insumo | `SUPPLY` | Material consumido no serviço, como óleo e graxa. |
| SKU | `Sku` | Código único do item no estoque. |
| Unidade de medida | `MeasurementUnit` | Unidade, litro ou quilo. |
| Preço de venda | `salePrice` | Valor cobrado do cliente por unidade. |
| Quantidade em estoque | `stockQuantity` | Total físico na prateleira. |
| Quantidade reservada | `reservedQuantity` | Soma das reservas ativas da peça. |
| Disponível | `availableQuantity` | Estoque menos reservado. |
| Estoque mínimo | `minimumStock` | Quantidade que dispara o alerta de estoque baixo. |
| Estoque baixo | `lowStock` | Disponível igual ou abaixo do estoque mínimo. |
| Movimentação de estoque | `StockMovement` | Registro de uma entrada, saída ou ajuste. |
| Tipo de movimentação | `StockMovementType` | Entrada, saída ou ajuste. |
| Entrada | `IN` | Mercadoria que chega ao estoque. |
| Baixa de estoque | `OUT` | Saída de peças para uma OS. |
| Devolução de estoque | `serviceOrderReturn` | Peças que voltam de uma OS para o estoque. |
| Ajuste de estoque | `ADJUSTMENT` | Saída por perda, quebra ou correção de inventário. |
| Pedido de ajuste | `StockAdjustmentRequest` | Pedido de ajuste que depende da aprovação de um administrador. |
| Status do pedido de ajuste | `StockAdjustmentRequestStatus` | Pendente, aprovado ou rejeitado. |
| Reserva de estoque | `StockReservation` | Quantidade de uma peça separada para uma OS. |
| Validade da reserva | `expiresAt` | Prazo da reserva: 7 dias, renovados a cada envio do orçamento. |

### Regras de negócio

O estoque nunca fica negativo, e a quantidade reservada nunca passa da quantidade em estoque. As peças são reservadas no envio do orçamento e baixadas na aprovação. No complementar, só a diferença em relação ao que já saiu é baixada. A reserva é liberada na rejeição, na retirada do orçamento, no cancelamento ou no vencimento, e o cancelamento devolve as peças que já saíram ([ADR 009](../adr/009-stock-reservation.md)). Só o administrador lança ajuste direto; o atendente abre um pedido, que um administrador aprova ou rejeita.

### Termos relacionados

`Part`, `PartType`, `Sku`, `MeasurementUnit`, `StockMovement`, `StockMovementType`, `StockAdjustmentRequest`, `StockReservation`.

## Notificações

Subdomínio genérico · Bounded context: `notification`

| Termo | Nome no código | Definição |
|---|---|---|
| Aviso ao cliente | `Notification` | Pedido para avisar o cliente sobre um fato da OS. |
| Tipo de aviso | `NotificationType` | Orçamento enviado, aprovado, rejeitado ou OS finalizada. |
| Canal | `NotificationChannel` | Meio de envio do aviso; hoje, só e-mail. |
| E-mail de notificação | `EmailNotification` | E-mail guardado para envio em segundo plano. |
| Status do e-mail | `EmailNotificationStatus` | Pendente, enviado ou com falha. |
| Tentativa de envio | `attempts` | Quantas vezes o envio já foi tentado. |

### Regras de negócio

O aviso é registrado junto com o fato da OS e enviado depois, em segundo plano. Cada falha agenda nova tentativa com espera crescente, até 5. Quando o cliente não tem e-mail utilizável, o aviso fica registrado sem envio.

### Termos relacionados

`Notification`, `NotificationType`, `NotificationChannel`, `EmailNotification`, `EmailNotificationStatus`.

## Indicadores Operacionais

Subdomínio de suporte · Bounded context: `metrics`

| Termo | Nome no código | Definição |
|---|---|---|
| Métricas de tempo da OS | `serviceOrderTimeMetrics` | Indicadores de tempo das OS finalizadas num período. |
| Tempo médio de execução | `averageExecutionTime` | Média do tempo que as OS passaram em execução. |
| Tempo em cada status | `timePerStatus` | Média de quanto as OS ficam em cada status. |
| Tempo real por serviço | `executionTimePerService` | Média do início à conclusão de cada serviço. |

### Regras de negócio

O contexto não tem entidade própria: lê o histórico de status, os itens da OS e o catálogo ([ADR 008](../adr/008-metrics-queries.md)). O período consultado tem no máximo 366 dias. O tempo real por serviço não conta as esperas do orçamento complementar e é comparável ao tempo estimado do catálogo.

### Termos relacionados

`StatusChange`, `ServiceItem`, `ServiceOffering`.

## Identidade e Acesso

Subdomínio genérico · Bounded context: `auth`

| Termo | Nome no código | Definição |
|---|---|---|
| Usuário | `User` | Pessoa da equipe da oficina com acesso ao sistema. |
| Perfil de acesso | `Role` | O que o usuário pode fazer. |
| Administrador | `ADMIN` | Gerencia usuários, cadastros, catálogo e estoque. |
| Atendente | `ATTENDANT` | Recebe o cliente, abre a OS e monta o orçamento. |
| Mecânico | `MECHANIC` | Diagnostica e executa os serviços. |
| Usuário ativo / desativado | `active` | Indica se o usuário pode fazer login. |
| Usuário atual | `CurrentUser` | Quem está fazendo a ação. |

### Regras de negócio

O cliente não é usuário: acessa a OS pelo código de acompanhamento e pelo documento. Um usuário pode ter mais de um perfil. Sempre existe ao menos um administrador ativo, que não pode ser desativado nem perder o perfil. O login tem limite de tentativas ([ADR 003](../adr/003-authentication.md)).

### Termos relacionados

`User`, `Role`, `CurrentUser`.

## Núcleo compartilhado

Objetos de valor usados por mais de um contexto · Pacote: `shared`

| Termo | Nome no código | Definição |
|---|---|---|
| Dinheiro | `Money` | Valor em reais com duas casas decimais. |
| CPF | `CPF` | Documento de pessoa física. |
| CNPJ | `CNPJ` | Documento de pessoa jurídica. |
| CPF ou CNPJ | `CPFCNPJ` | Documento que aceita os dois formatos. |
| E-mail | `Email` | Endereço de e-mail. |
| Telefone | `Phone` | Telefone brasileiro com DDD. |
| Placa | `LicensePlate` | Placa brasileira, antiga ou Mercosul. |
| Chassi | `Chassis` | Número de identificação do veículo. |

### Regras de negócio

Cada objeto de valor se valida na criação: CPF, CNPJ e chassi pelos dígitos verificadores, e placa, e-mail e telefone pelo formato. Um valor inválido nunca chega aos contextos.

## Eventos de domínio

Só os eventos da OS viraram classe ([ADR 004](../adr/004-domain-events.md)). Os demais fatos do Event Storming ficam registrados no próprio agregado, sem classe de evento.

| Termo | Nome no código | O que acontece depois |
|---|---|---|
| Orçamento enviado | `QuoteSentEvent` | O estoque reserva as peças e o cliente é avisado. |
| Orçamento aprovado | `QuoteApprovedEvent` | O estoque baixa as peças pela diferença e o cliente é avisado. |
| Orçamento rejeitado | `QuoteRejectedEvent` | O estoque libera a reserva e o cliente é avisado. |
| Orçamento retirado | `QuoteWithdrawnEvent` | O estoque libera a reserva. |
| OS finalizada | `ServiceOrderCompletedEvent` | O cliente é avisado. |
| OS cancelada | `ServiceOrderCanceledEvent` | O estoque devolve as peças que saíram e libera a reserva. |
| OS aberta | `serviceOrderOpened` | Nada; fica no histórico de status. |
| Diagnóstico iniciado | `diagnosisStarted` | Nada; fica no histórico de status. |
| Item incluído / removido | `itemAdded` / `itemRemoved` | O valor total é recalculado. |
| Execução iniciada | `executionStarted` | Nada; fica no histórico e entra nas métricas. |
| Serviço iniciado / concluído | `serviceItemStarted` / `completed` | Entra nas métricas; o último concluído finaliza a OS. |
| Veículo entregue | `delivered` | Nada; fica no histórico de status. |
| Cliente cadastrado | `customerCreated` | Nada. |
| Veículo cadastrado | `vehicleCreated` | Nada. |
| Serviço cadastrado / desativado | `serviceOfferingCreated` / `deactivated` | Nada. |
| Peça cadastrada | `partCreated` | Nada. |
| Estoque movimentado | `stockMovementRegistered` | A quantidade da peça muda. |
| Ajuste solicitado / revisado | `stockAdjustmentRequested` / `reviewed` | Se aprovado, vira movimentação de ajuste. |
| Estoque reservado / liberado | `stockReserved` / `released` | O disponível da peça muda. |
| E-mail enviado / falhou | `emailSent` / `failed` | Se falhou, agenda nova tentativa. |

## Termos que mudam de significado entre contextos

| Termo | Nome no código | Contexto | Significado |
|---|---|---|---|
| Serviço | `ServiceOffering` | Catálogo de Serviços | O serviço oferecido, com preço de mão de obra e tempo estimado. |
| Serviço | `ServiceItem` | Ordem de Serviço | A linha da OS, com quantidade, preço copiado, aprovação, início e conclusão. |
| Peça | `Part` | Estoque | O item controlado, com SKU, quantidades e reservas. Pode ser peça ou insumo. |
| Peça | `PartItem` | Ordem de Serviço | A linha da OS, com quantidade e preço copiado. |
| Preço | `laborPrice` | Catálogo de Serviços | Preço de mão de obra. |
| Preço | `salePrice` | Estoque | Preço de venda. |
| Preço | `unitPrice` | Ordem de Serviço | Preço copiado na inclusão do item, que não acompanha mudanças posteriores. |
| Cliente | `Customer` | Cadastro | O registro com documento e contatos. |
| Cliente | `customer` | Ordem de Serviço | O ator sem login que acompanha e decide a OS pelo código e pelo documento. |
| Cliente | `customer` | Notificações | O destinatário do aviso. |
| Usuário | `User` | Identidade e Acesso | A conta com perfis e senha. |
| Usuário | `changedBy` | Ordem de Serviço | Só quem fez a ação. Sem usuário no histórico significa decisão do cliente. |
| Aprovação | `QUOTE_APPROVED` | Ordem de Serviço | O cliente (ou a oficina) aceita o orçamento. |
| Aprovação | `APPROVED` | Estoque | O administrador aceita um pedido de ajuste. |
| Status | `ServiceOrderStatus` | Ordem de Serviço | Os 12 status do atendimento. |
| Status | `StockAdjustmentRequestStatus` | Estoque | A situação do pedido de ajuste. |
| Status | `EmailNotificationStatus` | Notificações | A situação do e-mail. |
| Concluído | `completed` | Ordem de Serviço (item de serviço) | O mecânico terminou aquele serviço. |
| Concluído | `COMPLETED` | Ordem de Serviço (OS) | Todos os serviços concluídos e nada pendente. |
| Ativo | `active` | Catálogo de Serviços | Serviço que pode entrar em OS nova. |
| Ativo | `active` | Identidade e Acesso | Usuário que pode fazer login. |
| Tempo | `estimatedMinutes` | Catálogo de Serviços | Tempo estimado de uma unidade do serviço. |
| Tempo | `averageExecutionTime` | Indicadores Operacionais | Tempo real, medido pelo histórico e pelos itens. |
