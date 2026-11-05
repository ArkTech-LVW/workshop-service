package br.com.arktech.workshop.inventory.api.stockmovement;

import static br.com.arktech.workshop.TestUsers.bearer;
import static br.com.arktech.workshop.UniqueTestData.uniqueCPF;
import static br.com.arktech.workshop.UniqueTestData.uniquePlate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.TestUsers;
import br.com.arktech.workshop.inventory.application.reconcileserviceorderstock.ReconcileServiceOrderStockCommand;
import br.com.arktech.workshop.inventory.application.reconcileserviceorderstock.ReconcileServiceOrderStockUseCase;
import br.com.arktech.workshop.inventory.application.serviceorderpart.ServiceOrderPart;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.security.Role;

import com.jayway.jsonpath.JsonPath;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class StockMovementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUsers testUsers;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReconcileServiceOrderStockUseCase reconcileServiceOrderStockUseCase;

    private Long adminId;

    static Stream<Arguments> stockEndpoints() {
        return Stream.of(
                Arguments.of(post("/admin/parts/1/stockMovements").contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of(get("/admin/parts/1/stockMovements")));
    }

    @BeforeEach
    void setUp() {
        adminId = testUsers.save(Role.ADMIN).getId();
    }

    @ParameterizedTest
    @MethodSource("stockEndpoints")
    void shouldRequireToken(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("stockEndpoints")
    void shouldDenyUsersWithoutAdminRole(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.with(bearer(adminId, Role.ATTENDANT, Role.MECHANIC)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.accessDenied"));
    }

    @Test
    void shouldRecordMovementsAndShowWhoMovedAndWhy() throws Exception {
        Integer partId = createPart(5);

        movement(partId, "IN", 10, "Compra do fornecedor, NF 1234")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("IN"))
                .andExpect(jsonPath("$.createdByUserId").value(adminId))
                .andExpect(jsonPath("$.dateCreated").isNotEmpty());
        movement(partId, "ADJUSTMENT", 7, "Avaria no transporte").andExpect(status().isCreated());

        mockMvc.perform(get("/admin/parts/{id}/stockMovements", partId).with(bearer(adminId, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].type").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.content[0].quantity").value(7))
                .andExpect(jsonPath("$.content[0].reason").value("Avaria no transporte"))
                .andExpect(jsonPath("$.content[0].createdByUserId").value(adminId))
                .andExpect(jsonPath("$.content[1].type").value("IN"))
                .andExpect(jsonPath("$.content[1].reason").value("Compra do fornecedor, NF 1234"));
        mockMvc.perform(get("/admin/parts/{id}", partId).with(bearer(adminId, Role.ADMIN)))
                .andExpect(jsonPath("$.stockQuantity").value(3));
    }

    @Test
    void shouldNeverLetStockGoNegative() throws Exception {
        Integer partId = createPart(0);
        movement(partId, "IN", 2, null).andExpect(status().isCreated());

        movement(partId, "ADJUSTMENT", 3, "Contagem")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("part.insufficientStock"));
        movement(partId, "ADJUSTMENT", 1, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("stockMovement.reasonRequired"));
        movement(partId, "OUT", 1, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("stockMovement.outRequiresServiceOrder"));

        mockMvc.perform(get("/admin/parts/{id}", partId).with(bearer(adminId, Role.ADMIN)))
                .andExpect(jsonPath("$.stockQuantity").value(2));
        mockMvc.perform(get("/admin/parts/{id}/stockMovements", partId).with(bearer(adminId, Role.ADMIN)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldListPartsThatNeedRestocking() throws Exception {
        Integer low = createPart(5);
        Integer enough = createPart(1);
        movement(low, "IN", 5, null).andExpect(status().isCreated());
        movement(enough, "IN", 5, null).andExpect(status().isCreated());

        String body = mockMvc.perform(get("/admin/parts/lowStock").param("size", "100")
                        .with(bearer(adminId, Role.ADMIN)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Integer> ids = JsonPath.read(body, "$.content[*].id");
        assertThat(ids).contains(low).doesNotContain(enough);
    }

    @Test
    void shouldLetOnlyOneOfTwoConcurrentWithdrawalsPassWhenStockCoversOne() throws Exception {
        Integer partId = createPart(0);
        movement(partId, "IN", 5, null).andExpect(status().isCreated());
        ServiceOrderPart threeUnits =
                new ServiceOrderPart(partId.longValue(), 3);
        CountDownLatch start = new CountDownLatch(1);

        List<Throwable> failures = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Void>> results = new ArrayList<>();
            for (Long serviceOrderId : List.of(openServiceOrder(), openServiceOrder())) {
                ReconcileServiceOrderStockCommand command =
                        new ReconcileServiceOrderStockCommand(serviceOrderId, List.of(threeUnits), adminId);
                results.add(executor.submit(withdrawal(command, start)));
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
        mockMvc.perform(get("/admin/parts/{id}", partId).with(bearer(adminId, Role.ADMIN)))
                .andExpect(jsonPath("$.stockQuantity").value(2));
        Integer withdrawals = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM stock_movement WHERE part_id = ? AND type = 'OUT'", Integer.class, partId);
        assertThat(withdrawals).isOne();
    }

    @Test
    void shouldMoveOnlyTheDifferenceFromWhatTheServiceOrderAlreadyTook() throws Exception {
        Integer partId = createPart(0);
        movement(partId, "IN", 5, null).andExpect(status().isCreated());
        Long serviceOrderId = openServiceOrder();

        reconcile(serviceOrderId, partId, 3, adminId);
        reconcile(serviceOrderId, partId, 1, null);
        reconcile(serviceOrderId, partId, 1, null);

        mockMvc.perform(get("/admin/parts/{id}", partId).with(bearer(adminId, Role.ADMIN)))
                .andExpect(jsonPath("$.stockQuantity").value(4));
        mockMvc.perform(get("/admin/parts/{id}/stockMovements", partId).with(bearer(adminId, Role.ADMIN)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].type").value("IN"))
                .andExpect(jsonPath("$.content[0].quantity").value(2))
                .andExpect(jsonPath("$.content[0].serviceOrderId").value(serviceOrderId))
                .andExpect(jsonPath("$.content[0].createdByUserId").doesNotExist())
                .andExpect(jsonPath("$.content[1].type").value("OUT"))
                .andExpect(jsonPath("$.content[1].quantity").value(3));
    }

    private void reconcile(Long serviceOrderId, Integer partId, int quantity, Long userId) {
        reconcileServiceOrderStockUseCase.execute(new ReconcileServiceOrderStockCommand(
                serviceOrderId, List.of(new ServiceOrderPart(partId.longValue(), quantity)), userId));
    }

    private Callable<Void> withdrawal(ReconcileServiceOrderStockCommand command, CountDownLatch start) {
        return () -> {
            start.await();
            reconcileServiceOrderStockUseCase.execute(command);
            return null;
        };
    }

    private Long openServiceOrder() throws Exception {
        CPFCNPJ document = uniqueCPF();
        mockMvc.perform(post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "document": "%s", "email": "ana@email.com"}
                        """.formatted(document.value())).with(bearer(adminId, Role.ADMIN)))
                .andExpect(status().isCreated());
        String body = mockMvc.perform(post("/admin/serviceOrders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerDocument": "%s",
                         "vehicle": {"licensePlate": "%s", "brand": "Volkswagen", "model": "Gol", "year": 2020,
                                     "color": "Prata"},
                         "problemDescription": "Troca de óleo"}
                        """.formatted(document.value(), uniquePlate())).with(bearer(adminId, Role.ADMIN)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions movement(Integer partId, String type, int quantity, String reason) throws Exception {
        String json = reason == null
                ? """
                {"type": "%s", "quantity": %d}
                """.formatted(type, quantity)
                : """
                {"type": "%s", "quantity": %d, "reason": "%s"}
                """.formatted(type, quantity, reason);
        return mockMvc.perform(post("/admin/parts/{id}/stockMovements", partId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(bearer(adminId, Role.ADMIN)));
    }

    private Integer createPart(int minimumStock) throws Exception {
        String sku = "IT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String created = mockMvc.perform(post("/admin/parts").contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku": "%s", "name": "Filtro de óleo", "type": "PART", "unit": "UNIT", "salePrice": 32,
                         "minimumStock": %d}
                        """.formatted(sku, minimumStock)).with(bearer(adminId, Role.ADMIN)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(created, "$.id");
    }
}
