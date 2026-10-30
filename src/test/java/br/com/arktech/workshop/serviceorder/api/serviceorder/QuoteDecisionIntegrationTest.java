package br.com.arktech.workshop.serviceorder.api.serviceorder;

import static br.com.arktech.workshop.UniqueTestData.uniqueCPF;
import static br.com.arktech.workshop.UniqueTestData.uniquePlate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.auth.application.port.TokenIssuer;
import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.port.PasswordHasher;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;
import br.com.arktech.workshop.serviceorder.application.approvequote.ApproveQuoteCommand;
import br.com.arktech.workshop.serviceorder.application.approvequote.ApproveQuoteUseCase;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.security.Role;
import br.com.arktech.workshop.shared.event.serviceorder.QuoteApprovedEvent;

import com.jayway.jsonpath.JsonPath;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
@RecordApplicationEvents
class QuoteDecisionIntegrationTest {

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
    private ApproveQuoteUseCase approveQuoteUseCase;

    @Autowired
    private ApplicationEvents applicationEvents;

    private Long userId;

    private Long serviceOrderId;

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
    }

    @Test
    void shouldRequireToken() throws Exception {
        mockMvc.perform(approve()).andExpect(status().isUnauthorized());
        mockMvc.perform(reject("Caro")).andExpect(status().isUnauthorized());
        mockMvc.perform(reopenQuote()).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldApproveAndWithdrawThePartsRecordingWhoApproved() throws Exception {
        Long filter = createPart(10);
        Long oil = createPart(8);
        sendQuoteWith(Map.of(filter, 2, oil, 5));

        as(Role.ATTENDANT, approve())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUOTE_APPROVED"));

        assertThat(applicationEvents.stream(QuoteApprovedEvent.class)).hasSize(1);

        assertThat(statusOf()).isEqualTo("QUOTE_APPROVED");
        assertThat(lastStatusChange())
                .containsEntry("from_status", "AWAITING_APPROVAL")
                .containsEntry("to_status", "QUOTE_APPROVED")
                .containsEntry("changed_by_user_id", userId);
        assertThat(stockOf(filter)).isEqualTo(8);
        assertThat(stockOf(oil)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForList(
                        """
                        SELECT part_id, quantity, created_by_user_id FROM stock_movement
                        WHERE service_order_id = ? AND type = 'OUT' ORDER BY part_id
                        """,
                        serviceOrderId))
                .containsExactly(
                        Map.of("part_id", filter, "quantity", 2, "created_by_user_id", userId),
                        Map.of("part_id", oil, "quantity", 5, "created_by_user_id", userId));
    }

    @Test
    void shouldChangeNothingWhenAPartIsShort() throws Exception {
        Long filter = createPart(10);
        Long oil = createPart(4);
        sendQuoteWith(Map.of(filter, 2, oil, 5));

        as(Role.ATTENDANT, approve())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("part.insufficientStock"))
                .andExpect(jsonPath("$.message")
                        .value("Estoque insuficiente para %s: há 4 em estoque e foram pedidos 5."
                                .formatted(skuOf(oil))));

        assertThat(statusOf()).isEqualTo("AWAITING_APPROVAL");
        assertThat(lastStatusChange()).containsEntry("to_status", "AWAITING_APPROVAL");
        assertThat(stockOf(filter)).isEqualTo(10);
        assertThat(stockOf(oil)).isEqualTo(4);
        assertThat(withdrawalCount()).isZero();
    }

    @Test
    void shouldRefuseToApproveTwice() throws Exception {
        Long filter = createPart(10);
        sendQuoteWith(Map.of(filter, 2));
        as(Role.ADMIN, approve()).andExpect(status().isOk());

        as(Role.ADMIN, approve())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.invalidStatusTransition"));

        assertThat(stockOf(filter)).isEqualTo(8);
        assertThat(withdrawalCount()).isOne();
    }

    @Test
    void shouldApproveWithoutLoggedUserWhenTheCustomerApproves() throws Exception {
        Long filter = createPart(10);
        sendQuoteWith(Map.of(filter, 3));
        SecurityContextHolder.clearContext();

        approveQuoteUseCase.execute(new ApproveQuoteCommand(serviceOrderId));

        assertThat(statusOf()).isEqualTo("QUOTE_APPROVED");
        assertThat(lastStatusChange().get("changed_by_user_id")).isNull();
        assertThat(stockOf(filter)).isEqualTo(7);
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT created_by_user_id FROM stock_movement WHERE service_order_id = ?",
                        Long.class,
                        serviceOrderId))
                .isNull();
    }

    @Test
    void shouldRequireTheRejectionReason() throws Exception {
        sendQuoteWith(Map.of(createPart(10), 1));

        as(Role.ATTENDANT, post("/admin/serviceOrders/{id}/reject", serviceOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("reason"));

        assertThat(statusOf()).isEqualTo("AWAITING_APPROVAL");
    }

    @Test
    void shouldRejectWithoutTouchingTheStockAndReopenForANewQuote() throws Exception {
        Long filter = createPart(10);
        sendQuoteWith(Map.of(filter, 2));

        as(Role.ATTENDANT, reject("Cliente achou o valor alto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUOTE_REJECTED"));
        assertThat(lastStatusChange())
                .containsEntry("to_status", "QUOTE_REJECTED")
                .containsEntry("description", "Cliente achou o valor alto");
        assertThat(stockOf(filter)).isEqualTo(10);

        as(Role.ATTENDANT, approve())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.invalidStatusTransition"));
        as(Role.ATTENDANT, reject("De novo"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.invalidStatusTransition"));
        as(Role.ATTENDANT, reopenQuote()).andExpect(status().isForbidden());

        as(Role.ADMIN, reopenQuote()
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Cliente pediu peça mais barata"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_DIAGNOSIS"));
        assertThat(lastStatusChange())
                .containsEntry("from_status", "QUOTE_REJECTED")
                .containsEntry("to_status", "IN_DIAGNOSIS")
                .containsEntry("description", "Cliente pediu peça mais barata");
        assertThat(withdrawalCount()).isZero();
    }

    @Test
    void shouldRefuseToReopenAQuoteThatWasNotRejected() throws Exception {
        as(Role.ADMIN, reopenQuote())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.invalidStatusTransition"));
    }

    private void sendQuoteWith(Map<Long, Integer> quantityByPart) throws Exception {
        as(Role.MECHANIC, post("/admin/serviceOrders/{id}/startDiagnosis", serviceOrderId))
                .andExpect(status().isOk());
        for (Map.Entry<Long, Integer> part : quantityByPart.entrySet()) {
            as(Role.MECHANIC, post("/admin/serviceOrders/{id}/parts", serviceOrderId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"partId": %d, "quantity": %d}
                                    """.formatted(part.getKey(), part.getValue())))
                    .andExpect(status().isCreated());
        }
        as(Role.ATTENDANT, post("/admin/serviceOrders/{id}/sendQuote", serviceOrderId))
                .andExpect(status().isOk());
    }

    private Long createPart(int stockQuantity) throws Exception {
        String sku = "IT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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

    private int stockOf(Long partId) {
        return jdbcTemplate.queryForObject("SELECT stock_quantity FROM part WHERE id = ?", Integer.class, partId);
    }

    private String skuOf(Long partId) {
        return jdbcTemplate.queryForObject("SELECT sku FROM part WHERE id = ?", String.class, partId);
    }

    private long withdrawalCount() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM stock_movement WHERE service_order_id = ?", Long.class, serviceOrderId);
    }

    private String statusOf() {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM service_order WHERE id = ?", String.class, serviceOrderId);
    }

    private Map<String, Object> lastStatusChange() {
        return jdbcTemplate.queryForMap(
                """
                SELECT from_status, to_status, changed_by_user_id, description FROM status_change
                WHERE service_order_id = ? ORDER BY id DESC LIMIT 1
                """,
                serviceOrderId);
    }

    private MockHttpServletRequestBuilder approve() {
        return post("/admin/serviceOrders/{id}/approve", serviceOrderId);
    }

    private MockHttpServletRequestBuilder reject(String reason) {
        return post("/admin/serviceOrders/{id}/reject", serviceOrderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"reason": "%s"}
                        """.formatted(reason));
    }

    private MockHttpServletRequestBuilder reopenQuote() {
        return post("/admin/serviceOrders/{id}/reopenQuote", serviceOrderId);
    }

    private ResultActions as(Role role, MockHttpServletRequestBuilder request) throws Exception {
        String token = tokenIssuer.issue(userId, Set.of(role)).value();
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }
}
