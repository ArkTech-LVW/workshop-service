package br.com.arktech.workshop.shared.infrastructure.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@ActiveProfiles({"test", "demo"})
class DemoSeedIntegrationTest {

    private static final String PASSWORD = "demo1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @ParameterizedTest
    @CsvSource({
        "gerente@workshop.local, ADMIN",
        "atendente@workshop.local, ATTENDANT",
        "mecanico@workshop.local, MECHANIC"
    })
    void shouldSeedOneUserPerRole(String email, String role) throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, PASSWORD)))
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForList("""
                SELECT user_role.role FROM user_role JOIN "user" ON "user".id = user_role.user_id
                WHERE "user".email = ?
                """, String.class, email))
                .containsExactly(role);
    }

    @Test
    void shouldListTheSeededCustomers() throws Exception {
        mockMvc.perform(get("/admin/customers").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)));
    }

    @Test
    void shouldSeedTheRegistryCatalogAndInventory() {
        assertThat(count("customer WHERE person_type = 'NATURAL_PERSON'")).isEqualTo(2);
        assertThat(count("customer WHERE person_type = 'LEGAL_PERSON'")).isEqualTo(1);
        assertThat(count("vehicle")).isEqualTo(4);
        assertThat(count("service_offering")).isEqualTo(6);
        assertThat(count("part WHERE stock_quantity > 0")).isEqualTo(10);
    }

    @Test
    void shouldSeedServiceOrdersAcrossTheLifecycle() throws Exception {
        assertThat(jdbcTemplate.queryForList("SELECT status FROM service_order ORDER BY number", String.class))
                .containsExactly(
                        "DELIVERED",
                        "DELIVERED",
                        "COMPLETED",
                        "IN_PROGRESS",
                        "AWAITING_APPROVAL",
                        "IN_DIAGNOSIS",
                        "RECEIVED",
                        "CANCELED");

        mockMvc.perform(get("/admin/serviceOrders").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(8)));
    }

    @Test
    void shouldLetTheCustomerSeeTheQuoteAwaitingApproval() throws Exception {
        mockMvc.perform(get("/customer/serviceOrders/7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d05")
                        .header("X-Customer-Document", "529.982.247-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_APPROVAL"));
    }

    @Test
    void shouldKeepServiceOrderTotalsConsistentWithTheirItems() {
        assertThat(count("""
                service_order WHERE total_price <> (
                    SELECT coalesce(sum(quantity * unit_price), 0) FROM service_item
                    WHERE service_item.service_order_id = service_order.id)
                  + (
                    SELECT coalesce(sum(quantity * unit_price), 0) FROM part_item
                    WHERE part_item.service_order_id = service_order.id)
                """)).isZero();
    }

    @Test
    void shouldKeepTheStockConsistentWithMovementsAndReservations() {
        assertThat(count("""
                part WHERE stock_quantity <> (
                    SELECT coalesce(sum(CASE WHEN type = 'IN' THEN quantity ELSE -quantity END), 0)
                    FROM stock_movement WHERE stock_movement.part_id = part.id)
                """)).isZero();
        assertThat(count("""
                part WHERE reserved_quantity <> (
                    SELECT coalesce(sum(quantity), 0) FROM stock_reservation
                    WHERE stock_reservation.part_id = part.id)
                """)).isZero();
        assertThat(count("stock_reservation")).isEqualTo(1);
        assertThat(count("stock_adjustment_request WHERE status = 'PENDING'")).isEqualTo(1);
    }

    @Test
    void shouldContinueTheServiceOrderNumbering() {
        assertThat(jdbcTemplate.queryForObject("SELECT sum(last_value) FROM service_order_number_counter", Long.class))
                .isEqualTo(8L);
    }

    private int count(String fromClause) {
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM " + fromClause, Integer.class);
        return count == null ? 0 : count;
    }

    private String adminToken() throws Exception {
        String body = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "gerente@workshop.local", "password": "%s"}
                        """.formatted(PASSWORD)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
