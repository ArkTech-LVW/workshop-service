package br.com.arktech.workshop;

import static br.com.arktech.workshop.UniqueTestData.uniqueCPF;
import static br.com.arktech.workshop.UniqueTestData.uniquePlate;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.port.PasswordHasher;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.security.Role;

import com.jayway.jsonpath.JsonPath;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

@IntegrationTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class MainFlowsIntegrationTest {

    private static final String PASSWORD = "Sup3r-Secret!";

    private static final String CUSTOMER_DOCUMENT = "X-Customer-Document";

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RestClient client;

    private String adminToken;

    private String attendantToken;

    private String mechanicToken;

    private String customerDocument;

    private Long serviceOrderId;

    private String publicId;

    @BeforeEach
    void loginTheStaff() {
        client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {})
                .build();
        adminToken = login(Role.ADMIN);
        attendantToken = login(Role.ATTENDANT);
        mechanicToken = login(Role.MECHANIC);
        customerDocument = createCustomer();
    }

    @Test
    void shouldTakeAServiceOrderFromOpeningToDeliveryWithTheCustomerApprovingByPublicId() {
        Long brakes = createService("Troca de pastilhas", "150.00");
        Long pad = createPart("Pastilha de freio", "45.90", 10);
        openServiceOrder();

        staff(mechanicToken, HttpMethod.POST, "/admin/serviceOrders/{id}/startDiagnosis", null)
                .expect(200)
                .expectStatus("IN_DIAGNOSIS");
        Long brakesItem = addItem("services", "serviceOfferingId", brakes, 1);
        addItem("parts", "partId", pad, 2);
        staff(attendantToken, HttpMethod.POST, "/admin/serviceOrders/{id}/sendQuote", null)
                .expect(200)
                .expectStatus("AWAITING_APPROVAL");

        customer(HttpMethod.POST, "/customer/serviceOrders/{publicId}/approve", null).expect(204);

        assertThat(statusOf()).isEqualTo("QUOTE_APPROVED");
        assertThat(stockOf(pad)).isEqualTo(8);
        assertThat(jdbcTemplate.queryForList(
                        "SELECT part_id, type, quantity FROM stock_movement WHERE service_order_id = ?",
                        serviceOrderId))
                .containsExactly(Map.of("part_id", pad, "type", "OUT", "quantity", 2));

        staff(mechanicToken, HttpMethod.POST, "/admin/serviceOrders/{id}/startExecution", null)
                .expect(200)
                .expectStatus("IN_PROGRESS");
        staff(mechanicToken, HttpMethod.POST, "/admin/serviceOrders/{id}/services/{itemId}/complete", null, brakesItem)
                .expect(200)
                .expectStatus("COMPLETED");
        staff(attendantToken, HttpMethod.POST, "/admin/serviceOrders/{id}/deliver", null)
                .expect(200)
                .expectStatus("DELIVERED");

        Response tracking = customer(HttpMethod.GET, "/customer/serviceOrders/{publicId}", null).expect(200);
        assertThat(tracking.<String>read("$.status")).isEqualTo("DELIVERED");
        assertThat(tracking.<List<String>>read("$.statusHistory[*].status"))
                .containsExactly(
                        "RECEIVED",
                        "IN_DIAGNOSIS",
                        "AWAITING_APPROVAL",
                        "QUOTE_APPROVED",
                        "IN_PROGRESS",
                        "COMPLETED",
                        "DELIVERED");
        assertThat(tracking.<Number>read("$.quote.totalPrice").doubleValue()).isEqualTo(241.80);
    }

    @Test
    void shouldLetTheWorkshopReopenARejectedQuoteForTheCustomerToApproveTheNewOne() {
        Long pad = createPart("Pastilha de freio", "45.90", 10);
        openServiceOrder();
        sendQuoteWithParts(Map.of(pad, 1));

        customer(HttpMethod.POST, "/customer/serviceOrders/{publicId}/reject", """
                        {"reason": "Achei o valor alto"}
                        """)
                .expect(204);

        assertThat(statusOf()).isEqualTo("QUOTE_REJECTED");
        assertThat(stockOf(pad)).isEqualTo(10);
        customer(HttpMethod.POST, "/customer/serviceOrders/{publicId}/approve", null)
                .expect(422)
                .expectCode("serviceOrder.invalidStatusTransition");

        staff(adminToken, HttpMethod.POST, "/admin/serviceOrders/{id}/reopenQuote", """
                        {"description": "Cliente pediu para incluir a troca das pastilhas"}
                        """)
                .expect(200)
                .expectStatus("IN_DIAGNOSIS");
        addItem("services", "serviceOfferingId", createService("Troca de pastilhas", "150.00"), 1);
        staff(attendantToken, HttpMethod.POST, "/admin/serviceOrders/{id}/sendQuote", null)
                .expect(200)
                .expectStatus("AWAITING_APPROVAL");

        Response newQuote = customer(HttpMethod.GET, "/customer/serviceOrders/{publicId}", null).expect(200);
        assertThat(newQuote.<Number>read("$.quote.totalPrice").doubleValue()).isEqualTo(195.90);

        customer(HttpMethod.POST, "/customer/serviceOrders/{publicId}/approve", null).expect(204);

        assertThat(statusOf()).isEqualTo("QUOTE_APPROVED");
        assertThat(stockOf(pad)).isEqualTo(9);
        assertThat(customer(HttpMethod.GET, "/customer/serviceOrders/{publicId}", null)
                        .expect(200)
                        .<List<String>>read("$.statusHistory[*].status"))
                .containsExactly(
                        "RECEIVED",
                        "IN_DIAGNOSIS",
                        "AWAITING_APPROVAL",
                        "QUOTE_REJECTED",
                        "IN_DIAGNOSIS",
                        "AWAITING_APPROVAL",
                        "QUOTE_APPROVED");
    }

    @Test
    void shouldRollBackTheStatusAndTheStockWhenAPartIsShortOnApproval() {
        Long filter = createPart("Filtro de óleo", "32.00", 10);
        Long oil = createPart("Óleo 5W30", "48.00", 4);
        openServiceOrder();
        sendQuoteWithParts(Map.of(filter, 2, oil, 5));

        customer(HttpMethod.POST, "/customer/serviceOrders/{publicId}/approve", null)
                .expect(422)
                .expectCode("part.insufficientStock");

        assertThat(statusOf()).isEqualTo("AWAITING_APPROVAL");
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT to_status FROM status_change WHERE service_order_id = ? ORDER BY id DESC LIMIT 1",
                        String.class,
                        serviceOrderId))
                .isEqualTo("AWAITING_APPROVAL");
        assertThat(stockOf(filter)).isEqualTo(10);
        assertThat(stockOf(oil)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM stock_movement WHERE service_order_id = ?", Long.class, serviceOrderId))
                .isZero();
    }

    @Test
    void shouldBlockRequestsWithoutTokenAndRolesWithoutPermission() {
        openServiceOrder();

        anonymous(HttpMethod.GET, "/admin/serviceOrders/{id}", serviceOrderId).expect(401);
        anonymous(HttpMethod.POST, "/admin/serviceOrders/{id}/startDiagnosis", serviceOrderId).expect(401);
        staff("not-a-valid-token", HttpMethod.GET, "/admin/serviceOrders/{id}", null).expect(401);

        staff(attendantToken, HttpMethod.POST, "/admin/serviceOrders/{id}/startExecution", null).expect(403);
        staff(mechanicToken, HttpMethod.POST, "/admin/serviceOrders/{id}/deliver", null).expect(403);
        staff(attendantToken, HttpMethod.POST, "/admin/serviceOrders/{id}/reopenQuote", null).expect(403);

        assertThat(statusOf()).isEqualTo("RECEIVED");
    }

    @Test
    void shouldShowTheServiceOrderToTheCustomerWithoutInternalFields() {
        Long brakes = createService("Troca de pastilhas", "150.00");
        Long pad = createPart("Pastilha de freio", "45.90", 10);
        openServiceOrder();
        staff(mechanicToken, HttpMethod.POST, "/admin/serviceOrders/{id}/startDiagnosis", null).expect(200);
        addItem("services", "serviceOfferingId", brakes, 1);
        addItem("parts", "partId", pad, 2);
        staff(attendantToken, HttpMethod.POST, "/admin/serviceOrders/{id}/sendQuote", null).expect(200);

        Map<String, Object> internal =
                staff(attendantToken, HttpMethod.GET, "/admin/serviceOrders/{id}", null).expect(200).read("$");
        assertThat(internal).containsKeys("id", "customerId", "vehicleId");

        Response tracking = customer(HttpMethod.GET, "/customer/serviceOrders/{publicId}", null).expect(200);
        assertThat(tracking.<Map<String, Object>>read("$"))
                .containsEntry("publicId", publicId)
                .doesNotContainKeys("id", "customerId", "vehicleId", "mechanicId");
        assertThat(tracking.<List<Map<String, Object>>>read("$.statusHistory"))
                .isNotEmpty()
                .allSatisfy(change -> assertThat(change).doesNotContainKey("changedByUserId"));
        assertThat(tracking.<Map<String, Object>>read("$.quote.services[0]"))
                .containsEntry("name", "Troca de pastilhas")
                .doesNotContainKeys("id", "serviceOfferingId");
        assertThat(tracking.<Map<String, Object>>read("$.quote.parts[0]"))
                .containsEntry("name", "Pastilha de freio")
                .doesNotContainKeys("id", "partId");
    }

    private String login(Role role) {
        String email = "%s-%s@workshop.com".formatted(role.name().toLowerCase(), UUID.randomUUID());
        userRepository.save(new User("Equipe", new Email(email), PASSWORD, Set.of(role), passwordHasher));
        return send(HttpMethod.POST, "/auth/login", """
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, PASSWORD), headers -> {})
                .expect(200)
                .read("$.accessToken");
    }

    private String createCustomer() {
        String document = uniqueCPF().value();
        staff(adminToken, HttpMethod.POST, "/admin/customers", """
                        {"name": "Ana", "document": "%s", "email": "ana@email.com"}
                        """.formatted(document))
                .expect(201);
        return document;
    }

    private Long createService(String name, String laborPrice) {
        return staff(adminToken, HttpMethod.POST, "/admin/services", """
                        {"name": "%s", "laborPrice": %s, "estimatedMinutes": 60}
                        """.formatted(name, laborPrice))
                .expect(201)
                .id();
    }

    private Long createPart(String name, String salePrice, int stockQuantity) {
        String sku = "MF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Long partId = staff(adminToken, HttpMethod.POST, "/admin/parts", """
                        {"sku": "%s", "name": "%s", "type": "PART", "unit": "UNIT", "salePrice": %s,
                         "minimumStock": 1}
                        """.formatted(sku, name, salePrice))
                .expect(201)
                .id();
        staff(adminToken, HttpMethod.POST, "/admin/parts/{partId}/stockMovements", """
                        {"type": "IN", "quantity": %d}
                        """.formatted(stockQuantity), partId)
                .expect(201);
        return partId;
    }

    private void openServiceOrder() {
        Response opened = staff(attendantToken, HttpMethod.POST, "/admin/serviceOrders", """
                        {"customerDocument": "%s",
                         "vehicle": {"licensePlate": "%s", "brand": "Volkswagen", "model": "Gol", "year": 2020,
                                     "color": "Prata"},
                         "problemDescription": "Barulho ao frear"}
                        """.formatted(customerDocument, uniquePlate()))
                .expect(201)
                .expectStatus("RECEIVED");
        serviceOrderId = opened.id();
        publicId = opened.read("$.publicId");
    }

    private void sendQuoteWithParts(Map<Long, Integer> quantityByPart) {
        staff(mechanicToken, HttpMethod.POST, "/admin/serviceOrders/{id}/startDiagnosis", null).expect(200);
        quantityByPart.forEach((partId, quantity) -> addItem("parts", "partId", partId, quantity));
        staff(attendantToken, HttpMethod.POST, "/admin/serviceOrders/{id}/sendQuote", null)
                .expect(200)
                .expectStatus("AWAITING_APPROVAL");
    }

    private Long addItem(String kind, String idField, Long id, int quantity) {
        return staff(mechanicToken, HttpMethod.POST, "/admin/serviceOrders/{id}/" + kind, """
                        {"%s": %d, "quantity": %d}
                        """.formatted(idField, id, quantity))
                .expect(201)
                .id();
    }

    private String statusOf() {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM service_order WHERE id = ?", String.class, serviceOrderId);
    }

    private int stockOf(Long partId) {
        return jdbcTemplate.queryForObject("SELECT stock_quantity FROM part WHERE id = ?", Integer.class, partId);
    }

    private Response staff(String token, HttpMethod method, String path, @Nullable String json, Object... extraIds) {
        Object[] uriVariables = path.contains("{id}") ? prepend(serviceOrderId, extraIds) : extraIds;
        return send(method, path, json, headers -> headers.setBearerAuth(token), uriVariables);
    }

    private Response customer(HttpMethod method, String path, @Nullable String json) {
        return send(method, path, json, headers -> headers.set(CUSTOMER_DOCUMENT, customerDocument), publicId);
    }

    private Response anonymous(HttpMethod method, String path, Object... uriVariables) {
        return send(method, path, null, headers -> {}, uriVariables);
    }

    private Response send(
            HttpMethod method,
            String path,
            @Nullable String json,
            Consumer<HttpHeaders> headers,
            Object... uriVariables) {
        RestClient.RequestBodySpec request = client.method(method).uri(path, uriVariables).headers(headers);
        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).body(json);
        }
        ResponseEntity<String> response = request.retrieve().toEntity(String.class);
        return new Response(response.getStatusCode().value(), response.getBody());
    }

    private static Object[] prepend(Object first, Object[] rest) {
        Object[] all = new Object[rest.length + 1];
        all[0] = first;
        System.arraycopy(rest, 0, all, 1, rest.length);
        return all;
    }

    private record Response(int status, @Nullable String body) {

        Response expect(int expected) {
            assertThat(status).as("HTTP status, body: %s", body).isEqualTo(expected);
            return this;
        }

        Response expectStatus(String serviceOrderStatus) {
            assertThat(this.<String>read("$.status")).isEqualTo(serviceOrderStatus);
            return this;
        }

        Response expectCode(String code) {
            assertThat(this.<String>read("$.code")).isEqualTo(code);
            return this;
        }

        Long id() {
            return this.<Number>read("$.id").longValue();
        }

        <T> T read(String path) {
            return JsonPath.read(body, path);
        }
    }
}
