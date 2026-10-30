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

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.money.Money;
import br.com.arktech.workshop.shared.event.serviceorder.QuoteApprovedEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
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
    private static final Instant COMPLETED_AT = Instant.parse("2026-10-28T16:45:00Z");

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
        assertThat(serviceOrder.domainEvents()).isEmpty();
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

        assertThat(serviceOrder.domainEvents())
                .containsExactlyElementsOf(transition.expectedEvents(serviceOrder));
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
        assertThat(serviceOrder.domainEvents()).isEmpty();
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

    @Test
    void shouldListThePartsToWithdrawWhenTheQuoteIsApproved() {
        ServiceOrder serviceOrder = newServiceOrder();
        ReflectionTestUtils.setField(serviceOrder, "id", 99L);
        serviceOrder.startDiagnosis(USER_ID, null);
        serviceOrder.addPartItem(PART_ID, 3, PART_PRICE);
        serviceOrder.addPartItem(PART_ID + 1, 1, PART_PRICE);
        serviceOrder.sendQuote(USER_ID, null);
        serviceOrder.clearDomainEvents();

        serviceOrder.approveQuote(null, null);

        assertThat(serviceOrder.domainEvents())
                .containsExactly(new QuoteApprovedEvent(
                        99L,
                        serviceOrder.getPublicId(),
                        List.of(
                                new QuoteApprovedEvent.PartQuantity(PART_ID, 3),
                                new QuoteApprovedEvent.PartQuantity(PART_ID + 1, 1)),
                        null));
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
        assertThat(serviceOrder.domainEvents()).isEmpty();
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
        assertThat(serviceOrder.domainEvents()).isEmpty();
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
        assertThat(serviceOrder.domainEvents()).isEmpty();
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
        assignIds(serviceOrder.getPartItems(), 1);

        serviceOrder.reopenQuote(USER_ID, "Proposta com peça usada");
        serviceOrder.removePartItem(1L);
        serviceOrder.addPartItem(PART_ID + 1, 2, PART_PRICE);
        serviceOrder.sendQuote(USER_ID, null);

        assertThat(serviceOrder.getStatus()).isEqualTo(AWAITING_APPROVAL);
        assertThat(serviceOrder.getPartItems()).singleElement().satisfies(item -> {
            assertThat(item.getPartId()).isEqualTo(PART_ID + 1);
            assertThat(item.getQuantity()).isEqualTo(2);
        });
        assertThat(serviceOrder.getTotalPrice()).isEqualTo(new Money(new BigDecimal("71.80")));
        assertThat(serviceOrder.getStatusHistory())
                .extracting(StatusChange::getToStatus)
                .containsExactly(
                        RECEIVED, IN_DIAGNOSIS, AWAITING_APPROVAL, QUOTE_REJECTED, IN_DIAGNOSIS, AWAITING_APPROVAL);
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
    void shouldStartTheQuoteAtZeroWithTwoDecimalPlaces() {
        ServiceOrder serviceOrder = newServiceOrder();

        assertThat(serviceOrder.getServicesTotal().amount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(serviceOrder.getPartsTotal().amount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(serviceOrder.getTotalPrice().amount()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void shouldTotalServicesAndPartsSeparatelyAcrossManyItems() {
        ServiceOrder serviceOrder = newServiceOrder();

        serviceOrder.addServiceItem(SERVICE_OFFERING_ID, 2, LABOR_PRICE);
        serviceOrder.addServiceItem(SERVICE_OFFERING_ID + 1, 1, new Money(new BigDecimal("89.99")));
        serviceOrder.addServiceItem(SERVICE_OFFERING_ID + 2, 999, new Money(new BigDecimal("0.01")));
        serviceOrder.addPartItem(PART_ID, 3, PART_PRICE);
        serviceOrder.addPartItem(PART_ID + 1, 3, new Money(new BigDecimal("33.33")));
        serviceOrder.addPartItem(PART_ID + 2, 4, new Money(new BigDecimal("12.5")));

        assertThat(serviceOrder.getServiceItems())
                .extracting(item -> item.getSubtotal().amount())
                .containsExactly(new BigDecimal("300.00"), new BigDecimal("89.99"), new BigDecimal("9.99"));
        assertThat(serviceOrder.getPartItems())
                .extracting(item -> item.getSubtotal().amount())
                .containsExactly(new BigDecimal("107.70"), new BigDecimal("99.99"), new BigDecimal("50.00"));
        assertThat(serviceOrder.getServicesTotal().amount()).isEqualTo(new BigDecimal("399.98"));
        assertThat(serviceOrder.getPartsTotal().amount()).isEqualTo(new BigDecimal("257.69"));
        assertThat(serviceOrder.getTotalPrice().amount()).isEqualTo(new BigDecimal("657.67"));
    }

    // Unit prices hold at most two decimal places and quantities are integers, so subtotals are exact and
    // the quote never rounds: the values below would drift if any step rounded.
    @ParameterizedTest
    @CsvSource({"0.01, 1, 0.01", "0.01, 999, 9.99", "33.33, 3, 99.99", "0.05, 7, 0.35", "9.99, 999, 9980.01"})
    void shouldCalculateEachSubtotalExactlyWithTwoDecimalPlaces(String unitPrice, int quantity, String subtotal) {
        ServiceOrder serviceOrder = newServiceOrder();

        serviceOrder.addPartItem(PART_ID, quantity, new Money(new BigDecimal(unitPrice)));

        assertThat(serviceOrder.getPartItems().getFirst().getSubtotal().amount()).isEqualTo(new BigDecimal(subtotal));
        assertThat(serviceOrder.getTotalPrice().amount()).isEqualTo(new BigDecimal(subtotal));
    }

    @Test
    void shouldRecalculateTheTotalOnEveryInclusionAndRemoval() {
        ServiceOrder serviceOrder = newServiceOrder();

        serviceOrder.addServiceItem(SERVICE_OFFERING_ID, 1, LABOR_PRICE);
        assertThat(serviceOrder.getTotalPrice()).isEqualTo(new Money(new BigDecimal("150.00")));
        serviceOrder.addPartItem(PART_ID, 2, PART_PRICE);
        assertThat(serviceOrder.getTotalPrice()).isEqualTo(new Money(new BigDecimal("221.80")));
        assignIds(serviceOrder.getServiceItems(), 1);
        assignIds(serviceOrder.getPartItems(), 2);

        serviceOrder.removeServiceItem(1L);
        assertThat(serviceOrder.getServicesTotal()).isEqualTo(Money.ZERO);
        assertThat(serviceOrder.getTotalPrice()).isEqualTo(new Money(new BigDecimal("71.80")));
        serviceOrder.removePartItem(2L);
        assertThat(serviceOrder.getTotalPrice()).isEqualTo(Money.ZERO);
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

        serviceOrder.addServiceItem(SERVICE_OFFERING_ID, 1, LABOR_PRICE);
        assignIds(serviceOrder.getPartItems(), 1);
        serviceOrder.removePartItem(1L);

        assertThat(serviceOrder.getServiceItems()).hasSize(1);
        assertThat(serviceOrder.getPartItems()).isEmpty();
        assertThat(serviceOrder.getTotalPrice()).isEqualTo(LABOR_PRICE);
    }

    @ParameterizedTest
    @EnumSource(names = {"RECEIVED", "IN_DIAGNOSIS"}, mode = EnumSource.Mode.EXCLUDE)
    void shouldLockItemsInAnyOtherStatus(ServiceOrderStatus status) {
        ServiceOrder serviceOrder = serviceOrderIn(status);
        assignIds(serviceOrder.getPartItems(), 1);
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
        assertThat(serviceOrder.getServiceItems()).isEmpty();
        assertThat(serviceOrder.getPartItems()).hasSize(1);
        assertThat(serviceOrder.getTotalPrice()).isEqualTo(totalPrice);
    }

    @Test
    void shouldForgetTheEventsOncePublished() {
        ServiceOrder serviceOrder = serviceOrderIn(AWAITING_APPROVAL);
        serviceOrder.approveQuote(USER_ID, null);
        assertThat(serviceOrder.domainEvents()).singleElement().isInstanceOf(QuoteApprovedEvent.class);

        serviceOrder.clearDomainEvents();

        assertThat(serviceOrder.domainEvents()).isEmpty();
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

    @Test
    void shouldCompleteAServiceRecordingWhenAndWhoWithoutFinishingWhileOthersArePending() {
        ServiceOrder serviceOrder = serviceOrderInProgressWithServices(2);
        List<StatusChange> previousHistory = serviceOrder.getStatusHistory();

        serviceOrder.completeServiceItem(1L, USER_ID, COMPLETED_AT);

        assertThat(serviceOrder.getServiceItems().getFirst()).satisfies(item -> {
            assertThat(item.isCompleted()).isTrue();
            assertThat(item.getCompletedAt()).isEqualTo(COMPLETED_AT);
            assertThat(item.getCompletedByUserId()).isEqualTo(USER_ID);
        });
        assertThat(serviceOrder.getServiceItems().getLast().isCompleted()).isFalse();
        assertThat(serviceOrder.getStatus()).isEqualTo(IN_PROGRESS);
        assertThat(serviceOrder.getStatusHistory()).containsExactlyElementsOf(previousHistory);
        assertThat(serviceOrder.domainEvents()).isEmpty();
    }

    @Test
    void shouldCompleteTheServiceOrderWhenTheLastServiceIsCompleted() {
        ServiceOrder serviceOrder = serviceOrderInProgressWithServices(2);
        serviceOrder.completeServiceItem(2L, ATTENDANT_ID, COMPLETED_AT);

        serviceOrder.completeServiceItem(1L, USER_ID, COMPLETED_AT.plusSeconds(60));

        assertThat(serviceOrder.getStatus()).isEqualTo(COMPLETED);
        assertThat(serviceOrder.getServiceItems()).allMatch(ServiceItem::isCompleted);
        assertThat(serviceOrder.getStatusHistory()).last().satisfies(change -> {
            assertThat(change.getFromStatus()).isEqualTo(IN_PROGRESS);
            assertThat(change.getToStatus()).isEqualTo(COMPLETED);
            assertThat(change.getChangedByUserId()).isEqualTo(USER_ID);
            assertThat(change.getDescription()).isNull();
        });
    }

    @Test
    void shouldRefuseToCompleteAServiceTwice() {
        ServiceOrder serviceOrder = serviceOrderInProgressWithServices(2);
        serviceOrder.completeServiceItem(1L, USER_ID, COMPLETED_AT);

        assertThatThrownBy(() -> serviceOrder.completeServiceItem(1L, ATTENDANT_ID, COMPLETED_AT.plusSeconds(60)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrder.serviceItemAlreadyCompleted");
        assertThat(serviceOrder.getServiceItems().getFirst()).satisfies(item -> {
            assertThat(item.getCompletedAt()).isEqualTo(COMPLETED_AT);
            assertThat(item.getCompletedByUserId()).isEqualTo(USER_ID);
        });
    }

    @Test
    void shouldReportAServiceThatIsNotInTheServiceOrder() {
        ServiceOrder serviceOrder = serviceOrderInProgressWithServices(1);

        assertThatThrownBy(() -> serviceOrder.completeServiceItem(99L, USER_ID, COMPLETED_AT))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("serviceOrder.itemNotFound");
        assertThat(serviceOrder.getStatus()).isEqualTo(IN_PROGRESS);
    }

    @ParameterizedTest
    @EnumSource(names = "IN_PROGRESS", mode = EnumSource.Mode.EXCLUDE)
    void shouldRefuseToCompleteAServiceOutsideTheExecution(ServiceOrderStatus status) {
        ServiceOrder serviceOrder = serviceOrderIn(status);

        assertThatThrownBy(() -> serviceOrder.completeServiceItem(1L, USER_ID, COMPLETED_AT))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrder.serviceItemCompletionNotAllowed");
        assertThat(serviceOrder.getStatus()).isEqualTo(status);
    }

    @Test
    void shouldRefuseToCompleteTheServiceOrderWithPendingServices() {
        ServiceOrder serviceOrder = serviceOrderInProgressWithServices(2);
        serviceOrder.completeServiceItem(1L, USER_ID, COMPLETED_AT);
        List<StatusChange> previousHistory = serviceOrder.getStatusHistory();

        assertThatThrownBy(() -> serviceOrder.complete(USER_ID, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("serviceOrder.pendingServiceItems");
        assertThat(serviceOrder.getStatus()).isEqualTo(IN_PROGRESS);
        assertThat(serviceOrder.getStatusHistory()).containsExactlyElementsOf(previousHistory);
        assertThat(serviceOrder.domainEvents()).isEmpty();
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
        serviceOrder.addPartItem(PART_ID, 1, PART_PRICE);
        return walkTo(serviceOrder, status);
    }

    private static ServiceOrder serviceOrderInProgressWithServices(int count) {
        ServiceOrder serviceOrder = newServiceOrder();
        for (int i = 0; i < count; i++) {
            serviceOrder.addServiceItem(SERVICE_OFFERING_ID + i, 1, LABOR_PRICE);
        }
        assignIds(serviceOrder.getServiceItems(), 1);
        return walkTo(serviceOrder, IN_PROGRESS);
    }

    private static ServiceOrder walkTo(ServiceOrder serviceOrder, ServiceOrderStatus status) {
        pathTo(status).forEach(transition -> transition.apply(serviceOrder));
        serviceOrder.clearDomainEvents();
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
                null),
        APPROVE_QUOTE(
                Set.of(AWAITING_APPROVAL),
                QUOTE_APPROVED,
                serviceOrder -> serviceOrder.approveQuote(USER_ID, DESCRIPTION),
                serviceOrder -> new QuoteApprovedEvent(
                        serviceOrder.getId(),
                        serviceOrder.getPublicId(),
                        serviceOrder.getPartItems().stream()
                                .map(item -> new QuoteApprovedEvent.PartQuantity(item.getPartId(), item.getQuantity()))
                                .toList(),
                        USER_ID)),
        REJECT_QUOTE(
                Set.of(AWAITING_APPROVAL),
                QUOTE_REJECTED,
                serviceOrder -> serviceOrder.rejectQuote(USER_ID, DESCRIPTION),
                null),
        REOPEN_QUOTE(
                Set.of(QUOTE_REJECTED),
                IN_DIAGNOSIS,
                serviceOrder -> serviceOrder.reopenQuote(USER_ID, DESCRIPTION),
                null),
        START_EXECUTION(
                Set.of(QUOTE_APPROVED),
                IN_PROGRESS,
                serviceOrder -> serviceOrder.startExecution(USER_ID, DESCRIPTION),
                null),
        COMPLETE(
                Set.of(IN_PROGRESS),
                COMPLETED,
                serviceOrder -> serviceOrder.complete(USER_ID, DESCRIPTION),
                null),
        DELIVER(
                Set.of(COMPLETED),
                DELIVERED,
                serviceOrder -> serviceOrder.deliver(USER_ID, DESCRIPTION),
                null),
        CANCEL(
                Set.of(RECEIVED, IN_DIAGNOSIS, AWAITING_APPROVAL, QUOTE_REJECTED),
                CANCELED,
                serviceOrder -> serviceOrder.cancel(USER_ID, DESCRIPTION),
                null);

        private final Set<ServiceOrderStatus> from;
        private final ServiceOrderStatus to;
        private final Consumer<ServiceOrder> action;
        private final @Nullable Function<ServiceOrder, Object> event;

        Transition(
                Set<ServiceOrderStatus> from,
                ServiceOrderStatus to,
                Consumer<ServiceOrder> action,
                @Nullable Function<ServiceOrder, Object> event) {
            this.from = from;
            this.to = to;
            this.action = action;
            this.event = event;
        }

        void apply(ServiceOrder serviceOrder) {
            action.accept(serviceOrder);
        }

        List<Object> expectedEvents(ServiceOrder serviceOrder) {
            return event == null ? List.of() : List.of(event.apply(serviceOrder));
        }
    }
}
