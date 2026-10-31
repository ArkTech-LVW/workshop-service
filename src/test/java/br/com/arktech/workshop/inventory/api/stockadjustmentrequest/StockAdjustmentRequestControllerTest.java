package br.com.arktech.workshop.inventory.api.stockadjustmentrequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.inventory.application.approvestockadjustmentrequest.ApproveStockAdjustmentRequestCommand;
import br.com.arktech.workshop.inventory.application.approvestockadjustmentrequest.ApproveStockAdjustmentRequestUseCase;
import br.com.arktech.workshop.inventory.application.liststockadjustmentrequests.ListStockAdjustmentRequestsQuery;
import br.com.arktech.workshop.inventory.application.liststockadjustmentrequests.ListStockAdjustmentRequestsUseCase;
import br.com.arktech.workshop.inventory.application.rejectstockadjustmentrequest.RejectStockAdjustmentRequestCommand;
import br.com.arktech.workshop.inventory.application.rejectstockadjustmentrequest.RejectStockAdjustmentRequestUseCase;
import br.com.arktech.workshop.inventory.application.requeststockadjustment.RequestStockAdjustmentCommand;
import br.com.arktech.workshop.inventory.application.requeststockadjustment.RequestStockAdjustmentUseCase;
import br.com.arktech.workshop.inventory.application.stockadjustmentrequest.StockAdjustmentRequestResult;
import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequestStatus;
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

@WebMvcTest(controllers = StockAdjustmentRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiErrorTestConfig.class)
class StockAdjustmentRequestControllerTest {

    private static final StockAdjustmentRequestResult PENDING = new StockAdjustmentRequestResult(
            4L,
            1L,
            2,
            "Avaria",
            StockAdjustmentRequestStatus.PENDING,
            7L,
            null,
            null,
            null,
            Instant.parse("2026-10-30T14:30:00Z"));
    private static final String PENDING_JSON = """
            {"id": 4, "partId": 1, "quantity": 2, "reason": "Avaria", "status": "PENDING", "requestedByUserId": 7,
             "reviewedByUserId": null, "reviewNote": null, "reviewedAt": null, "dateCreated": "2026-10-30T14:30:00Z"}
            """;
    private static final StockAdjustmentRequestResult REJECTED = new StockAdjustmentRequestResult(
            4L,
            1L,
            2,
            "Avaria",
            StockAdjustmentRequestStatus.REJECTED,
            7L,
            8L,
            "Peça encontrada",
            Instant.parse("2026-10-31T10:00:00Z"),
            Instant.parse("2026-10-30T14:30:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequestStockAdjustmentUseCase requestStockAdjustmentUseCase;

    @MockitoBean
    private ListStockAdjustmentRequestsUseCase listStockAdjustmentRequestsUseCase;

    @MockitoBean
    private ApproveStockAdjustmentRequestUseCase approveStockAdjustmentRequestUseCase;

    @MockitoBean
    private RejectStockAdjustmentRequestUseCase rejectStockAdjustmentRequestUseCase;

    @Test
    void shouldOpenRequest() throws Exception {
        when(requestStockAdjustmentUseCase.execute(any())).thenReturn(PENDING);

        mockMvc.perform(post("/admin/parts/1/stockAdjustmentRequests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 2, "reason": "Avaria"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(json(PENDING_JSON, JsonCompareMode.STRICT));
        verify(requestStockAdjustmentUseCase).execute(new RequestStockAdjustmentCommand(1L, 2, "Avaria"));
    }

    @Test
    void shouldValidateRequest() throws Exception {
        mockMvc.perform(post("/admin/parts/1/stockAdjustmentRequests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 0, "reason": " "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(json("""
                        {
                          "code": "request.invalidFields",
                          "details": [
                            {"field": "quantity", "message": "Deve ser maior que zero."},
                            {"field": "reason", "message": "Não pode estar em branco."}
                          ]
                        }
                        """, JsonCompareMode.LENIENT));
        verifyNoInteractions(requestStockAdjustmentUseCase);
    }

    @Test
    void shouldListByStatus() throws Exception {
        when(listStockAdjustmentRequestsUseCase.execute(
                        new ListStockAdjustmentRequestsQuery(StockAdjustmentRequestStatus.PENDING, 0, 20)))
                .thenReturn(new Page<>(List.of(PENDING), new Pagination(0, 20), 1));

        mockMvc.perform(get("/admin/stockAdjustmentRequests").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(json("""
                        {"content": [%s], "page": 0, "size": 20, "totalElements": 1, "totalPages": 1}
                        """.formatted(PENDING_JSON), JsonCompareMode.STRICT));
    }

    @Test
    void shouldListWithoutStatusFilter() throws Exception {
        when(listStockAdjustmentRequestsUseCase.execute(new ListStockAdjustmentRequestsQuery(null, 0, 20)))
                .thenReturn(new Page<>(List.of(), new Pagination(0, 20), 0));

        mockMvc.perform(get("/admin/stockAdjustmentRequests")).andExpect(status().isOk());
        verify(listStockAdjustmentRequestsUseCase).execute(new ListStockAdjustmentRequestsQuery(null, 0, 20));
    }

    @Test
    void shouldApprove() throws Exception {
        when(approveStockAdjustmentRequestUseCase.execute(new ApproveStockAdjustmentRequestCommand(4L)))
                .thenReturn(PENDING);

        mockMvc.perform(post("/admin/stockAdjustmentRequests/4/approve")).andExpect(status().isOk());
        verify(approveStockAdjustmentRequestUseCase).execute(new ApproveStockAdjustmentRequestCommand(4L));
    }

    @Test
    void shouldAnswer422WhenRequesterReviewsOwnRequest() throws Exception {
        when(approveStockAdjustmentRequestUseCase.execute(any()))
                .thenThrow(new BusinessRuleException("stockAdjustmentRequest.selfReview"));

        mockMvc.perform(post("/admin/stockAdjustmentRequests/4/approve"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(json("""
                        {"code": "stockAdjustmentRequest.selfReview",
                         "message": "Quem pediu o ajuste de estoque não pode revisar o próprio pedido."}
                        """, JsonCompareMode.LENIENT));
    }

    @Test
    void shouldReject() throws Exception {
        when(rejectStockAdjustmentRequestUseCase.execute(any())).thenReturn(REJECTED);

        mockMvc.perform(post("/admin/stockAdjustmentRequests/4/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reviewNote": "Peça encontrada"}
                                """))
                .andExpect(status().isOk())
                .andExpect(json("""
                        {"status": "REJECTED", "reviewedByUserId": 8, "reviewNote": "Peça encontrada",
                         "reviewedAt": "2026-10-31T10:00:00Z"}
                        """, JsonCompareMode.LENIENT));
        verify(rejectStockAdjustmentRequestUseCase)
                .execute(new RejectStockAdjustmentRequestCommand(4L, "Peça encontrada"));
    }

    @Test
    void shouldRequireReviewNoteOnRejection() throws Exception {
        mockMvc.perform(post("/admin/stockAdjustmentRequests/4/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(json("""
                        {"code": "request.invalidFields",
                         "details": [{"field": "reviewNote", "message": "Não pode estar em branco."}]}
                        """, JsonCompareMode.LENIENT));
        verifyNoInteractions(rejectStockAdjustmentRequestUseCase);
    }

    private static ResultMatcher json(String expected, JsonCompareMode mode) {
        return content().json(expected, mode);
    }
}
