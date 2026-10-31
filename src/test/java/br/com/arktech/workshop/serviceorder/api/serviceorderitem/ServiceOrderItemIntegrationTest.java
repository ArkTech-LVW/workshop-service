package br.com.arktech.workshop.serviceorder.api.serviceorderitem;

import static br.com.arktech.workshop.UniqueTestData.uniqueCPF;
import static br.com.arktech.workshop.UniqueTestData.uniquePlate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.auth.application.port.TokenIssuer;
import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.port.PasswordHasher;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;
import br.com.arktech.workshop.catalog.domain.model.ServiceOffering;
import br.com.arktech.workshop.catalog.domain.repository.ServiceOfferingRepository;
import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.repository.ServiceOrderRepository;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.money.Money;
import br.com.arktech.workshop.shared.domain.security.Role;

import com.jayway.jsonpath.JsonPath;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class ServiceOrderItemIntegrationTest {

    private static final Money LABOR_PRICE = new Money(new BigDecimal("150.00"));
    private static final Money PAD_PRICE = new Money(new BigDecimal("35.90"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenIssuer tokenIssuer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private ServiceOfferingRepository serviceOfferingRepository;

    @Autowired
    private PartRepository partRepository;

    @Autowired
    private ServiceOrderRepository serviceOrderRepository;

    private Long userId;

    private Long serviceOrderId;

    @BeforeEach
    void openAServiceOrder() throws Exception {
        long suffix = ThreadLocalRandom.current().nextLong(1_000_000_000L);
        userId = userRepository
                .save(new User(
                        "Mecânico",
                        new Email("mecanico-%d@workshop.com".formatted(suffix)),
                        "secret",
                        Set.of(Role.MECHANIC),
                        passwordHasher))
                .getId();
        serviceOrderId = openServiceOrder();
    }

    @Test
    void shouldRequireToken() throws Exception {
        mockMvc.perform(addService(serviceOrderId, savedService(LABOR_PRICE), 1)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldLetMechanicsBuildAndUndoTheQuote() throws Exception {
        Long serviceItemId = idOf(as(Role.MECHANIC, addService(serviceOrderId, savedService(LABOR_PRICE), 2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.unitPrice").value(150.0))
                .andExpect(jsonPath("$.subtotal").value(300.0))
                .andExpect(jsonPath("$.serviceOrderTotalPrice").value(300.0)));
        Long partItemId = idOf(as(Role.MECHANIC, addPart(serviceOrderId, savedPart(5), 3))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unitPrice").value(35.9))
                .andExpect(jsonPath("$.subtotal").value(107.7))
                .andExpect(jsonPath("$.serviceOrderTotalPrice").value(407.7))
                .andExpect(jsonPath("$.availableQuantity").value(5))
                .andExpect(jsonPath("$.insufficientStock").doesNotExist()));
        assertThat(totalPrice()).isEqualByComparingTo("407.70");

        as(Role.MECHANIC, delete("/admin/serviceOrders/{id}/services/{itemId}", serviceOrderId, serviceItemId))
                .andExpect(status().isNoContent());
        as(Role.MECHANIC, delete("/admin/serviceOrders/{id}/parts/{itemId}", serviceOrderId, partItemId))
                .andExpect(status().isNoContent());

        assertThat(itemCount()).isZero();
        assertThat(totalPrice()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldKeepThePriceOfTheMomentOfInclusion() throws Exception {
        Long serviceOfferingId = savedService(LABOR_PRICE);
        as(Role.ATTENDANT, addService(serviceOrderId, serviceOfferingId, 1)).andExpect(status().isCreated());
        ServiceOffering serviceOffering = serviceOfferingRepository.findRequiredById(serviceOfferingId);
        serviceOffering.changeLaborPrice(new Money(new BigDecimal("200.00")));
        serviceOfferingRepository.save(serviceOffering);

        as(Role.ATTENDANT, addService(serviceOrderId, serviceOfferingId, 1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unitPrice").value(200.0))
                .andExpect(jsonPath("$.serviceOrderTotalPrice").value(350.0));

        assertThat(jdbcTemplate.queryForList(
                        "SELECT unit_price FROM service_item WHERE service_order_id = ? ORDER BY id",
                        BigDecimal.class,
                        serviceOrderId))
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("150.00"), new BigDecimal("200.00"));
    }

    @Test
    void shouldRefuseWhatTheCatalogOrTheStockDoNotHave() throws Exception {
        ServiceOffering inactive = serviceOfferingRepository.save(new ServiceOffering("Antigo", null, LABOR_PRICE, 30));
        inactive.deactivate();
        serviceOfferingRepository.save(inactive);

        as(Role.MECHANIC, addService(serviceOrderId, inactive.getId(), 1))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOffering.inactive"));
        as(Role.MECHANIC, addService(serviceOrderId, Long.MAX_VALUE, 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("serviceOffering.notFound"));
        as(Role.MECHANIC, addPart(serviceOrderId, Long.MAX_VALUE, 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("part.notFound"));
        as(Role.MECHANIC, addService(Long.MAX_VALUE, savedService(LABOR_PRICE), 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("serviceOrder.notFound"));
        as(Role.MECHANIC, addPart(serviceOrderId, savedPart(5), 1000))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("quantity"));
        as(Role.MECHANIC, addPart(serviceOrderId, savedPart(2), 3))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("part.insufficientStock"));

        assertThat(itemCount()).isZero();
    }

    @Test
    void shouldLockTheItemsOnceTheQuoteIsSent() throws Exception {
        Long serviceItemId = idOf(as(Role.MECHANIC, addService(serviceOrderId, savedService(LABOR_PRICE), 1))
                .andExpect(status().isCreated()));
        sendQuote();

        as(Role.MECHANIC, addPart(serviceOrderId, savedPart(5), 1))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.itemsLocked"));
        as(Role.MECHANIC, delete("/admin/serviceOrders/{id}/services/{itemId}", serviceOrderId, serviceItemId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.itemsLocked"));

        assertThat(itemCount()).isOne();
    }

    @Test
    void shouldRefuseATotalAboveWhatTheColumnHolds() throws Exception {
        as(Role.MECHANIC, addService(serviceOrderId, savedService(new Money(new BigDecimal("9999999999.99"))), 1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceOrderTotalPrice").value(9999999999.99));

        as(Role.MECHANIC, addService(serviceOrderId, savedService(new Money(new BigDecimal("1.00"))), 1))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.totalPriceTooHigh"));

        assertThat(itemCount()).isOne();
        assertThat(totalPrice()).isEqualByComparingTo("9999999999.99");
    }

    @Test
    void shouldNotRemoveAnItemThroughAnotherServiceOrder() throws Exception {
        Long serviceItemId = idOf(as(Role.MECHANIC, addService(serviceOrderId, savedService(LABOR_PRICE), 1))
                .andExpect(status().isCreated()));
        Long otherServiceOrderId = openServiceOrder();

        as(Role.MECHANIC, delete("/admin/serviceOrders/{id}/services/{itemId}", otherServiceOrderId, serviceItemId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("serviceOrder.itemNotFound"));

        assertThat(itemCount()).isOne();
    }

    private Long openServiceOrder() throws Exception {
        CPFCNPJ document = uniqueCPF();
        as(Role.ADMIN, post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "document": "%s", "email": "ana@email.com"}
                        """.formatted(document.value())))
                .andExpect(status().isCreated());
        return idOf(as(Role.ATTENDANT, post("/admin/serviceOrders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerDocument": "%s",
                         "vehicle": {"licensePlate": "%s", "brand": "Volkswagen", "model": "Gol", "year": 2020,
                                     "color": "Prata"},
                         "problemDescription": "Barulho ao frear"}
                        """.formatted(document.value(), uniquePlate())))
                .andExpect(status().isCreated()));
    }

    private void sendQuote() {
        transactionTemplate.executeWithoutResult(transaction -> {
            ServiceOrder serviceOrder = serviceOrderRepository.findRequiredById(serviceOrderId);
            serviceOrder.startDiagnosis(userId, null);
            serviceOrder.sendQuote(userId, null, null);
            serviceOrderRepository.save(serviceOrder);
        });
    }

    private Long savedService(Money laborPrice) {
        return serviceOfferingRepository.save(new ServiceOffering("Freios", null, laborPrice, 60)).getId();
    }

    private Long savedPart(int stockQuantity) {
        String sku = "PAST-%d".formatted(ThreadLocalRandom.current().nextLong(1_000_000_000L));
        Part part = partRepository.save(
                new Part(new Sku(sku), "Pastilha de freio", PartType.PART, MeasurementUnit.UNIT, PAD_PRICE, 1));
        part.increaseStock(stockQuantity);
        return partRepository.save(part).getId();
    }

    private long itemCount() {
        List<Long> counts = jdbcTemplate.queryForList(
                """
                SELECT (SELECT count(*) FROM service_item WHERE service_order_id = ?)
                     + (SELECT count(*) FROM part_item WHERE service_order_id = ?)
                """,
                Long.class,
                serviceOrderId,
                serviceOrderId);
        return counts.getFirst();
    }

    private BigDecimal totalPrice() {
        return jdbcTemplate.queryForObject(
                "SELECT total_price FROM service_order WHERE id = ?", BigDecimal.class, serviceOrderId);
    }

    private static MockHttpServletRequestBuilder addService(Long serviceOrderId, Long serviceOfferingId, int quantity) {
        return post("/admin/serviceOrders/{id}/services", serviceOrderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"serviceOfferingId": %d, "quantity": %d}
                        """.formatted(serviceOfferingId, quantity));
    }

    private static MockHttpServletRequestBuilder addPart(Long serviceOrderId, Long partId, int quantity) {
        return post("/admin/serviceOrders/{id}/parts", serviceOrderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"partId": %d, "quantity": %d}
                        """.formatted(partId, quantity));
    }

    private ResultActions as(Role role, MockHttpServletRequestBuilder request) throws Exception {
        String token = tokenIssuer.issue(userId, Set.of(role)).value();
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private static Long idOf(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
