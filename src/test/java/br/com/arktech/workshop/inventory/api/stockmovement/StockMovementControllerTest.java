package br.com.arktech.workshop.inventory.api.stockmovement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.inventory.application.liststockmovements.ListStockMovementsQuery;
import br.com.arktech.workshop.inventory.application.liststockmovements.ListStockMovementsUseCase;
import br.com.arktech.workshop.inventory.application.registerstockmovement.RegisterStockMovementCommand;
import br.com.arktech.workshop.inventory.application.registerstockmovement.RegisterStockMovementUseCase;
import br.com.arktech.workshop.inventory.application.stockmovement.StockMovementResult;
import br.com.arktech.workshop.inventory.domain.model.StockMovementType;
import br.com.arktech.workshop.shared.api.error.ApiErrorTestConfig;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import java.time.Instant;
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

@WebMvcTest(controllers = StockMovementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiErrorTestConfig.class)
class StockMovementControllerTest {

    private static final StockMovementResult ENTRY = new StockMovementResult(
            5L, 1L, StockMovementType.IN, 10, "NF 1234", null, 7L, null, null, Instant.parse("2026-10-26T14:30:00Z"));
    private static final String ENTRY_JSON = """
            {"id": 5, "partId": 1, "type": "IN", "quantity": 10, "reason": "NF 1234", "serviceOrderId": null,
             "createdByUserId": 7, "stockAdjustmentRequestId": null, "requestedByUserId": null,
             "dateCreated": "2026-10-26T14:30:00Z"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterStockMovementUseCase registerStockMovementUseCase;

    @MockitoBean
    private ListStockMovementsUseCase listStockMovementsUseCase;

    @Test
    void shouldRegisterMovement() throws Exception {
        when(registerStockMovementUseCase.execute(any())).thenReturn(ENTRY);

        mockMvc.perform(post("/admin/parts/1/stockMovements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "IN", "quantity": 10, "reason": "NF 1234"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(json(ENTRY_JSON, JsonCompareMode.STRICT));
        verify(registerStockMovementUseCase)
                .execute(new RegisterStockMovementCommand(1L, StockMovementType.IN, 10, "NF 1234"));
    }

    @Test
    void shouldValidateMovement() throws Exception {
        mockMvc.perform(post("/admin/parts/1/stockMovements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 0, "reason": "%s"}
                                """.formatted("x".repeat(256))))
                .andExpect(status().isBadRequest())
                .andExpect(json("""
                        {
                          "code": "request.invalidFields",
                          "details": [
                            {"field": "quantity", "message": "Deve ser maior que zero."},
                            {"field": "reason", "message": "Deve ter no máximo 255 caracteres."},
                            {"field": "type", "message": "Campo obrigatório."}
                          ]
                        }
                        """, JsonCompareMode.LENIENT));
        verifyNoInteractions(registerStockMovementUseCase);
    }

    @Test
    void shouldAnswer422WhenStockIsNotEnough() throws Exception {
        when(registerStockMovementUseCase.execute(any()))
                .thenThrow(new BusinessRuleException("part.insufficientStock", "FILTRO-01", 2, 3));

        mockMvc.perform(post("/admin/parts/1/stockMovements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "ADJUSTMENT", "quantity": 3, "reason": "Avaria"}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(json("""
                        {"code": "part.insufficientStock",
                         "message": "Estoque insuficiente para FILTRO-01: há 2 em estoque e foram pedidos 3."}
                        """, JsonCompareMode.LENIENT));
    }

    @Test
    void shouldListHistory() throws Exception {
        when(listStockMovementsUseCase.execute(new ListStockMovementsQuery(1L, 0, 20)))
                .thenReturn(new Page<>(List.of(ENTRY), new Pagination(0, 20), 1));

        mockMvc.perform(get("/admin/parts/1/stockMovements"))
                .andExpect(status().isOk())
                .andExpect(json("""
                        {"content": [%s], "page": 0, "size": 20, "totalElements": 1, "totalPages": 1}
                        """.formatted(ENTRY_JSON), JsonCompareMode.STRICT));
    }

    private static ResultMatcher json(String expected, JsonCompareMode mode) {
        return content().json(expected, mode);
    }
}
