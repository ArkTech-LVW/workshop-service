package br.com.arktech.workshop.inventory.application.reserveserviceorderstock;

import static br.com.arktech.workshop.UniqueTestData.uniqueCPF;
import static br.com.arktech.workshop.UniqueTestData.uniquePlate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.auth.application.port.TokenIssuer;
import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.port.PasswordHasher;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;
import br.com.arktech.workshop.inventory.application.serviceorderpart.ServiceOrderPart;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.security.Role;

import com.jayway.jsonpath.JsonPath;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class StockReservationIntegrationTest {

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

    @Autowired
    private ReserveServiceOrderStockUseCase reserveServiceOrderStockUseCase;

    private Long userId;

    @BeforeEach
    void createTheAttendant() {
        long suffix = ThreadLocalRandom.current().nextLong(1_000_000_000L);
        userId = userRepository
                .save(new User(
                        "Atendente",
                        new Email("atendente-%d@workshop.com".formatted(suffix)),
                        "secret",
                        Set.of(Role.ADMIN),
                        passwordHasher))
                .getId();
    }

    @Test
    void shouldReserveOnSendAndTurnTheReservationIntoTheWithdrawalOnApproval() throws Exception {
        Long filter = createPart(5);
        Long serviceOrderId = openServiceOrderWith(filter, 3);
        Instant sentAt = Instant.now();

        sendQuote(serviceOrderId).andExpect(status().isOk());

        part(filter)
                .andExpect(jsonPath("$.stockQuantity").value(5))
                .andExpect(jsonPath("$.reservedQuantity").value(3))
                .andExpect(jsonPath("$.availableQuantity").value(2));
        Instant expiresAt = jdbcTemplate
                .queryForObject(
                        "SELECT expires_at FROM stock_reservation WHERE service_order_id = ?",
                        OffsetDateTime.class,
                        serviceOrderId)
                .toInstant();
        assertThat(expiresAt).isBetween(sentAt.plus(Duration.ofDays(7)), Instant.now().plus(Duration.ofDays(7)));

        as(post("/admin/serviceOrders/{id}/approve", serviceOrderId)).andExpect(status().isOk());

        part(filter)
                .andExpect(jsonPath("$.stockQuantity").value(2))
                .andExpect(jsonPath("$.reservedQuantity").value(0));
        assertThat(reservationCount(serviceOrderId)).isZero();
        assertThat(withdrawnQuantity(serviceOrderId)).isEqualTo(3);
    }

    @Test
    void shouldRefuseToSendWhatAnotherServiceOrderReserved() throws Exception {
        Long filter = createPart(5);
        Long first = openServiceOrderWith(filter, 3);
        Long second = openServiceOrderWith(filter, 3);
        sendQuote(first).andExpect(status().isOk());

        sendQuote(second)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("part.insufficientStock"));

        assertThat(statusOf(second)).isEqualTo("IN_DIAGNOSIS");
        assertThat(reservationCount(second)).isZero();
        part(filter).andExpect(jsonPath("$.reservedQuantity").value(3));
    }

    @Test
    void shouldReleaseTheReservationWhenTheQuoteIsRejected() throws Exception {
        Long filter = createPart(5);
        Long serviceOrderId = openServiceOrderWith(filter, 3);
        sendQuote(serviceOrderId).andExpect(status().isOk());

        as(post("/admin/serviceOrders/{id}/reject", serviceOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Achou caro"}
                                """))
                .andExpect(status().isOk());

        part(filter)
                .andExpect(jsonPath("$.stockQuantity").value(5))
                .andExpect(jsonPath("$.reservedQuantity").value(0));
        assertThat(reservationCount(serviceOrderId)).isZero();
        assertThat(movementCount(serviceOrderId)).isZero();
    }

    @Test
    void shouldReleaseTheReservationWhenTheServiceOrderIsCanceled() throws Exception {
        Long filter = createPart(5);
        Long serviceOrderId = openServiceOrderWith(filter, 3);
        sendQuote(serviceOrderId).andExpect(status().isOk());

        as(post("/admin/serviceOrders/{id}/cancel", serviceOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Cliente desistiu"}
                                """))
                .andExpect(status().isOk());

        part(filter)
                .andExpect(jsonPath("$.stockQuantity").value(5))
                .andExpect(jsonPath("$.reservedQuantity").value(0));
        assertThat(reservationCount(serviceOrderId)).isZero();
        assertThat(movementCount(serviceOrderId)).isZero();
    }

    @Test
    void shouldReserveOnlyWhatIsNewInTheSupplementaryQuote() throws Exception {
        Long pad = createPart(10);
        Long filter = createPart(10);
        Long serviceOrderId = openServiceOrderWith(pad, 2);
        sendQuote(serviceOrderId).andExpect(status().isOk());
        as(post("/admin/serviceOrders/{id}/approve", serviceOrderId)).andExpect(status().isOk());
        as(post("/admin/serviceOrders/{id}/startExecution", serviceOrderId)).andExpect(status().isOk());
        addPart(serviceOrderId, pad, 1);
        addPart(serviceOrderId, filter, 1);

        as(post("/admin/serviceOrders/{id}/sendQuote", serviceOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"canProceedWithoutChanges": true}
                                """))
                .andExpect(status().isOk());

        part(pad).andExpect(jsonPath("$.stockQuantity").value(8)).andExpect(jsonPath("$.reservedQuantity").value(1));
        part(filter).andExpect(jsonPath("$.reservedQuantity").value(1));

        as(post("/admin/serviceOrders/{id}/approve", serviceOrderId)).andExpect(status().isOk());

        part(pad).andExpect(jsonPath("$.stockQuantity").value(7)).andExpect(jsonPath("$.reservedQuantity").value(0));
        part(filter).andExpect(jsonPath("$.stockQuantity").value(9)).andExpect(jsonPath("$.reservedQuantity").value(0));
        assertThat(reservationCount(serviceOrderId)).isZero();
    }

    @Test
    void shouldReleaseTheNewPartsWhenTheCustomerApprovesOnlyTheOriginalQuote() throws Exception {
        Long pad = createPart(10);
        Long filter = createPart(10);
        Long serviceOrderId = openServiceOrderWith(pad, 2);
        sendQuote(serviceOrderId).andExpect(status().isOk());
        as(post("/admin/serviceOrders/{id}/approve", serviceOrderId)).andExpect(status().isOk());
        as(post("/admin/serviceOrders/{id}/startExecution", serviceOrderId)).andExpect(status().isOk());
        addPart(serviceOrderId, filter, 1);
        as(post("/admin/serviceOrders/{id}/sendQuote", serviceOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"canProceedWithoutChanges": true}
                                """))
                .andExpect(status().isOk());

        as(post("/admin/serviceOrders/{id}/approveOriginalOnly", serviceOrderId)).andExpect(status().isOk());

        part(filter)
                .andExpect(jsonPath("$.stockQuantity").value(10))
                .andExpect(jsonPath("$.reservedQuantity").value(0));
        part(pad).andExpect(jsonPath("$.stockQuantity").value(8));
        assertThat(reservationCount(serviceOrderId)).isZero();
    }

    @Test
    void shouldLetOnlyOneOfTwoConcurrentReservationsPassWhenTheStockCoversOne() throws Exception {
        Long filter = createPart(5);
        ServiceOrderPart threeUnits = new ServiceOrderPart(filter, 3);
        CountDownLatch start = new CountDownLatch(1);

        List<Throwable> failures = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Void>> results = new ArrayList<>();
            for (Long serviceOrderId : List.of(openServiceOrder(), openServiceOrder())) {
                ReserveServiceOrderStockCommand command =
                        new ReserveServiceOrderStockCommand(serviceOrderId, List.of(threeUnits));
                results.add(executor.submit(reservation(command, start)));
            }
            start.countDown();
            for (Future<Void> result : results) {
                try {
                    result.get(30, TimeUnit.SECONDS);
                } catch (ExecutionException ex) {
                    failures.add(ex.getCause());
                }
            }
        }

        assertThat(failures)
                .singleElement()
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("message", "part.insufficientStock");
        part(filter).andExpect(jsonPath("$.reservedQuantity").value(3));
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM stock_reservation WHERE part_id = ?", Integer.class, filter))
                .isOne();
    }

    private Callable<Void> reservation(ReserveServiceOrderStockCommand command, CountDownLatch start) {
        return () -> {
            start.await();
            reserveServiceOrderStockUseCase.execute(command);
            return null;
        };
    }

    private Long openServiceOrderWith(Long partId, int quantity) throws Exception {
        Long serviceOrderId = openServiceOrder();
        as(post("/admin/serviceOrders/{id}/startDiagnosis", serviceOrderId)).andExpect(status().isOk());
        addPart(serviceOrderId, partId, quantity);
        return serviceOrderId;
    }

    private void addPart(Long serviceOrderId, Long partId, int quantity) throws Exception {
        as(post("/admin/serviceOrders/{id}/parts", serviceOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"partId": %d, "quantity": %d}
                                """.formatted(partId, quantity)))
                .andExpect(status().isCreated());
    }

    private ResultActions sendQuote(Long serviceOrderId) throws Exception {
        return as(post("/admin/serviceOrders/{id}/sendQuote", serviceOrderId));
    }

    private ResultActions part(Long partId) throws Exception {
        return as(get("/admin/parts/{id}", partId)).andExpect(status().isOk());
    }

    private Long createPart(int stockQuantity) throws Exception {
        String sku = "RS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String body = as(post("/admin/parts").contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku": "%s", "name": "Filtro de óleo", "type": "PART", "unit": "UNIT", "salePrice": 32,
                         "minimumStock": 1}
                        """.formatted(sku)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long partId = ((Number) JsonPath.read(body, "$.id")).longValue();
        as(post("/admin/parts/{id}/stockMovements", partId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "IN", "quantity": %d}
                                """.formatted(stockQuantity)))
                .andExpect(status().isCreated());
        return partId;
    }

    private Long openServiceOrder() throws Exception {
        CPFCNPJ document = uniqueCPF();
        as(post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "document": "%s", "email": "ana@email.com"}
                        """.formatted(document.value())))
                .andExpect(status().isCreated());
        String body = as(post("/admin/serviceOrders").contentType(MediaType.APPLICATION_JSON).content("""
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

    private long reservationCount(Long serviceOrderId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM stock_reservation WHERE service_order_id = ?", Long.class, serviceOrderId);
    }

    private long movementCount(Long serviceOrderId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM stock_movement WHERE service_order_id = ?", Long.class, serviceOrderId);
    }

    private int withdrawnQuantity(Long serviceOrderId) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(quantity), 0) FROM stock_movement WHERE service_order_id = ? AND type = 'OUT'",
                Integer.class,
                serviceOrderId);
    }

    private String statusOf(Long serviceOrderId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM service_order WHERE id = ?", String.class, serviceOrderId);
    }

    private ResultActions as(MockHttpServletRequestBuilder request) throws Exception {
        String token = tokenIssuer.issue(userId, Set.of(Role.ADMIN)).value();
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }
}
