package br.com.arktech.workshop.serviceorder.domain.model;

import static br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus.AWAITING_APPROVAL;
import static br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus.CANCELED;
import static br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus.COMPLETED;
import static br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus.DELIVERED;
import static br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus.IN_DIAGNOSIS;
import static br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus.IN_PROGRESS;
import static br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus.QUOTE_APPROVED;
import static br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus.QUOTE_REJECTED;
import static br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus.RECEIVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderEvent.QuoteApproved;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderEvent.QuoteRejected;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderEvent.QuoteReopened;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderEvent.QuoteSent;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderEvent.ServiceOrderCanceled;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderEvent.ServiceOrderCompleted;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class ServiceOrderTest {

    private static final ServiceOrderNumber NUMBER = new ServiceOrderNumber("SO-2026-000123");
    private static final Long CUSTOMER_ID = 10L;
    private static final Long VEHICLE_ID = 20L;
    private static final Long ATTENDANT_ID = 1L;
    private static final Long USER_ID = 2L;
    private static final Long SERVICE_OFFERING_ID = 30L;
    private static final Long PART_ID = 40L;
    private static final Money LABOR_PRICE = new Money(new BigDecimal("150.00"));
    private static final Money PART_PRICE = new Money(new BigDecimal("35.90"));
    private static final String DESCRIPTION = "Cliente avisado por telefone";

    @Test
    void shouldOpenAsReceivedWithTheOpeningRecord() {
        ServiceOrder serviceOrder = newServiceOrder();

        assertThat(serviceOrder.getPublicId()).isNotNull();
        assertThat(serviceOrder.getNumber()).isEqualTo(NUMBER);
        assertThat(serviceOrder.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(serviceOrder.getVehicleId()).isEqualTo(VEHICLE_ID);
        assertThat(serviceOrder.getMechanicId()).isNull();
        assertThat(serviceOrder.getProblemDescription()).isEqualTo("Barulho ao frear");
        assertThat(serviceOrder.getStatus()).isEqualTo(RECEIVED);
        assertThat(serviceOrder.getTotalPrice()).isEqualTo(Money.ZERO);
        assertThat(serviceOrder.getServiceItems()).isEmpty();
        assertThat(serviceOrder.getPartItems()).isEmpty();
        assertThat(serviceOrder.getStatusHistory()).singleElement().satisfies(change -> {
            assertThat(change.getFromStatus()).isNull();
            assertThat(change.getToStatus()).isEqualTo(RECEIVED);
            assertThat(change.getChangedByUserId()).isEqualTo(ATTENDANT_ID);
            assertThat(change.getDescription()).isNull();
        });
        assertThat(serviceOrder.pullEvents()).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void shouldRequireTheProblemDescription(String problemDescription) {
        assertThatThrownBy(() -> new ServiceOrder(NUMBER, CUSTOMER_ID, VEHICLE_ID, problemDescription, ATTENDANT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrder.problemDescriptionRequired");
    }

    @Test
    void shouldGenerateADifferentPublicIdForEachServiceOrder() {
        assertThat(newServiceOrder().getPublicId()).isNotEqualTo(newServiceOrder().getPublicId());
    }

    @ParameterizedTest
    @MethodSource("validTransitions")
    void shouldMoveFromAnOriginStatusRecordingTheChange(Transition transition, ServiceOrderStatus origin) {
        ServiceOrder serviceOrder = serviceOrderIn(origin);
        List<StatusChange> previousHistory = serviceOrder.getStatusHistory();

        transition.apply(serviceOrder);

        assertThat(serviceOrder.getStatus()).isEqualTo(transition.to);
        assertThat(serviceOrder.getStatusHistory())
                .hasSize(previousHistory.size() + 1)
                .startsWith(previousHistory.toArray(StatusChange[]::new))
                .last()
                .satisfies(change -> {
                    assertThat(change.getFromStatus()).isEqualTo(origin);
                    assertThat(change.getToStatus()).isEqualTo(transition.to);
                    assertThat(change.getChangedByUserId()).isEqualTo(USER_ID);
                    assertThat(change.getDescription()).isEqualTo(DESCRIPTION);
                });
    }

    @ParameterizedTest
    @MethodSource("validTransitions")
    void shouldRegisterTheEventOfTheTransition(Transition transition, ServiceOrderStatus origin) {
        ServiceOrder serviceOrder = serviceOrderIn(origin);

        transition.apply(serviceOrder);

        assertThat(serviceOrder.pullEvents())
                .containsExactlyElementsOf(transition.expectedEvents(serviceOrder.getPublicId()));
    }

    @ParameterizedTest
    @MethodSource("transitionsFromAnotherStatus")
    void shouldRefuseTheTransitionFromAnyOtherStatus(Transition transition, ServiceOrderStatus status) {
        ServiceOrder serviceOrder = serviceOrderIn(status);
        List<StatusChange> previousHistory = serviceOrder.getStatusHistory();

        assertThatThrownBy(() -> transition.apply(serviceOrder))
                .isInstanceOfSatisfying(InvalidStatusTransitionException.class, exception -> {
                    assertThat(exception.getFrom()).isEqualTo(status);
                    assertThat(exception.getTo()).isEqualTo(transition.to);
                })
                .hasMessage("serviceOrder.invalidStatusTransition");
        assertThat(serviceOrder.getStatus()).isEqualTo(status);
        assertThat(serviceOrder.getStatusHistory()).containsExactlyElementsOf(previousHistory);
        assertThat(serviceOrder.pullEvents()).isEmpty();
    }

    @Test
    void shouldAssignTheMechanicWhoStartsTheDiagnosis() {
        ServiceOrder serviceOrder = newServiceOrder();

        serviceOrder.startDiagnosis(USER_ID, null);

        assertThat(serviceOrder.getMechanicId()).isEqualTo(USER_ID);
        assertThat(serviceOrder.getStatusHistory().getLast().getChangedByUserId()).isEqualTo(USER_ID);
        assertThat(serviceOrder.getStatusHistory().getLast().getDescription()).isNull();
    }

    @Test
    void shouldRecordTheCustomerDecisionWithoutUser() {
        ServiceOrder approved = serviceOrderIn(AWAITING_APPROVAL);
        ServiceOrder rejected = serviceOrderIn(AWAITING_APPROVAL);

        approved.approveQuote(null, null);
        rejected.rejectQuote(null, "Achou caro");

        assertThat(approved.getStatusHistory().getLast().getChangedByUserId()).isNull();
        assertThat(rejected.getStatusHistory().getLast().getChangedByUserId()).isNull();
        assertThat(rejected.getStatusHistory().getLast().getDescription()).isEqualTo("Achou caro");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void shouldRequireAReasonToRejectTheQuote(String reason) {
        ServiceOrder serviceOrder = serviceOrderIn(AWAITING_APPROVAL);

        assertThatThrownBy(() -> serviceOrder.rejectQuote(null, reason))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrder.reasonRequired");
        assertThat(serviceOrder.getStatus()).isEqualTo(AWAITING_APPROVAL);
        assertThat(serviceOrder.pullEvents()).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void shouldRequireAReasonToCancel(String reason) {
        ServiceOrder serviceOrder = newServiceOrder();

        assertThatThrownBy(() -> serviceOrder.cancel(USER_ID, reason))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrder.reasonRequired");
        assertThat(serviceOrder.getStatus()).isEqualTo(RECEIVED);
        assertThat(serviceOrder.getStatusHistory()).hasSize(1);
        assertThat(serviceOrder.pullEvents()).isEmpty();
    }

    @Test
    void shouldRefuseToSendAQuoteWithoutItems() {
        ServiceOrder serviceOrder = newServiceOrder();
        serviceOrder.startDiagnosis(USER_ID, null);

        assertThatThrownBy(() -> serviceOrder.sendQuote(USER_ID, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrder.quoteWithoutItems");
        assertThat(serviceOrder.getStatus()).isEqualTo(IN_DIAGNOSIS);
        assertThat(serviceOrder.getStatusHistory()).hasSize(2);
        assertThat(serviceOrder.pullEvents()).isEmpty();
    }

    @Test
    void shouldSendAQuoteWithPartsOnly() {
        ServiceOrder serviceOrder = newServiceOrder();
        serviceOrder.startDiagnosis(USER_ID, null);
        serviceOrder.addPartItem(PART_ID, 1, PART_PRICE);

        serviceOrder.sendQuote(USER_ID, null);

        assertThat(serviceOrder.getStatus()).isEqualTo(AWAITING_APPROVAL);
    }

    @Test
    void shouldChangeItemsAndSendANewQuoteAfterReopeningARejectedQuote() {
        ServiceOrder serviceOrder = serviceOrderIn(QUOTE_REJECTED);
        assignIds(serviceOrder.getServiceItems(), 1);

        serviceOrder.reopenQuote(USER_ID, "Proposta com peça usada");
        serviceOrder.removeServiceItem(1L);
        serviceOrder.addPartItem(PART_ID, 2, PART_PRICE);
        serviceOrder.sendQuote(USER_ID, null);

        assertThat(serviceOrder.getStatus()).isEqualTo(AWAITING_APPROVAL);
        assertThat(serviceOrder.getServiceItems()).isEmpty();
        assertThat(serviceOrder.getPartItems()).singleElement().extracting(PartItem::getQuantity).isEqualTo(2);
        assertThat(serviceOrder.getTotalPrice()).isEqualTo(new Money(new BigDecimal("71.80")));
        assertThat(serviceOrder.getStatusHistory())
                .extracting(StatusChange::getToStatus)
                .containsExactly(
                        RECEIVED, IN_DIAGNOSIS, AWAITING_APPROVAL, QUOTE_REJECTED, IN_DIAGNOSIS, AWAITING_APPROVAL);
        assertThat(serviceOrder.pullEvents())
                .containsExactly(
                        new QuoteReopened(serviceOrder.getPublicId()), new QuoteSent(serviceOrder.getPublicId()));
    }

    @Test
    void shouldAddItemsCopyingTheUnitPriceAndRecalculateTheTotal() {
        ServiceOrder serviceOrder = newServiceOrder();

        serviceOrder.addServiceItem(SERVICE_OFFERING_ID, 2, LABOR_PRICE);
        serviceOrder.addPartItem(PART_ID, 3, PART_PRICE);

        assertThat(serviceOrder.getServiceItems()).singleElement().satisfies(item -> {
            assertThat(item.getServiceOfferingId()).isEqualTo(SERVICE_OFFERING_ID);
            assertThat(item.getQuantity()).isEqualTo(2);
            assertThat(item.getUnitPrice()).isEqualTo(LABOR_PRICE);
            assertThat(item.getSubtotal()).isEqualTo(new Money(new BigDecimal("300.00")));
        });
        assertThat(serviceOrder.getPartItems()).singleElement().satisfies(item -> {
            assertThat(item.getPartId()).isEqualTo(PART_ID);
            assertThat(item.getQuantity()).isEqualTo(3);
            assertThat(item.getUnitPrice()).isEqualTo(PART_PRICE);
            assertThat(item.getSubtotal()).isEqualTo(new Money(new BigDecimal("107.70")));
        });
        assertThat(serviceOrder.getTotalPrice()).isEqualTo(new Money(new BigDecimal("407.70")));
    }

    @Test
    void shouldRemoveItemsAndRecalculateTheTotal() {
        ServiceOrder serviceOrder = newServiceOrder();
        serviceOrder.addServiceItem(SERVICE_OFFERING_ID, 1, LABOR_PRICE);
        serviceOrder.addServiceItem(SERVICE_OFFERING_ID + 1, 1, new Money(new BigDecimal("80.00")));
        serviceOrder.addPartItem(PART_ID, 2, PART_PRICE);
        assignIds(serviceOrder.getServiceItems(), 1);
        assignIds(serviceOrder.getPartItems(), 3);

        serviceOrder.removeServiceItem(1L);
        serviceOrder.removePartItem(3L);

        assertThat(serviceOrder.getServiceItems())
                .singleElement()
                .extracting(ServiceItem::getServiceOfferingId)
                .isEqualTo(SERVICE_OFFERING_ID + 1);
        assertThat(serviceOrder.getPartItems()).isEmpty();
        assertThat(serviceOrder.getTotalPrice()).isEqualTo(new Money(new BigDecimal("80.00")));
    }

    @Test
    void shouldReportAnItemThatIsNotInTheServiceOrder() {
        ServiceOrder serviceOrder = newServiceOrder();
        serviceOrder.addServiceItem(SERVICE_OFFERING_ID, 1, LABOR_PRICE);
        assignIds(serviceOrder.getServiceItems(), 1);

        assertThatThrownBy(() -> serviceOrder.removeServiceItem(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOrder.itemNotFound");
        assertThatThrownBy(() -> serviceOrder.removePartItem(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOrder.itemNotFound");
        assertThat(serviceOrder.getServiceItems()).hasSize(1);
        assertThat(serviceOrder.getTotalPrice()).isEqualTo(LABOR_PRICE);
    }

    @Test
    void shouldRefuseAnItemThatPushesTheTotalPastWhatTheColumnHolds() {
        ServiceOrder serviceOrder = newServiceOrder();
        serviceOrder.addServiceItem(SERVICE_OFFERING_ID, 1, new Money(new BigDecimal("9999999999.00")));

        assertThatThrownBy(() -> serviceOrder.addPartItem(PART_ID, 1, new Money(new BigDecimal("1.00"))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrder.totalPriceTooHigh");
        assertThat(serviceOrder.getPartItems()).isEmpty();
        assertThat(serviceOrder.getTotalPrice()).isEqualTo(new Money(new BigDecimal("9999999999.00")));

        serviceOrder.addPartItem(PART_ID, 1, new Money(new BigDecimal("0.99")));

        assertThat(serviceOrder.getTotalPrice()).isEqualTo(new Money(new BigDecimal("9999999999.99")));
    }

    @Test
    void shouldRefuseAQuantityWhoseSubtotalIsTooHigh() {
        ServiceOrder serviceOrder = newServiceOrder();

        assertThatThrownBy(() -> serviceOrder.addServiceItem(SERVICE_OFFERING_ID, Integer.MAX_VALUE, LABOR_PRICE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrder.totalPriceTooHigh");
        assertThat(serviceOrder.getServiceItems()).isEmpty();
        assertThat(serviceOrder.getTotalPrice()).isEqualTo(Money.ZERO);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldRejectNonPositiveQuantity(int quantity) {
        ServiceOrder serviceOrder = newServiceOrder();

        assertThatThrownBy(() -> serviceOrder.addServiceItem(SERVICE_OFFERING_ID, quantity, LABOR_PRICE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrder.itemQuantityNotPositive");
        assertThatThrownBy(() -> serviceOrder.addPartItem(PART_ID, quantity, PART_PRICE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrder.itemQuantityNotPositive");
        assertThat(serviceOrder.getServiceItems()).isEmpty();
        assertThat(serviceOrder.getPartItems()).isEmpty();
        assertThat(serviceOrder.getTotalPrice()).isEqualTo(Money.ZERO);
    }

    @ParameterizedTest
    @EnumSource(names = {"RECEIVED", "IN_DIAGNOSIS"})
    void shouldAllowItemChangesWhileReceivedOrInDiagnosis(ServiceOrderStatus status) {
        ServiceOrder serviceOrder = serviceOrderIn(status);

        serviceOrder.addPartItem(PART_ID, 1, PART_PRICE);
        assignIds(serviceOrder.getServiceItems(), 1);
        serviceOrder.removeServiceItem(1L);

        assertThat(serviceOrder.getServiceItems()).isEmpty();
        assertThat(serviceOrder.getPartItems()).hasSize(1);
        assertThat(serviceOrder.getTotalPrice()).isEqualTo(PART_PRICE);
    }

    @ParameterizedTest
    @EnumSource(names = {"RECEIVED", "IN_DIAGNOSIS"}, mode = EnumSource.Mode.EXCLUDE)
    void shouldLockItemsInAnyOtherStatus(ServiceOrderStatus status) {
        ServiceOrder serviceOrder = serviceOrderIn(status);
        assignIds(serviceOrder.getServiceItems(), 1);
        Money totalPrice = serviceOrder.getTotalPrice();

        assertThatThrownBy(() -> serviceOrder.addServiceItem(SERVICE_OFFERING_ID, 1, LABOR_PRICE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrder.itemsLocked");
        assertThatThrownBy(() -> serviceOrder.addPartItem(PART_ID, 1, PART_PRICE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrder.itemsLocked");
        assertThatThrownBy(() -> serviceOrder.removeServiceItem(1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrder.itemsLocked");
        assertThatThrownBy(() -> serviceOrder.removePartItem(1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrder.itemsLocked");
        assertThat(serviceOrder.getServiceItems()).hasSize(1);
        assertThat(serviceOrder.getPartItems()).isEmpty();
        assertThat(serviceOrder.getTotalPrice()).isEqualTo(totalPrice);
    }

    @Test
    void shouldHandOverEachEventOnlyOnce() {
        ServiceOrder serviceOrder = serviceOrderIn(IN_DIAGNOSIS);
        serviceOrder.sendQuote(USER_ID, null);

        assertThat(serviceOrder.pullEvents()).containsExactly(new QuoteSent(serviceOrder.getPublicId()));
        assertThat(serviceOrder.pullEvents()).isEmpty();
    }

    @Test
    void shouldNotExposeTheInternalLists() {
        ServiceOrder serviceOrder = newServiceOrder();

        assertThatThrownBy(() -> serviceOrder.getServiceItems().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> serviceOrder.getPartItems().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> serviceOrder.getStatusHistory().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    static Stream<Arguments> validTransitions() {
        return Arrays.stream(Transition.values())
                .flatMap(transition -> transition.from.stream().map(origin -> Arguments.of(transition, origin)));
    }

    static Stream<Arguments> transitionsFromAnotherStatus() {
        return Arrays.stream(Transition.values())
                .flatMap(transition -> Arrays.stream(ServiceOrderStatus.values())
                        .filter(status -> !transition.from.contains(status))
                        .map(status -> Arguments.of(transition, status)));
    }

    private static ServiceOrder newServiceOrder() {
        return new ServiceOrder(NUMBER, CUSTOMER_ID, VEHICLE_ID, "Barulho ao frear", ATTENDANT_ID);
    }

    private static ServiceOrder serviceOrderIn(ServiceOrderStatus status) {
        ServiceOrder serviceOrder = newServiceOrder();
        serviceOrder.addServiceItem(SERVICE_OFFERING_ID, 1, LABOR_PRICE);
        pathTo(status).forEach(transition -> transition.apply(serviceOrder));
        serviceOrder.pullEvents();
        return serviceOrder;
    }

    private static List<Transition> pathTo(ServiceOrderStatus status) {
        return switch (status) {
            case RECEIVED -> List.of();
            case IN_DIAGNOSIS -> append(pathTo(RECEIVED), Transition.START_DIAGNOSIS);
            case AWAITING_APPROVAL -> append(pathTo(IN_DIAGNOSIS), Transition.SEND_QUOTE);
            case QUOTE_APPROVED -> append(pathTo(AWAITING_APPROVAL), Transition.APPROVE_QUOTE);
            case QUOTE_REJECTED -> append(pathTo(AWAITING_APPROVAL), Transition.REJECT_QUOTE);
            case IN_PROGRESS -> append(pathTo(QUOTE_APPROVED), Transition.START_EXECUTION);
            case COMPLETED -> append(pathTo(IN_PROGRESS), Transition.COMPLETE);
            case DELIVERED -> append(pathTo(COMPLETED), Transition.DELIVER);
            case CANCELED -> append(pathTo(RECEIVED), Transition.CANCEL);
        };
    }

    private static List<Transition> append(List<Transition> path, Transition last) {
        return Stream.concat(path.stream(), Stream.of(last)).toList();
    }

    private static void assignIds(List<? extends ServiceOrderItem> items, long firstId) {
        long id = firstId;
        for (ServiceOrderItem item : items) {
            ReflectionTestUtils.setField(item, "id", id++);
        }
    }

    enum Transition {
        START_DIAGNOSIS(
                Set.of(RECEIVED),
                IN_DIAGNOSIS,
                serviceOrder -> serviceOrder.startDiagnosis(USER_ID, DESCRIPTION),
                null),
        SEND_QUOTE(
                Set.of(IN_DIAGNOSIS),
                AWAITING_APPROVAL,
                serviceOrder -> serviceOrder.sendQuote(USER_ID, DESCRIPTION),
                QuoteSent::new),
        APPROVE_QUOTE(
                Set.of(AWAITING_APPROVAL),
                QUOTE_APPROVED,
                serviceOrder -> serviceOrder.approveQuote(USER_ID, DESCRIPTION),
                QuoteApproved::new),
        REJECT_QUOTE(
                Set.of(AWAITING_APPROVAL),
                QUOTE_REJECTED,
                serviceOrder -> serviceOrder.rejectQuote(USER_ID, DESCRIPTION),
                QuoteRejected::new),
        REOPEN_QUOTE(
                Set.of(QUOTE_REJECTED),
                IN_DIAGNOSIS,
                serviceOrder -> serviceOrder.reopenQuote(USER_ID, DESCRIPTION),
                QuoteReopened::new),
        START_EXECUTION(
                Set.of(QUOTE_APPROVED),
                IN_PROGRESS,
                serviceOrder -> serviceOrder.startExecution(USER_ID, DESCRIPTION),
                null),
        COMPLETE(
                Set.of(IN_PROGRESS),
                COMPLETED,
                serviceOrder -> serviceOrder.complete(USER_ID, DESCRIPTION),
                ServiceOrderCompleted::new),
        DELIVER(
                Set.of(COMPLETED),
                DELIVERED,
                serviceOrder -> serviceOrder.deliver(USER_ID, DESCRIPTION),
                null),
        CANCEL(
                Set.of(RECEIVED, IN_DIAGNOSIS, AWAITING_APPROVAL, QUOTE_REJECTED),
                CANCELED,
                serviceOrder -> serviceOrder.cancel(USER_ID, DESCRIPTION),
                ServiceOrderCanceled::new);

        private final Set<ServiceOrderStatus> from;
        private final ServiceOrderStatus to;
        private final Consumer<ServiceOrder> action;
        private final @Nullable Function<UUID, ServiceOrderEvent> event;

        Transition(
                Set<ServiceOrderStatus> from,
                ServiceOrderStatus to,
                Consumer<ServiceOrder> action,
                @Nullable Function<UUID, ServiceOrderEvent> event) {
            this.from = from;
            this.to = to;
            this.action = action;
            this.event = event;
        }

        void apply(ServiceOrder serviceOrder) {
            action.accept(serviceOrder);
        }

        List<ServiceOrderEvent> expectedEvents(UUID publicId) {
            return event == null ? List.of() : List.of(event.apply(publicId));
        }
    }
}
