package br.com.arktech.workshop.registry.api.customer;

import static br.com.arktech.workshop.TestUsers.bearer;
import static br.com.arktech.workshop.UniqueTestData.uniqueCNPJ;
import static br.com.arktech.workshop.UniqueTestData.uniqueCPF;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.security.Role;

import com.jayway.jsonpath.JsonPath;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class CustomerManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    static Stream<Arguments> customerEndpoints() {
        return Stream.of(
                Arguments.of(post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of(get("/admin/customers")),
                Arguments.of(get("/admin/customers/1")),
                Arguments.of(put("/admin/customers/1").contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of(delete("/admin/customers/1")));
    }

    @ParameterizedTest
    @MethodSource("customerEndpoints")
    void shouldRequireToken(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("customerEndpoints")
    void shouldDenyMechanics(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.with(bearer(Role.MECHANIC)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.accessDenied"));
    }

    @Test
    void shouldLetAttendantsRegisterFindAndUpdateCustomersButNotDeleteThem() throws Exception {
        CPFCNPJ cpf = uniqueCPF();

        String created = mockMvc.perform(post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana Souza", "document": "%s", "email": "ana@email.com"}
                        """.formatted(cpf.value())).with(bearer(Role.ATTENDANT)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Integer id = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/admin/customers").param("document", cpf.value()).with(bearer(Role.ATTENDANT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id));
        mockMvc.perform(get("/admin/customers/{id}", id).with(bearer(Role.ATTENDANT))).andExpect(status().isOk());
        mockMvc.perform(put("/admin/customers/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana Lima", "document": "%s", "email": "ana@email.com"}
                        """.formatted(cpf.value())).with(bearer(Role.ATTENDANT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Lima"));
        mockMvc.perform(delete("/admin/customers/{id}", id).with(bearer(Role.ATTENDANT)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.accessDenied"));
    }

    @Test
    void shouldCreateFindAndSearchCustomerByDocument() throws Exception {
        CPFCNPJ cpf = uniqueCPF();

        String created = mockMvc.perform(post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana Souza", "document": "%s", "email": "Ana@Email.com", "phone": "(11) 98765-4321"}
                        """.formatted(cpf.formatted())).with(bearer(Role.ADMIN)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.document").value(cpf.value()))
                .andExpect(jsonPath("$.personType").value("NATURAL_PERSON"))
                .andExpect(jsonPath("$.email").value("ana@email.com"))
                .andExpect(jsonPath("$.phone").value("11987654321"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Integer id = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/admin/customers/{id}", id).with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Souza"));
        mockMvc.perform(get("/admin/customers").param("document", cpf.formatted()).with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id));
        mockMvc.perform(get("/admin/customers").param("document", uniqueCPF().value()).with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldRejectDuplicatedDocumentWithOrWithoutMask() throws Exception {
        CPFCNPJ cpf = uniqueCPF();
        createCustomer(cpf.value());

        mockMvc.perform(post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Outra Ana", "document": "%s", "email": "outra@email.com"}
                        """.formatted(cpf.formatted())).with(bearer(Role.ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("customer.documentAlreadyExists"));
    }

    @Test
    void shouldRejectInvalidDocument() throws Exception {
        mockMvc.perform(post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "document": "529.982.247-24", "email": "ana@email.com"}
                        """).with(bearer(Role.ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("document"));
    }

    @Test
    void shouldUpdateDataAndPersonType() throws Exception {
        CPFCNPJ cpf = uniqueCPF();
        CPFCNPJ cnpj = uniqueCNPJ();
        Integer id = createCustomer(cpf.value());

        mockMvc.perform(put("/admin/customers/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana Souza ME", "document": "%s", "email": "Contato@AnaSouza.com"}
                        """.formatted(cnpj.formatted())).with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Souza ME"))
                .andExpect(jsonPath("$.document").value(cnpj.value()))
                .andExpect(jsonPath("$.personType").value("LEGAL_PERSON"))
                .andExpect(jsonPath("$.email").value("contato@anasouza.com"))
                .andExpect(jsonPath("$.phone").value(nullValue()));
        mockMvc.perform(get("/admin/customers").param("document", cpf.value()).with(bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldRejectUpdateToAnotherCustomersDocument() throws Exception {
        CPFCNPJ taken = uniqueCPF();
        createCustomer(taken.value());
        Integer id = createCustomer(uniqueCPF().value());

        mockMvc.perform(put("/admin/customers/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "document": "%s", "email": "ana@email.com"}
                        """.formatted(taken.formatted())).with(bearer(Role.ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("customer.documentAlreadyExists"));
    }

    @Test
    void shouldDeleteCustomer() throws Exception {
        Integer id = createCustomer(uniqueCPF().value());

        mockMvc.perform(delete("/admin/customers/{id}", id).with(bearer(Role.ADMIN))).andExpect(status().isNoContent());
        mockMvc.perform(get("/admin/customers/{id}", id).with(bearer(Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("customer.notFound"));
    }

    @Test
    void shouldKeepCustomerThatIsStillReferenced() throws Exception {
        Integer id = createCustomer(uniqueCPF().value());
        jdbcTemplate.execute("CREATE TABLE customer_reference_test (customer_id BIGINT REFERENCES customer (id))");
        try {
            jdbcTemplate.update("INSERT INTO customer_reference_test (customer_id) VALUES (?)", id);

            mockMvc.perform(delete("/admin/customers/{id}", id).with(bearer(Role.ADMIN)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("customer.inUse"));
            mockMvc.perform(get("/admin/customers/{id}", id).with(bearer(Role.ADMIN))).andExpect(status().isOk());
        } finally {
            jdbcTemplate.execute("DROP TABLE customer_reference_test");
        }
    }

    @Test
    void shouldAnswer404ForUnknownCustomer() throws Exception {
        mockMvc.perform(get("/admin/customers/{id}", Long.MAX_VALUE).with(bearer(Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("customer.notFound"));
    }

    private Integer createCustomer(String document) throws Exception {
        String body = mockMvc.perform(post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "document": "%s", "email": "ana@email.com"}
                        """.formatted(document)).with(bearer(Role.ADMIN)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}
