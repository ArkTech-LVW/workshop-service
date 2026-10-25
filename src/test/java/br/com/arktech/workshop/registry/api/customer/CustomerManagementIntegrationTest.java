package br.com.arktech.workshop.registry.api.customer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.auth.application.port.TokenIssuer;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.security.Role;

import com.jayway.jsonpath.JsonPath;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class CustomerManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenIssuer tokenIssuer;

    static Stream<Arguments> customerEndpoints() {
        return Stream.of(
                Arguments.of(post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of(get("/admin/customers")),
                Arguments.of(get("/admin/customers/1")));
    }

    @ParameterizedTest
    @MethodSource("customerEndpoints")
    void shouldRequireToken(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("customerEndpoints")
    void shouldDenyUsersWithoutAdminRole(MockHttpServletRequestBuilder request) throws Exception {
        String token = tokenFor(Role.ATTENDANT, Role.MECHANIC);

        mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("auth.accessDenied"));
    }

    @Test
    void shouldCreateFindAndSearchCustomerByDocument() throws Exception {
        CPFCNPJ cpf = uniqueCPF();

        String created = asAdmin(post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana Souza", "document": "%s", "email": "Ana@Email.com", "phone": "(11) 98765-4321"}
                        """.formatted(cpf.formatted())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.document").value(cpf.value()))
                .andExpect(jsonPath("$.personType").value("NATURAL_PERSON"))
                .andExpect(jsonPath("$.email").value("ana@email.com"))
                .andExpect(jsonPath("$.phone").value("11987654321"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Integer id = JsonPath.read(created, "$.id");

        asAdmin(get("/admin/customers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Souza"));
        asAdmin(get("/admin/customers").param("document", cpf.formatted()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id));
        asAdmin(get("/admin/customers").param("document", uniqueCPF().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldRejectDuplicatedDocumentWithOrWithoutMask() throws Exception {
        CPFCNPJ cpf = uniqueCPF();
        createCustomer(cpf.value());

        asAdmin(post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Outra Ana", "document": "%s", "email": "outra@email.com"}
                        """.formatted(cpf.formatted())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("customer.documentAlreadyExists"));
    }

    @Test
    void shouldRejectInvalidDocument() throws Exception {
        asAdmin(post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "document": "529.982.247-24", "email": "ana@email.com"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("document"));
    }

    @Test
    void shouldAnswer404ForUnknownCustomer() throws Exception {
        asAdmin(get("/admin/customers/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("customer.notFound"));
    }

    private Integer createCustomer(String document) throws Exception {
        String body = asAdmin(post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Ana", "document": "%s", "email": "ana@email.com"}
                        """.formatted(document)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(Role.ADMIN)));
    }

    private String tokenFor(Role... roles) {
        return tokenIssuer.issue(1L, Set.of(roles)).value();
    }

    private static CPFCNPJ uniqueCPF() {
        return withCheckDigits("%09d".formatted(ThreadLocalRandom.current().nextLong(1_000_000_000L)));
    }

    private static CPFCNPJ withCheckDigits(String base) {
        return IntStream.range(0, 100)
                .mapToObj(checkDigits -> base + "%02d".formatted(checkDigits))
                .filter(CPFCNPJ::isValid)
                .findFirst()
                .map(CPFCNPJ::of)
                .orElseThrow();
    }
}
