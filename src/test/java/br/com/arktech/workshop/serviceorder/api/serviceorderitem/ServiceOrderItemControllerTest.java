package br.com.arktech.workshop.serviceorder.api.serviceorderitem;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.serviceorder.application.addpartitem.AddPartItemCommand;
import br.com.arktech.workshop.serviceorder.application.addpartitem.AddPartItemResult;
import br.com.arktech.workshop.serviceorder.application.addpartitem.AddPartItemUseCase;
import br.com.arktech.workshop.serviceorder.application.addserviceitem.AddServiceItemCommand;
import br.com.arktech.workshop.serviceorder.application.addserviceitem.AddServiceItemResult;
import br.com.arktech.workshop.serviceorder.application.addserviceitem.AddServiceItemUseCase;
import br.com.arktech.workshop.serviceorder.application.removepartitem.RemovePartItemCommand;
import br.com.arktech.workshop.serviceorder.application.removepartitem.RemovePartItemUseCase;
import br.com.arktech.workshop.serviceorder.application.removeserviceitem.RemoveServiceItemCommand;
import br.com.arktech.workshop.serviceorder.application.removeserviceitem.RemoveServiceItemUseCase;
import br.com.arktech.workshop.shared.api.error.ApiErrorTestConfig;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;

import java.math.BigDecimal;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = ServiceOrderItemController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiErrorTestConfig.class)
class ServiceOrderItemControllerTest {

    private static final String ITEMS_LOCKED =
            "Os itens só podem ser alterados enquanto a ordem de serviço está recebida ou em diagnóstico.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AddServiceItemUseCase addServiceItemUseCase;

    @MockitoBean
    private RemoveServiceItemUseCase removeServiceItemUseCase;

    @MockitoBean
    private AddPartItemUseCase addPartItemUseCase;

    @MockitoBean
    private RemovePartItemUseCase removePartItemUseCase;

    @Test
    void shouldAddService() throws Exception {
        when(addServiceItemUseCase.execute(any()))
                .thenReturn(new AddServiceItemResult(
                        5L, 3L, 2, new BigDecimal("150.00"), new BigDecimal("300.00"), new BigDecimal("300.00")));

        mockMvc.perform(post1("/services", """
                        {"serviceOfferingId": 3, "quantity": 2}
                        """))
                .andExpect(status().isCreated())
                .andExpect(json("""
                        {"id": 5, "serviceOfferingId": 3, "quantity": 2, "unitPrice": 150.00, "subtotal": 300.00,
                         "serviceOrderTotalPrice": 300.00}
                        """));
        verify(addServiceItemUseCase).execute(new AddServiceItemCommand(1L, 3L, 2));
    }

    @Test
    void shouldAddPartWarningAboutTheStock() throws Exception {
        when(addPartItemUseCase.execute(any()))
                .thenReturn(new AddPartItemResult(
                        9L,
                        4L,
                        3,
                        new BigDecimal("35.90"),
                        new BigDecimal("107.70"),
                        new BigDecimal("257.70"),
                        2,
                        true));

        mockMvc.perform(post1("/parts", """
                        {"partId": 4, "quantity": 3}
                        """))
                .andExpect(status().isCreated())
                .andExpect(json("""
                        {"id": 9, "partId": 4, "quantity": 3, "unitPrice": 35.90, "subtotal": 107.70,
                         "serviceOrderTotalPrice": 257.70, "availableQuantity": 2, "insufficientStock": true}
                        """));
        verify(addPartItemUseCase).execute(new AddPartItemCommand(1L, 4L, 3));
    }

    @Test
    void shouldRequireTheItemAndTheQuantity() throws Exception {
        mockMvc.perform(post1("/services", "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {"code": "request.invalidFields",
                         "details": [{"field": "quantity", "message": "Campo obrigatório."},
                                     {"field": "serviceOfferingId", "message": "Campo obrigatório."}]}
                        """,
                        JsonCompareMode.LENIENT));
        mockMvc.perform(post1("/parts", "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {"code": "request.invalidFields",
                         "details": [{"field": "partId", "message": "Campo obrigatório."},
                                     {"field": "quantity", "message": "Campo obrigatório."}]}
                        """,
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(addServiceItemUseCase, addPartItemUseCase);
    }

    @Test
    void shouldLimitTheQuantityFrom1To999() throws Exception {
        mockMvc.perform(post1("/services", """
                        {"serviceOfferingId": 3, "quantity": 0}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(json("""
                        {"details": [{"field": "quantity", "message": "Deve ser maior que zero."}]}
                        """, JsonCompareMode.LENIENT));
        mockMvc.perform(post1("/parts", """
                        {"partId": 4, "quantity": 1000}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(json("""
                        {"details": [{"field": "quantity", "message": "Deve ser no máximo 999."}]}
                        """, JsonCompareMode.LENIENT));
        verifyNoInteractions(addServiceItemUseCase, addPartItemUseCase);
    }

    @Test
    void shouldRemoveServiceAndPart() throws Exception {
        mockMvc.perform(delete("/admin/serviceOrders/1/services/5")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/admin/serviceOrders/1/parts/9")).andExpect(status().isNoContent());

        verify(removeServiceItemUseCase).execute(new RemoveServiceItemCommand(1L, 5L));
        verify(removePartItemUseCase).execute(new RemovePartItemCommand(1L, 9L));
    }

    @Test
    void shouldAnswer422WhenTheItemsAreLocked() throws Exception {
        when(addServiceItemUseCase.execute(any())).thenThrow(new BusinessRuleException("serviceOrder.itemsLocked"));

        mockMvc.perform(post1("/services", """
                        {"serviceOfferingId": 3, "quantity": 1}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(json("""
                        {"code": "serviceOrder.itemsLocked", "message": "%s"}
                        """.formatted(ITEMS_LOCKED)));
    }

    @Test
    void shouldAnswer404WhenTheItemIsNotInTheServiceOrder() throws Exception {
        doThrow(new NotFoundException("serviceOrder.itemNotFound", "9"))
                .when(removePartItemUseCase)
                .execute(any());

        mockMvc.perform(delete("/admin/serviceOrders/1/parts/9"))
                .andExpect(status().isNotFound())
                .andExpect(json("""
                        {"code": "serviceOrder.itemNotFound", "message": "Item 9 não encontrado na ordem de serviço."}
                        """));
    }

    private static MockHttpServletRequestBuilder post1(String items, String body) {
        return post("/admin/serviceOrders/1" + items).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static ResultMatcher json(String expected) {
        return json(expected, JsonCompareMode.STRICT);
    }

    private static ResultMatcher json(String expected, JsonCompareMode mode) {
        return content().json(expected, mode);
    }
}
