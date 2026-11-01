# Linguagem ubíqua e glossário

Termos do domínio da oficina mecânica, por contexto delimitado, com o nome que cada um recebe no código. A versão resumida está na aba **Linguagem ubíqua e glossário** do [`workshop-ddd.drawio`](workshop-ddd.drawio).

Convenções:

- **Nome no código** é como o termo é chamado no código, em inglês.
- Nomes em `PascalCase` são classes ou enums, e em `UPPER_CASE`, valores de enum. Nomes em `camelCase` são conceitos sem classe própria: aparecem no código como atributo, operação ou status.
- Os contextos seguem o mapa de contextos: Ordem de Serviço, Cadastro, Catálogo de Serviços, Estoque, Notificações, Indicadores Operacionais e Identidade e Acesso. O núcleo compartilhado reúne os objetos de valor comuns a vários contextos.

## Sumário

- [Ordem de Serviço](#ordem-de-serviço)
- [Status da OS](#status-da-os)
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

Subdomínio principal. Vai da abertura da OS até a entrega do veículo.

| Termo | Nome no código | Definição | Exemplo |
|---|---|---|---|
| Ordem de Serviço (OS) | `ServiceOrder` | O atendimento de um veículo de um cliente, com os itens, o orçamento, o status e o histórico. | A OS do Corolla do João, aberta por barulho na suspensão. |
| Número da OS | `ServiceOrderNumber` | Identificador legível da OS, no formato `SO-<ano>-<sequência>`. É o que a oficina fala com o cliente. | `SO-2026-000042` |
| Código de acompanhamento | `publicId` | Código que a oficina repassa ao cliente para ele consultar e decidir a OS ([ADR 005](../adr/005-customer-access.md)). | `3f1c9a2e-...` |
| Descrição do problema | `problemDescription` | O relato do cliente na abertura. Obrigatório. | "Barulho ao passar em lombada." |
| Diagnóstico | `diagnosis` | Fase em que o mecânico avalia o veículo e os itens do orçamento são montados. Quem inicia o diagnóstico vira o mecânico responsável. | O mecânico Carlos assume a OS e começa o diagnóstico. |
| Mecânico responsável | `mechanic` | O mecânico que assumiu a OS no diagnóstico. | Carlos. |
| Item da OS | `ServiceOrderItem` | O que entra no orçamento: um serviço ou uma peça, com quantidade e preço unitário. | 2 × pastilha de freio a R$ 90,00. |
| Item de serviço | `ServiceItem` | Serviço do catálogo incluído na OS. Tem início e conclusão próprios, registrados pelo mecânico. Quando o último é concluído e nada está pendente de aprovação, a OS é finalizada. | 1 × alinhamento, iniciado às 9h e concluído às 9h40. |
| Item de peça | `PartItem` | Peça ou insumo do estoque incluído na OS. | 4 L de óleo 5W30. |
| Preço unitário | `unitPrice` | Preço copiado do catálogo ou do estoque na inclusão do item. Não muda se o preço de origem mudar depois. | R$ 120,00 copiado do alinhamento. |
| Valor total da OS | `totalPrice` | Soma dos subtotais dos serviços e das peças. | R$ 1.250,00. |
| Orçamento | `quote` | A proposta de serviços e peças com o valor total, montada a partir dos itens da OS. É enviado ao cliente, que aprova ou rejeita; antes da decisão, a oficina pode retirá-lo para mudar os itens. Não é uma entidade separada da OS ([ADR 006](../adr/006-supplementary-quote.md)). | Orçamento de R$ 1.250,00 com 3 serviços e 5 peças. |
| Item aprovado | `approvedItem` | Item que o cliente já aceitou. Enquanto não é aprovado, o item está pendente. | Pastilha aprovada em 30/10. |
| Etapa do item no orçamento | `QuoteItemStage` | Como o cliente vê cada item: já feito (não pode recusar), já aprovado ou novo, esperando decisão. | Alinhamento: feito; troca de amortecedor: novo. |
| Orçamento complementar | `supplementaryQuote` | Novo pedido de aprovação durante a execução, com itens novos ou trocados, sem perder o que já foi aprovado e feito. O cliente aprova tudo, só o original ou rejeita ([ADR 006](../adr/006-supplementary-quote.md)). | O mecânico acha um amortecedor vazando durante a troca de pastilha. |
| Pode seguir sem as mudanças | `canProceedWithoutChanges` | Informado no envio do complementar: diz se o serviço original pode continuar sem os itens novos. Habilita a resposta "só o original". | Sim: dá para entregar o carro sem trocar o amortecedor. |
| Motivo | `reason` | Justificativa obrigatória para rejeitar e cancelar. | "Preço acima do esperado." |
| Histórico de status | `StatusChange` | Registro de cada mudança de status: de onde, para onde, quem mudou e a observação. É a base das métricas de tempo. | Aguardando aprovação → Orçamento aprovado, sem usuário (decisão do cliente). |
| Cliente (ator) | `customer` | Dono do veículo, sem login. Identifica-se pelo código de acompanhamento e pelo CPF/CNPJ. As decisões dele ficam sem usuário no histórico. | O João aprova o orçamento pelo celular. |

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

## Cadastro

| Termo | Nome no código | Definição | Exemplo |
|---|---|---|---|
| Cliente | `Customer` | Pessoa física ou jurídica dona de veículos, identificada pelo CPF/CNPJ. | João Silva. |
| Documento do cliente | `document` (`CPFCNPJ`) | CPF ou CNPJ do cliente, validado pelos dígitos verificadores. Não se repete entre clientes. | 123.456.789-09 |
| Tipo de pessoa | `PersonType` | Física ou jurídica, deduzida do documento. | CNPJ → pessoa jurídica. |
| Contato do cliente | `email`, `phone` | E-mail obrigatório e telefone opcional, usados nos avisos. | joao@exemplo.com, (85) 99999-0000. |
| Veículo | `Vehicle` | Veículo de um cliente, identificado pela placa. | Corolla prata 2020. |
| Placa | `LicensePlate` | Placa no padrão antigo ou Mercosul. Não se repete entre veículos. | ABC1234, ABC1D23. |
| Chassi | `Chassis` | Número de identificação do veículo, com 17 caracteres. Opcional. | 9BWZZZ377VT004251 |
| Marca, modelo, ano e cor | `brand`, `model`, `year`, `color` | Dados do veículo. O ano vai de 1981 até o ano seguinte ao atual. | Toyota, Corolla, 2020, prata. |
| Dono do veículo | `customer` | O cliente a quem o veículo pertence. | O Corolla é do João. |

## Catálogo de Serviços

| Termo | Nome no código | Definição | Exemplo |
|---|---|---|---|
| Serviço (do catálogo) | `ServiceOffering` | Serviço que a oficina oferece, com preço de mão de obra e tempo estimado. É o modelo de onde saem os itens de serviço da OS. | Alinhamento. |
| Preço de mão de obra | `laborPrice` | Valor do serviço, maior que zero. É copiado para o item da OS na inclusão. | R$ 120,00. |
| Tempo estimado | `estimatedMinutes` | Duração esperada de uma unidade do serviço, em minutos. É comparado com o tempo real nas métricas. | 40 minutos. |
| Serviço ativo / desativado | `active` | Serviço desativado continua no histórico das OS, mas não entra em OS nova. | "Polimento" desativado na baixa temporada. |

## Estoque

| Termo | Nome no código | Definição | Exemplo |
|---|---|---|---|
| Peça (do estoque) | `Part` | Item controlado em estoque, com SKU, tipo, unidade, preço de venda e quantidades. Pode ser peça ou insumo. | Pastilha de freio dianteira. |
| Tipo do item de estoque | `PartType` | Diferencia peça de insumo. | Pastilha é peça; óleo é insumo. |
| Peça | `PART` | Componente instalado no veículo, contado por unidade. | Filtro de óleo. |
| Insumo | `SUPPLY` | Material consumido no serviço, em geral medido por volume ou peso. | Óleo 5W30, graxa. |
| SKU | `Sku` | Código único do item no estoque. | PAST-DIANT-001 |
| Unidade de medida | `MeasurementUnit` | Como a quantidade do item é contada: unidade, litro ou quilo. | Óleo em litros. |
| Preço de venda | `salePrice` | Valor cobrado do cliente por unidade. É copiado para o item da OS na inclusão. | R$ 90,00. |
| Quantidade em estoque | `stockQuantity` | Total físico na prateleira. Nunca fica negativo. | 20 unidades. |
| Quantidade reservada | `reservedQuantity` | Soma das reservas ativas da peça. Nunca passa da quantidade em estoque. | 4 unidades reservadas. |
| Disponível | `availableQuantity` | Estoque menos reservado. Toda saída e toda reserva saem do disponível ([ADR 009](../adr/009-stock-reservation.md)). | 20 − 4 = 16. |
| Estoque mínimo | `minimumStock` | Quantidade abaixo da qual a peça entra no alerta de estoque baixo. | 5 unidades. |
| Estoque baixo | `lowStock` | Disponível igual ou abaixo do estoque mínimo. | 4 disponíveis com mínimo 5. |
| Movimentação de estoque | `StockMovement` | Registro de cada entrada, saída ou ajuste, com quantidade, motivo, usuário e, quando houver, a OS. | Saída de 2 pastilhas para a OS 42. |
| Tipo de movimentação | `StockMovementType` | Entrada, saída ou ajuste. | Compra do fornecedor é entrada. |
| Entrada | `IN` | Mercadoria que chega e aumenta o estoque. | Chegaram 10 filtros. |
| Baixa de estoque | `OUT` | Saída de peças para uma OS, na aprovação do orçamento. No complementar, sai só a diferença em relação ao que já tinha saído. | 2 pastilhas saem para a OS 42. |
| Devolução de estoque | `serviceOrderReturn` | Peças que voltam ao estoque quando a OS é cancelada ou o complementar tira peças que já tinham saído. É registrada como entrada ligada à OS ([ADR 006](../adr/006-supplementary-quote.md)). | OS cancelada devolve 2 pastilhas. |
| Ajuste de estoque | `ADJUSTMENT` | Saída por perda, quebra ou correção de inventário, com motivo obrigatório. | 1 filtro danificado. |
| Pedido de ajuste | `StockAdjustmentRequest` | Pedido de ajuste aberto pelo atendente (ou por um ADMIN). Só vira movimentação depois que um ADMIN, que não seja quem pediu, aprova. Lançar o ajuste direto é só do ADMIN. | O atendente pede ajuste de 1 litro de óleo derramado. |
| Status do pedido de ajuste | `StockAdjustmentRequestStatus` | Pendente, aprovado ou rejeitado. | Pendente até o ADMIN revisar. |
| Reserva de estoque | `StockReservation` | Quantidade de uma peça separada para uma OS no envio do orçamento, com validade. É liberada na rejeição, na retirada do orçamento, no cancelamento ou no vencimento. | 2 pastilhas reservadas para a OS 42 até 07/11. |
| Validade da reserva | `expiresAt` | 7 dias, renovada a cada envio. A reserva vencida é liberada sozinha, sem mudar a OS. | Enviado em 31/10, vence em 07/11. |

## Notificações

| Termo | Nome no código | Definição | Exemplo |
|---|---|---|---|
| Aviso ao cliente | `Notification` | Pedido de outro contexto para avisar o cliente sobre um fato da OS. O contexto de notificações decide o canal. | "Seu orçamento está disponível." |
| Tipo de aviso | `NotificationType` | O fato que gera o aviso: orçamento enviado, aprovado, rejeitado e OS finalizada. | Orçamento enviado. |
| Canal | `NotificationChannel` | Meio de envio do aviso. Hoje só e-mail. | E-mail. |
| E-mail de notificação | `EmailNotification` | E-mail guardado para envio em segundo plano, com assunto, corpo, tentativas e erro. | E-mail do orçamento da OS 42. |
| Status do e-mail | `EmailNotificationStatus` | Pendente, enviado ou falhou depois do limite de tentativas. | Falhou após 5 tentativas. |

## Indicadores Operacionais

Não tem entidade própria: lê o histórico de status, os itens da OS e o catálogo ([ADR 008](../adr/008-metrics-queries.md)).

| Termo | Nome no código | Definição | Exemplo |
|---|---|---|---|
| Métricas de tempo da OS | `serviceOrderTimeMetrics` | Indicadores calculados sobre as OS finalizadas num período de até 366 dias. | Outubro de 2026. |
| Tempo médio de execução | `averageExecutionTime` | Média do tempo que as OS do período passaram em execução. | 2h15. |
| Tempo em cada status | `timePerStatus` | Média de quanto as OS ficam em cada status, a partir do histórico. | 1 dia aguardando aprovação. |
| Tempo real por serviço | `executionTimePerService` | Média por unidade do início à conclusão de cada serviço, sem contar as esperas do complementar. Comparável ao tempo estimado do catálogo. | Alinhamento: 38 min reais × 40 min estimados. |

## Identidade e Acesso

| Termo | Nome no código | Definição | Exemplo |
|---|---|---|---|
| Usuário | `User` | Pessoa da equipe da oficina com acesso ao sistema. O cliente não é usuário. | Ana, administradora. |
| Perfil de acesso | `Role` | O que o usuário pode fazer. Um usuário pode ter mais de um perfil. | Ana é administradora e atendente. |
| Administrador | `ADMIN` | Gerencia usuários, cadastros, catálogo e estoque e revisa pedidos de ajuste. Sempre existe ao menos um ativo. | Ana. |
| Atendente | `ATTENDANT` | Recebe o cliente, abre a OS, monta e envia o orçamento. | Bruno. |
| Mecânico | `MECHANIC` | Diagnostica, inclui itens, envia o orçamento e executa os serviços. | Carlos. |
| Usuário ativo / desativado | `active` | Usuário desativado não faz login. O último administrador ativo não pode ser desativado. | Ex-funcionário desativado. |
| Usuário atual | `CurrentUser` | Quem está fazendo a ação. É o "quem mudou" do histórico. | Bruno envia o orçamento. |

## Núcleo compartilhado

Objetos de valor usados por mais de um contexto.

| Termo | Nome no código | Definição | Exemplo |
|---|---|---|---|
| Dinheiro | `Money` | Valor em reais com duas casas decimais. | R$ 1.250,00. |
| CPF | `CPF` | Documento de pessoa física, validado pelos dígitos verificadores. | 123.456.789-09 |
| CNPJ | `CNPJ` | Documento de pessoa jurídica, validado pelos dígitos verificadores. | 11.222.333/0001-81 |
| CPF ou CNPJ | `CPFCNPJ` | Documento que aceita os dois formatos. É o documento do cliente. | Um dos dois acima. |
| E-mail | `Email` | Endereço de e-mail validado. | joao@exemplo.com |
| Telefone | `Phone` | Telefone brasileiro com DDD, fixo ou celular. | (85) 99999-0000 |
| Placa | `LicensePlate` | Placa brasileira, antiga ou Mercosul. | ABC1D23 |
| Chassi | `Chassis` | Número de identificação do veículo, com 17 caracteres. | 9BWZZZ377VT004251 |

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
