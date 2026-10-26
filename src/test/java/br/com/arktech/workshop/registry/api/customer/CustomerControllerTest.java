package br.com.arktech.workshop.registry.api.customer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.registry.application.createcustomer.CreateCustomerCommand;
import br.com.arktech.workshop.registry.application.createcustomer.CreateCustomerUseCase;
import br.com.arktech.workshop.registry.application.customer.CustomerResult;
import br.com.arktech.workshop.registry.application.deletecustomer.DeleteCustomerCommand;
import br.com.arktech.workshop.registry.application.deletecustomer.DeleteCustomerUseCase;
import br.com.arktech.workshop.registry.application.findcustomer.FindCustomerUseCase;
import br.com.arktech.workshop.registry.application.listcustomers.ListCustomersQuery;
import br.com.arktech.workshop.registry.application.listcustomers.ListCustomersUseCase;
import br.com.arktech.workshop.registry.application.updatecustomer.UpdateCustomerCommand;
import br.com.arktech.workshop.registry.application.updatecustomer.UpdateCustomerUseCase;
import br.com.arktech.workshop.shared.api.error.ApiErrorTestConfig;
import br.com.arktech.workshop.shared.domain.document.PersonType;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

@WebMvcTest(controllers = CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiErrorTestConfig.class)
class CustomerControllerTest {

    private static final CustomerResult ANA = new CustomerResult(
            1L, "Ana Souza", "52998224725", PersonType.NATURAL_PERSON, "ana@email.com", "11987654321");
    private static final String ANA_JSON = """
            {"id": 1, "name": "Ana Souza", "document": "52998224725", "personType": "NATURAL_PERSON",
             "email": "ana@email.com", "phone": "11987654321"}
            """;
    private static final String ANA_REQUEST = """
            {"name": "Ana Souza", "document": "529.982.247-25", "email": "ana@email.com", "phone": "(11) 98765-4321"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCustomerUseCase createCustomerUseCase;

    @MockitoBean
    private ListCustomersUseCase listCustomersUseCase;

    @MockitoBean
    private FindCustomerUseCase findCustomerUseCase;

    @MockitoBean
    private UpdateCustomerUseCase updateCustomerUseCase;

    @MockitoBean
    private DeleteCustomerUseCase deleteCustomerUseCase;

    @Test
    void shouldCreateCustomer() throws Exception {
        when(createCustomerUseCase.execute(any())).thenReturn(ANA);

        mockMvc.perform(post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content(ANA_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(json(ANA_JSON));
        verify(createCustomerUseCase)
                .execute(new CreateCustomerCommand("Ana Souza", "529.982.247-25", "ana@email.com", "(11) 98765-4321"));
    }

    @Test
    void shouldCreateCustomerWithoutPhone() throws Exception {
        when(createCustomerUseCase.execute(any())).thenReturn(ANA);

        mockMvc.perform(post("/admin/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ana Souza", "document": "52998224725", "email": "ana@email.com"}
                                """))
                .andExpect(status().isCreated());
        verify(createCustomerUseCase)
                .execute(new CreateCustomerCommand("Ana Souza", "52998224725", "ana@email.com", null));
    }

    @Test
    void shouldRequireNameDocumentAndEmail() throws Exception {
        mockMvc.perform(post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {
                          "code": "request.invalidFields",
                          "details": [
                            {"field": "document", "message": "Não pode estar em branco."},
                            {"field": "email", "message": "Não pode estar em branco."},
                            {"field": "name", "message": "Não pode estar em branco."}
                          ]
                        }
                        """,
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(createCustomerUseCase);
    }

    @Test
    void shouldValidateNewCustomer() throws Exception {
        mockMvc.perform(post("/admin/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "document": "529.982.247-24", "email": "not-an-email",
                                 "phone": "9876-5432"}
                                """.formatted("a".repeat(151))))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {
                          "code": "request.invalidFields",
                          "details": [
                            {"field": "document", "message": "CPF ou CNPJ inválido."},
                            {"field": "email", "message": "E-mail inválido."},
                            {"field": "name", "message": "Deve ter no máximo 150 caracteres."},
                            {"field": "phone",
                             "message": "Telefone inválido. Informe o DDD e o número, como (11) 98765-4321."}
                          ]
                        }
                        """,
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(createCustomerUseCase);
    }

    @Test
    void shouldAnswer409WhenDocumentIsTaken() throws Exception {
        when(createCustomerUseCase.execute(any()))
                .thenThrow(new ConflictException("customer.documentAlreadyExists", "529.982.247-25"));

        mockMvc.perform(post("/admin/customers").contentType(MediaType.APPLICATION_JSON).content(ANA_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(json("""
                        {"code": "customer.documentAlreadyExists",
                         "message": "Já existe um cliente com o CPF/CNPJ 529.982.247-25.", "details": null}
                        """));
    }

    @Test
    void shouldListWithDefaultPagination() throws Exception {
        when(listCustomersUseCase.execute(new ListCustomersQuery(null, 0, 20)))
                .thenReturn(new Page<>(List.of(ANA), new Pagination(0, 20), 1));

        mockMvc.perform(get("/admin/customers"))
                .andExpect(status().isOk())
                .andExpect(json("""
                        {"content": [%s], "page": 0, "size": 20, "totalElements": 1, "totalPages": 1}
                        """.formatted(ANA_JSON)));
    }

    @Test
    void shouldFilterByDocument() throws Exception {
        when(listCustomersUseCase.execute(new ListCustomersQuery("529.982.247-25", 0, 20)))
                .thenReturn(new Page<>(List.of(ANA), new Pagination(0, 20), 1));

        mockMvc.perform(get("/admin/customers").param("document", "529.982.247-25"))
                .andExpect(status().isOk())
                .andExpect(json("""
                        {"content": [%s], "totalElements": 1}
                        """.formatted(ANA_JSON), JsonCompareMode.LENIENT));
    }

    @Test
    void shouldValidateDocumentFilterAndPagination() throws Exception {
        mockMvc.perform(get("/admin/customers")
                        .param("document", "123")
                        .param("page", "-1")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {
                          "code": "request.invalidFields",
                          "details": [
                            {"field": "document", "message": "CPF ou CNPJ inválido."},
                            {"field": "page", "message": "Deve ser maior ou igual a zero."},
                            {"field": "size", "message": "Deve ser no máximo 100."}
                          ]
                        }
                        """,
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(listCustomersUseCase);
    }

    @Test
    void shouldFindCustomer() throws Exception {
        when(findCustomerUseCase.execute(any())).thenReturn(ANA);

        mockMvc.perform(get("/admin/customers/1")).andExpect(status().isOk()).andExpect(json(ANA_JSON));
    }

    @Test
    void shouldAnswer404ForUnknownCustomer() throws Exception {
        when(findCustomerUseCase.execute(any())).thenThrow(new NotFoundException("customer.notFound", "1234"));

        mockMvc.perform(get("/admin/customers/1234"))
                .andExpect(status().isNotFound())
                .andExpect(json("""
                        {"code": "customer.notFound", "message": "Cliente 1234 não encontrado.", "details": null}
                        """));
    }

    @Test
    void shouldUpdateCustomer() throws Exception {
        when(updateCustomerUseCase.execute(any())).thenReturn(ANA);

        mockMvc.perform(put("/admin/customers/1").contentType(MediaType.APPLICATION_JSON).content(ANA_REQUEST))
                .andExpect(status().isOk())
                .andExpect(json(ANA_JSON));
        verify(updateCustomerUseCase)
                .execute(new UpdateCustomerCommand(
                        1L, "Ana Souza", "529.982.247-25", "ana@email.com", "(11) 98765-4321"));
    }

    @Test
    void shouldValidateUpdatedCustomer() throws Exception {
        mockMvc.perform(put("/admin/customers/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(json("""
                        {"code": "request.invalidFields"}
                        """, JsonCompareMode.LENIENT));
        verifyNoInteractions(updateCustomerUseCase);
    }

    @Test
    void shouldAnswer409WhenUpdatingToAnotherCustomersDocument() throws Exception {
        when(updateCustomerUseCase.execute(any()))
                .thenThrow(new ConflictException("customer.documentAlreadyExists", "529.982.247-25"));

        mockMvc.perform(put("/admin/customers/1").contentType(MediaType.APPLICATION_JSON).content(ANA_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(json("""
                        {"code": "customer.documentAlreadyExists"}
                        """, JsonCompareMode.LENIENT));
    }

    @Test
    void shouldDeleteCustomer() throws Exception {
        mockMvc.perform(delete("/admin/customers/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(deleteCustomerUseCase).execute(new DeleteCustomerCommand(1L));
    }

    @Test
    void shouldAnswer409WhenCustomerIsInUse() throws Exception {
        doThrow(new ConflictException("customer.inUse", "1")).when(deleteCustomerUseCase).execute(any());

        mockMvc.perform(delete("/admin/customers/1"))
                .andExpect(status().isConflict())
                .andExpect(json("""
                        {"code": "customer.inUse",
                         "message": "O cliente 1 tem veículos ou ordens de serviço ligados e não pode ser excluído.",
                         "details": null}
                        """));
    }

    private static ResultMatcher json(String expected) {
        return json(expected, JsonCompareMode.STRICT);
    }

    private static ResultMatcher json(String expected, JsonCompareMode mode) {
        return content().json(expected, mode);
    }
}
