package br.com.arktech.workshop.notification.application.sendnotification;

import static br.com.arktech.workshop.UniqueTestData.uniqueCPF;
import static br.com.arktech.workshop.UniqueTestData.uniquePlate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.auth.application.port.TokenIssuer;
import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.port.PasswordHasher;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;
import br.com.arktech.workshop.notification.application.port.Notifier;
import br.com.arktech.workshop.notification.domain.model.Notification;
import br.com.arktech.workshop.notification.domain.model.NotificationType;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.security.Role;

import com.jayway.jsonpath.JsonPath;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class ServiceOrderNotificationIntegrationTest {

    private static final Duration WAIT = Duration.ofSeconds(5);

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

    private final BlockingQueue<Delivery> deliveries = new LinkedBlockingQueue<>();

    private Long userId;

    private Long serviceOrderId;

    private UUID publicId;

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
        doAnswer(invocation -> {
                    Notification notification = invocation.getArgument(0);
                    deliveries.add(new Delivery(
                            notification,
                            statusOf(notification.serviceOrderPublicId()),
                            Thread.currentThread().getName()));
                    return invocation.callRealMethod();
                })
                .when(notifier)
                .send(any());
    }

    @Test
    void shouldNotifyInTheBackgroundOnlyAfterTheQuoteIsCommitted() throws Exception {
        sendQuoteWith(createPart(10), 2);

        Delivery delivery = awaitDelivery(NotificationType.QUOTE_SENT);

        assertThat(delivery.serviceOrderStatus()).isEqualTo("AWAITING_APPROVAL");
        assertThat(delivery.threadName()).startsWith("async-");
        assertThat(delivery.notification().customerId()).isEqualTo(customerIdOf());
        assertThat(delivery.notification().totalPrice()).isEqualByComparingTo("64.00");
        assertThat(delivery.notification().message()).contains(publicId.toString(), "R$ 64,00");
    }

    @Test
    void shouldKeepTheResultOfTheRequestWhenTheNotificationFails() throws Exception {
        doThrow(new IllegalStateException("Notifier unavailable")).when(notifier).send(any());
        Long partId = createPart(10);
        startDiagnosisWith(partId, 2);

        as(Role.ATTENDANT, sendQuote())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_APPROVAL"));

        verify(notifier, timeout(WAIT.toMillis())).send(argThat(notificationOf(NotificationType.QUOTE_SENT)));
        assertThat(statusOf(publicId)).isEqualTo("AWAITING_APPROVAL");
    }

    @Test
    void shouldNotNotifyWhenTheTransactionIsRolledBack() throws Exception {
        sendQuoteWith(createPart(1), 2);
        awaitDelivery(NotificationType.QUOTE_SENT);

        as(Role.ATTENDANT, post("/admin/serviceOrders/{id}/approve", serviceOrderId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("part.insufficientStock"));

        verify(notifier, after(WAIT.toMillis()).never())
                .send(argThat(notificationOf(NotificationType.QUOTE_APPROVED)));
        assertThat(statusOf(publicId)).isEqualTo("AWAITING_APPROVAL");
    }

    private Delivery awaitDelivery(NotificationType type) throws InterruptedException {
        long deadline = System.nanoTime() + WAIT.toNanos();
        while (true) {
            Delivery delivery = deliveries.poll(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
            if (delivery == null) {
                return fail("No %s notification for service order %s".formatted(type, publicId));
            }
            if (notificationOf(type).matches(delivery.notification())) {
                return delivery;
            }
        }
    }

    private ArgumentMatcher<Notification> notificationOf(NotificationType type) {
        return notification -> notification != null
                && notification.type() == type
                && publicId.equals(notification.serviceOrderPublicId());
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

    private Long customerIdOf() {
        return jdbcTemplate.queryForObject(
                "SELECT customer_id FROM service_order WHERE id = ?", Long.class, serviceOrderId);
    }

    private ResultActions as(Role role, MockHttpServletRequestBuilder request) throws Exception {
        String token = tokenIssuer.issue(userId, Set.of(role)).value();
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private record Delivery(Notification notification, String serviceOrderStatus, String threadName) {
    }
}
