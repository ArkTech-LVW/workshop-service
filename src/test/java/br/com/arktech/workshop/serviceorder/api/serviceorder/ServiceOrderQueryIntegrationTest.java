package br.com.arktech.workshop.serviceorder.api.serviceorder;

import static br.com.arktech.workshop.UniqueTestData.uniqueCPF;
import static br.com.arktech.workshop.UniqueTestData.uniquePlate;
import static br.com.arktech.workshop.UniqueTestData.withHyphen;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import br.com.arktech.workshop.registry.domain.model.Customer;
import br.com.arktech.workshop.registry.domain.model.Vehicle;
import br.com.arktech.workshop.registry.domain.repository.CustomerRepository;
import br.com.arktech.workshop.registry.domain.repository.VehicleRepository;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.serviceorder.domain.repository.ServiceOrderRepository;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.money.Money;
import br.com.arktech.workshop.shared.domain.security.Role;
import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import jakarta.persistence.EntityManagerFactory;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class ServiceOrderQueryIntegrationTest {

    private static final Money LABOR_PRICE = new Money(new BigDecimal("150.00"));
    private static final Money PART_PRICE = new Money(new BigDecimal("35.90"));
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);

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
    private CustomerRepository customerRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ServiceOfferingRepository serviceOfferingRepository;

    @Autowired
    private PartRepository partRepository;

    @Autowired
    private ServiceOrderRepository serviceOrderRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Long userId;

    private CPFCNPJ document;

    private Long customerId;

    private String golPlate;

    private Long golId;

    private Long unoId;

    @BeforeEach
    void saveTheCustomerAndTheirVehicles() {
        long suffix = ThreadLocalRandom.current().nextLong(1_000_000_000L);
        Email email = new Email("atendente-%d@workshop.com".formatted(suffix));
        userId = userRepository
                .save(new User("Atendente", email, "secret", Set.of(Role.ATTENDANT), passwordHasher))
                .getId();
        document = uniqueCPF();
        customerId = customerRepository
                .save(new Customer("Ana", document, new Email("ana@email.com"), null))
                .getId();
        golPlate = uniquePlate();
        golId = saveVehicle(golPlate);
        unoId = saveVehicle(uniquePlate());
    }

    @Test
    void shouldRequireToken() throws Exception {
        mockMvc.perform(get("/admin/serviceOrders")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/admin/serviceOrders/{id}", 1L)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldListOpenServiceOrdersFirstAndTheOldestFirst() throws Exception {
        Long received = openServiceOrder(golId, daysAgo(3), ServiceOrderStatus.RECEIVED);
        Long delivered = openServiceOrder(golId, daysAgo(5), ServiceOrderStatus.DELIVERED);
        Long inProgress = openServiceOrder(unoId, daysAgo(1), ServiceOrderStatus.IN_PROGRESS);
        Long canceled = openServiceOrder(unoId, daysAgo(4), ServiceOrderStatus.CANCELED);
        Long awaitingApproval = openServiceOrder(golId, daysAgo(10), ServiceOrderStatus.AWAITING_APPROVAL);

        asAttendant(get("/admin/serviceOrders").param("customerDocument", document.formatted()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id")
                        .value(contains(asInts(awaitingApproval, received, inProgress, delivered, canceled))))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    void shouldPaginateKeepingTheOrder() throws Exception {
        Long oldest = openServiceOrder(golId, daysAgo(3), ServiceOrderStatus.RECEIVED);
        Long middle = openServiceOrder(golId, daysAgo(2), ServiceOrderStatus.RECEIVED);
        Long newest = openServiceOrder(golId, daysAgo(1), ServiceOrderStatus.RECEIVED);

        asAttendant(get("/admin/serviceOrders")
                        .param("customerDocument", document.value())
                        .param("size", "2"))
                .andExpect(jsonPath("$.content[*].id").value(contains(asInts(oldest, middle))))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        asAttendant(get("/admin/serviceOrders")
                        .param("customerDocument", document.value())
                        .param("size", "2")
                        .param("page", "1"))
                .andExpect(jsonPath("$.content[*].id").value(contains(asInts(newest))))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void shouldFilterByPlateStatusAndOpeningPeriod() throws Exception {
        Long golLastWeek = openServiceOrder(golId, daysAgo(7), ServiceOrderStatus.RECEIVED);
        Long golYesterday = openServiceOrder(golId, daysAgo(1), ServiceOrderStatus.RECEIVED);
        Long golInDiagnosis = openServiceOrder(golId, daysAgo(2), ServiceOrderStatus.IN_DIAGNOSIS);
        openServiceOrder(unoId, daysAgo(1), ServiceOrderStatus.RECEIVED);

        asAttendant(get("/admin/serviceOrders").param("licensePlate", withHyphen(golPlate.toLowerCase())))
                .andExpect(jsonPath("$.content[*].id")
                        .value(contains(asInts(golLastWeek, golInDiagnosis, golYesterday))));
        asAttendant(get("/admin/serviceOrders").param("licensePlate", golPlate).param("status", "RECEIVED"))
                .andExpect(jsonPath("$.content[*].id").value(contains(asInts(golLastWeek, golYesterday))));
        asAttendant(get("/admin/serviceOrders")
                        .param("licensePlate", golPlate)
                        .param("openedFrom", daysAgo(2).toString())
                        .param("openedTo", daysAgo(1).toString()))
                .andExpect(jsonPath("$.content[*].id").value(contains(asInts(golInDiagnosis, golYesterday))));
        asAttendant(get("/admin/serviceOrders")
                        .param("customerDocument", document.value())
                        .param("openedTo", daysAgo(3).toString()))
                .andExpect(jsonPath("$.content[*].id").value(contains(asInts(golLastWeek))));
    }

    @Test
    void shouldAnswerAnEmptyPageForAnUnregisteredDocumentOrPlate() throws Exception {
        openServiceOrder(golId, daysAgo(1), ServiceOrderStatus.RECEIVED);

        asAttendant(get("/admin/serviceOrders").param("customerDocument", uniqueCPF().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
        asAttendant(get("/admin/serviceOrders")
                        .param("customerDocument", document.value())
                        .param("licensePlate", uniquePlate()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void shouldDetailItemsQuoteStatusHistoryAndTimes() throws Exception {
        Long id = openServiceOrderWithItems(1, 1);

        asAttendant(get("/admin/serviceOrders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("AWAITING_APPROVAL"))
                .andExpect(jsonPath("$.services.length()").value(1))
                .andExpect(jsonPath("$.parts.length()").value(1))
                .andExpect(jsonPath("$.servicesTotal").value(150.00))
                .andExpect(jsonPath("$.partsTotal").value(35.90))
                .andExpect(jsonPath("$.totalPrice").value(185.90))
                .andExpect(jsonPath("$.statusHistory[*].toStatus")
                        .value(contains("RECEIVED", "IN_DIAGNOSIS", "AWAITING_APPROVAL")))
                .andExpect(jsonPath("$.statusHistory[1].fromStatus").value("RECEIVED"))
                .andExpect(jsonPath("$.statusHistory[1].changedByUserId").value(userId))
                .andExpect(jsonPath("$.statusHistory[1].description").value("Pastilhas gastas"))
                .andExpect(jsonPath("$.statusHistory[2].changedAt").isNotEmpty());
    }

    @Test
    void shouldAnswer404ForAnUnknownServiceOrder() throws Exception {
        asAttendant(get("/admin/serviceOrders/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("serviceOrder.notFound"));
    }

    @Test
    void shouldListWithTheSameQueriesWhateverTheNumberOfServiceOrders() throws Exception {
        openServiceOrderWithItems(2, 2);
        long oneServiceOrder = statementsToRun(get("/admin/serviceOrders").param("customerDocument", document.value()));
        IntStream.range(0, 4).forEach(ignored -> openServiceOrderWithItems(2, 2));

        long fiveServiceOrders =
                statementsToRun(get("/admin/serviceOrders").param("customerDocument", document.value()));

        assertThat(oneServiceOrder).isPositive();
        assertThat(fiveServiceOrders).isEqualTo(oneServiceOrder);
    }

    @Test
    void shouldDetailWithTheSameQueriesWhateverTheNumberOfItems() throws Exception {
        Long fewItems = openServiceOrderWithItems(1, 1);
        Long manyItems = openServiceOrderWithItems(5, 5);

        long fewItemsStatements = statementsToRun(get("/admin/serviceOrders/{id}", fewItems));
        long manyItemsStatements = statementsToRun(get("/admin/serviceOrders/{id}", manyItems));

        assertThat(fewItemsStatements).isPositive();
        assertThat(manyItemsStatements).isEqualTo(fewItemsStatements);
    }

    private long statementsToRun(MockHttpServletRequestBuilder request) throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        asAttendant(request).andExpect(status().isOk());
        return statistics.getPrepareStatementCount();
    }

    private Long openServiceOrder(Long vehicleId, Instant openedAt, ServiceOrderStatus status) {
        Long id = transactionTemplate.execute(transaction -> serviceOrderRepository
                .save(new ServiceOrder(
                        serviceOrderRepository.nextNumber(), customerId, vehicleId, "Barulho ao frear", userId))
                .getId());
        jdbcTemplate.update(
                "UPDATE service_order SET date_created = ?, status = ? WHERE id = ?",
                Timestamp.from(openedAt),
                status.name(),
                id);
        return id;
    }

    private Long openServiceOrderWithItems(int services, int parts) {
        Long serviceOfferingId = serviceOfferingRepository
                .save(new ServiceOffering("Troca de pastilhas", null, LABOR_PRICE, 60))
                .getId();
        Long partId = savePart();
        return transactionTemplate.execute(transaction -> {
            ServiceOrder serviceOrder = new ServiceOrder(
                    serviceOrderRepository.nextNumber(), customerId, golId, "Barulho ao frear", userId);
            IntStream.range(0, services)
                    .forEach(ignored -> serviceOrder.addServiceItem(serviceOfferingId, 1, LABOR_PRICE));
            IntStream.range(0, parts).forEach(ignored -> serviceOrder.addPartItem(partId, 1, PART_PRICE));
            serviceOrder.startDiagnosis(userId, "Pastilhas gastas");
            serviceOrder.sendQuote(userId, null);
            return serviceOrderRepository.save(serviceOrder).getId();
        });
    }

    private Long saveVehicle(String plate) {
        return vehicleRepository
                .save(new Vehicle(customerId, new LicensePlate(plate), null, "Volkswagen", "Gol", 2020, "Prata"))
                .getId();
    }

    private Long savePart() {
        String sku = "PAST-%d".formatted(ThreadLocalRandom.current().nextLong(1_000_000_000L));
        return partRepository
                .save(new Part(new Sku(sku), "Pastilha de freio", PartType.PART, MeasurementUnit.UNIT, PART_PRICE, 1))
                .getId();
    }

    private static Instant daysAgo(int days) {
        return NOW.minus(Duration.ofDays(days));
    }

    private static Integer[] asInts(Long... ids) {
        return Arrays.stream(ids).map(Long::intValue).toArray(Integer[]::new);
    }

    private ResultActions asAttendant(MockHttpServletRequestBuilder request) throws Exception {
        String token = tokenIssuer.issue(userId, Set.of(Role.ATTENDANT)).value();
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }
}
