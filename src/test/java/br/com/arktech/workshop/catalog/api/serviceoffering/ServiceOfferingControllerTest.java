package br.com.arktech.workshop.catalog.api.serviceoffering;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.catalog.application.createserviceoffering.CreateServiceOfferingCommand;
import br.com.arktech.workshop.catalog.application.createserviceoffering.CreateServiceOfferingUseCase;
import br.com.arktech.workshop.catalog.application.findserviceoffering.FindServiceOfferingUseCase;
import br.com.arktech.workshop.catalog.application.listserviceofferings.ListServiceOfferingsQuery;
import br.com.arktech.workshop.catalog.application.listserviceofferings.ListServiceOfferingsUseCase;
import br.com.arktech.workshop.catalog.application.serviceoffering.ServiceOfferingResult;
import br.com.arktech.workshop.shared.api.error.ApiErrorTestConfig;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import java.math.BigDecimal;
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

@WebMvcTest(controllers = ServiceOfferingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiErrorTestConfig.class)
class ServiceOfferingControllerTest {

    private static final ServiceOfferingResult OIL_CHANGE = new ServiceOfferingResult(
            1L, "Troca de óleo", "Óleo e filtro", new BigDecimal("150.00"), 60, true);
    private static final String OIL_CHANGE_JSON = """
            {"id": 1, "name": "Troca de óleo", "description": "Óleo e filtro", "laborPrice": 150.00,
             "estimatedMinutes": 60, "active": true}
            """;
    private static final String OIL_CHANGE_REQUEST = """
            {"name": "Troca de óleo", "description": "Óleo e filtro", "laborPrice": 150.00, "estimatedMinutes": 60}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateServiceOfferingUseCase createServiceOfferingUseCase;

    @MockitoBean
    private ListServiceOfferingsUseCase listServiceOfferingsUseCase;

    @MockitoBean
    private FindServiceOfferingUseCase findServiceOfferingUseCase;

    @Test
    void shouldCreateServiceOffering() throws Exception {
        when(createServiceOfferingUseCase.execute(any())).thenReturn(OIL_CHANGE);

        mockMvc.perform(post("/admin/services").contentType(MediaType.APPLICATION_JSON).content(OIL_CHANGE_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(json(OIL_CHANGE_JSON));
        verify(createServiceOfferingUseCase)
                .execute(new CreateServiceOfferingCommand(
                        "Troca de óleo", "Óleo e filtro", new BigDecimal("150.00"), 60));
    }

    @Test
    void shouldCreateServiceOfferingWithoutDescription() throws Exception {
        when(createServiceOfferingUseCase.execute(any())).thenReturn(OIL_CHANGE);

        mockMvc.perform(post("/admin/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Troca de óleo", "laborPrice": 150, "estimatedMinutes": 60}
                                """))
                .andExpect(status().isCreated());
        verify(createServiceOfferingUseCase)
                .execute(new CreateServiceOfferingCommand("Troca de óleo", null, new BigDecimal("150"), 60));
    }

    @Test
    void shouldRequireNameLaborPriceAndEstimatedMinutes() throws Exception {
        mockMvc.perform(post("/admin/services").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {
                          "code": "request.invalidFields",
                          "details": [
                            {"field": "estimatedMinutes", "message": "Campo obrigatório."},
                            {"field": "laborPrice", "message": "Campo obrigatório."},
                            {"field": "name", "message": "Não pode estar em branco."}
                          ]
                        }
                        """,
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(createServiceOfferingUseCase);
    }

    @Test
    void shouldValidateNewServiceOffering() throws Exception {
        mockMvc.perform(post("/admin/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "description": "%s", "laborPrice": 150.555, "estimatedMinutes": 0}
                                """.formatted("a".repeat(151), "a".repeat(2001))))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {
                          "code": "request.invalidFields",
                          "details": [
                            {"field": "description", "message": "Deve ter no máximo 2000 caracteres."},
                            {"field": "estimatedMinutes", "message": "Deve ser maior que zero."},
                            {"field": "laborPrice",
                             "message": "Valor inválido. Informe um valor maior ou igual a zero, com até 10 dígitos inteiros e duas casas decimais."},
                            {"field": "name", "message": "Deve ter no máximo 150 caracteres."}
                          ]
                        }
                        """,
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(createServiceOfferingUseCase);
    }

    @Test
    void shouldRejectNonPositiveLaborPrice() throws Exception {
        mockMvc.perform(post("/admin/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Troca de óleo", "laborPrice": 0, "estimatedMinutes": 60}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {"code": "request.invalidFields",
                         "details": [{"field": "laborPrice", "message": "Deve ser maior que zero."}]}
                        """,
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(createServiceOfferingUseCase);
    }

    @Test
    void shouldRejectLaborPriceThatDoesNotFitTheColumn() throws Exception {
        mockMvc.perform(post("/admin/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Troca de óleo", "laborPrice": 12345678901, "estimatedMinutes": 60}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {"code": "request.invalidFields",
                         "details": [{"field": "laborPrice",
                                      "message": "Valor inválido. Informe um valor maior ou igual a zero, com até 10 dígitos inteiros e duas casas decimais."}]}
                        """,
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(createServiceOfferingUseCase);
    }

    @Test
    void shouldListWithDefaultPagination() throws Exception {
        when(listServiceOfferingsUseCase.execute(new ListServiceOfferingsQuery(null, 0, 20)))
                .thenReturn(new Page<>(List.of(OIL_CHANGE), new Pagination(0, 20), 1));

        mockMvc.perform(get("/admin/services"))
                .andExpect(status().isOk())
                .andExpect(json("""
                        {"content": [%s], "page": 0, "size": 20, "totalElements": 1, "totalPages": 1}
                        """.formatted(OIL_CHANGE_JSON)));
    }

    @Test
    void shouldFilterByName() throws Exception {
        when(listServiceOfferingsUseCase.execute(new ListServiceOfferingsQuery("óleo", 0, 20)))
                .thenReturn(new Page<>(List.of(OIL_CHANGE), new Pagination(0, 20), 1));

        mockMvc.perform(get("/admin/services").param("name", "óleo"))
                .andExpect(status().isOk())
                .andExpect(json("""
                        {"content": [%s], "totalElements": 1}
                        """.formatted(OIL_CHANGE_JSON), JsonCompareMode.LENIENT));
    }

    @Test
    void shouldValidatePagination() throws Exception {
        mockMvc.perform(get("/admin/services").param("page", "-1").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {
                          "code": "request.invalidFields",
                          "details": [
                            {"field": "page", "message": "Deve ser maior ou igual a zero."},
                            {"field": "size", "message": "Deve ser no máximo 100."}
                          ]
                        }
                        """,
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(listServiceOfferingsUseCase);
    }

    @Test
    void shouldRejectNameFilterLongerThanTheColumn() throws Exception {
        mockMvc.perform(get("/admin/services").param("name", "a".repeat(151)))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {"code": "request.invalidFields",
                         "details": [{"field": "name", "message": "Deve ter no máximo 150 caracteres."}]}
                        """,
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(listServiceOfferingsUseCase);
    }

    @Test
    void shouldFindServiceOffering() throws Exception {
        when(findServiceOfferingUseCase.execute(any())).thenReturn(OIL_CHANGE);

        mockMvc.perform(get("/admin/services/1")).andExpect(status().isOk()).andExpect(json(OIL_CHANGE_JSON));
    }

    @Test
    void shouldAnswer404ForUnknownServiceOffering() throws Exception {
        when(findServiceOfferingUseCase.execute(any()))
                .thenThrow(new NotFoundException("serviceOffering.notFound", "1234"));

        mockMvc.perform(get("/admin/services/1234"))
                .andExpect(status().isNotFound())
                .andExpect(json("""
                        {"code": "serviceOffering.notFound", "message": "Serviço 1234 não encontrado.",
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
