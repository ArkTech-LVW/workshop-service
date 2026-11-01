package br.com.arktech.workshop.serviceorder.application.notifycustomer;

import static br.com.arktech.workshop.UniqueTestData.uniqueCPF;
import static br.com.arktech.workshop.UniqueTestData.uniquePlate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.auth.application.port.TokenIssuer;
import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.port.PasswordHasher;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;
import br.com.arktech.workshop.shared.application.notification.Notification;
import br.com.arktech.workshop.shared.application.notification.NotificationType;
import br.com.arktech.workshop.shared.application.notification.Notifier;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.security.Role;

import com.jayway.jsonpath.JsonPath;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@IntegrationTest
class ServiceOrderNotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenIssuer tokenIssuer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @MockitoSpyBean
    private Notifier notifier;

    private final List<Delivery> deliveries = new CopyOnWriteArrayList<>();

    private Long userId;

    private Long serviceOrderId;

    private UUID publicId;

    private Long customerId;

    @BeforeEach
    void openAServiceOrder() throws Exception {
        long suffix = ThreadLocalRandom.current().nextLong(1_000_000_000L);
        userId = userRepository
                .save(new User(
                        "Atendente",
                        new Email("atendente-%d@workshop.com".formatted(suffix)),
                        "secret",
                        Set.of(Role.ATTENDANT),
                        passwordHasher))
                .getId();
        serviceOrderId = openServiceOrder();
        publicId = jdbcTemplate.queryForObject(
                "SELECT public_id FROM service_order WHERE id = ?", UUID.class, serviceOrderId);
        customerId = jdbcTemplate.queryForObject(
                "SELECT customer_id FROM service_order WHERE id = ?", Long.class, serviceOrderId);
        doAnswer(invocation -> {
                    deliveries.add(new Delivery(
                            invocation.getArgument(0),
                            TransactionSynchronizationManager.isActualTransactionActive()));
                    return invocation.callRealMethod();
                })
                .when(AopTestUtils.<Notifier>getUltimateTargetObject(notifier))
                .send(any());
    }

    @Test
    void shouldNotifyWithinTheTransactionOfTheQuote() throws Exception {
        sendQuoteWith(createPart(10), 2);

        assertThat(deliveriesOf(NotificationType.SERVICE_ORDER_QUOTE_SENT))
                .singleElement()
                .satisfies(delivery -> {
                    assertThat(delivery.withinTransaction()).isTrue();
                    assertThat(delivery.notification().customerId()).isEqualTo(customerId);
                    assertThat(delivery.notification().parameters()).containsExactly(publicId.toString(), "R$ 64,00");
                });
        assertThat(emailStatusesOf(NotificationType.SERVICE_ORDER_QUOTE_SENT)).containsExactly("PENDING");
    }

    @Test
    void shouldRollBackTheActionWhenTheNotificationCannotBeRecorded() throws Exception {
        doThrow(new IllegalStateException("Notifier unavailable"))
                .when(AopTestUtils.<Notifier>getUltimateTargetObject(notifier))
                .send(any());
        Long partId = createPart(10);
        startDiagnosisWith(partId, 2);

        as(Role.ATTENDANT, sendQuote()).andExpect(status().isInternalServerError());

        assertThat(statusOf(publicId)).isEqualTo("IN_DIAGNOSIS");
        assertThat(emailStatusesOf(NotificationType.SERVICE_ORDER_QUOTE_SENT)).isEmpty();
    }

    @Test
    void shouldNotNotifyWhenTheTransactionIsRolledBack() throws Exception {
        Long partId = createPart(2);
        sendQuoteWith(partId, 2);
        releaseTheReservationAsIfItExpired();
        jdbcTemplate.update("UPDATE part SET stock_quantity = 1 WHERE id = ?", partId);

        as(Role.ATTENDANT, post("/admin/serviceOrders/{id}/approve", serviceOrderId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("part.insufficientStock"));

        assertThat(deliveriesOf(NotificationType.SERVICE_ORDER_QUOTE_APPROVED)).isEmpty();
        assertThat(emailStatusesOf(NotificationType.SERVICE_ORDER_QUOTE_APPROVED)).isEmpty();
        assertThat(statusOf(publicId)).isEqualTo("AWAITING_APPROVAL");
    }

    @Test
    void shouldNotNotifyWhenTheWorkshopWithdrawsTheQuote() throws Exception {
        sendQuoteWith(createPart(10), 2);
        deliveries.clear();

        as(Role.ADMIN, post("/admin/serviceOrders/{id}/reopenQuote", serviceOrderId)).andExpect(status().isOk());

        assertThat(deliveries).isEmpty();
        assertThat(statusOf(publicId)).isEqualTo("IN_DIAGNOSIS");
    }

    private List<String> emailStatusesOf(NotificationType type) {
        return jdbcTemplate.queryForList(
                "SELECT status FROM email_notification WHERE customer_id = ? AND type = ?",
                String.class,
                customerId,
                type.name());
    }

    private List<Delivery> deliveriesOf(NotificationType type) {
        return deliveries.stream()
                .filter(delivery -> delivery.notification().type() == type)
                .filter(delivery -> delivery.notification().parameters().contains(publicId.toString()))
                .toList();
    }

    private void sendQuoteWith(Long partId, int quantity) throws Exception {
        startDiagnosisWith(partId, quantity);
        as(Role.ATTENDANT, sendQuote()).andExpect(status().isOk());
    }

    private void startDiagnosisWith(Long partId, int quantity) throws Exception {
        as(Role.MECHANIC, post("/admin/serviceOrders/{id}/startDiagnosis", serviceOrderId))
                .andExpect(status().isOk());
        as(Role.MECHANIC, post("/admin/serviceOrders/{id}/parts", serviceOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"partId": %d, "quantity": %d}
                                """.formatted(partId, quantity)))
                .andExpect(status().isCreated());
    }

    private MockHttpServletRequestBuilder sendQuote() {
        return post("/admin/serviceOrders/{id}/sendQuote", serviceOrderId);
    }

    private void releaseTheReservationAsIfItExpired() {
        jdbcTemplate.update(
                """
                UPDATE part SET reserved_quantity = part.reserved_quantity - r.quantity
                FROM stock_reservation r
                WHERE r.part_id = part.id AND r.service_order_id = ?
                """,
                serviceOrderId);
        jdbcTemplate.update("DELETE FROM stock_reservation WHERE service_order_id = ?", serviceOrderId);
    }

    private Long createPart(int stockQuantity) throws Exception {
        String sku = "NT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String body = as(Role.ADMIN, post("/admin/parts").contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku": "%s", "name": "Filtro de óleo", "type": "PART", "unit": "UNIT", "salePrice": 32,
                         "minimumStock": 1}
                        """.formatted(sku)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long partId = ((Number) JsonPath.read(body, "$.id")).longValue();
        as(Role.ADMIN, post("/admin/parts/{id}/stockMovements", partId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "IN", "quantity": %d}
                                """.formatted(stockQuantity)))
                .andExpect(status().isCreated());
        return partId;
    }

    private Long openServiceOrder() throws Exception {
        CPFCNPJ document = uniqueCPF();
        as(Role.ADMIN, post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "document": "%s", "email": "ana@email.com"}
                        """.formatted(document.value())))
                .andExpect(status().isCreated());
        String body = as(Role.ATTENDANT, post("/admin/serviceOrders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerDocument": "%s",
                         "vehicle": {"licensePlate": "%s", "brand": "Volkswagen", "model": "Gol", "year": 2020,
                                     "color": "Prata"},
                         "problemDescription": "Barulho ao frear"}
                        """.formatted(document.value(), uniquePlate())))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String statusOf(UUID serviceOrderPublicId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM service_order WHERE public_id = ?", String.class, serviceOrderPublicId);
    }

    private ResultActions as(Role role, MockHttpServletRequestBuilder request) throws Exception {
        String token = tokenIssuer.issue(userId, Set.of(role)).value();
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private record Delivery(Notification notification, boolean withinTransaction) {
    }
}
