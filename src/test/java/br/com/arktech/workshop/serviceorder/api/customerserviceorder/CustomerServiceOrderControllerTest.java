package br.com.arktech.workshop.serviceorder.api.customerserviceorder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.serviceorder.application.trackserviceorder.QuoteItemResult;
import br.com.arktech.workshop.serviceorder.application.trackserviceorder.QuoteResult;
import br.com.arktech.workshop.serviceorder.application.trackserviceorder.StatusChangeResult;
import br.com.arktech.workshop.serviceorder.application.trackserviceorder.TrackServiceOrderQuery;
import br.com.arktech.workshop.serviceorder.application.trackserviceorder.TrackServiceOrderResult;
import br.com.arktech.workshop.serviceorder.application.trackserviceorder.TrackServiceOrderUseCase;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.shared.api.error.ApiErrorTestConfig;
import br.com.arktech.workshop.shared.application.registry.VehicleDetails;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = CustomerServiceOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiErrorTestConfig.class)
class CustomerServiceOrderControllerTest {

    private static final UUID PUBLIC_ID = UUID.fromString("3f1c2b8e-7d4a-4c9e-9b1f-2a6d8e5c4b3a");
    private static final String DOCUMENT = "529.982.247-25";
    private static final VehicleDetails GOL = new VehicleDetails("ABC1D23", "Volkswagen", "Gol", 2020, "Prata");
    private static final List<StatusChangeResult> HISTORY = List.of(
            new StatusChangeResult(ServiceOrderStatus.RECEIVED, null, Instant.parse("2026-10-27T14:30:00Z")),
            new StatusChangeResult(ServiceOrderStatus.IN_DIAGNOSIS, null, Instant.parse("2026-10-27T15:00:00Z")),
            new StatusChangeResult(
                    ServiceOrderStatus.AWAITING_APPROVAL, null, Instant.parse("2026-10-27T16:05:00Z")));
    private static final TrackServiceOrderResult AWAITING_APPROVAL = new TrackServiceOrderResult(
            PUBLIC_ID,
            "SO-2026-000123",
            ServiceOrderStatus.AWAITING_APPROVAL,
            "Barulho ao frear",
            GOL,
            HISTORY,
            new QuoteResult(
                    List.of(new QuoteItemResult(
                            "Troca de pastilhas", 1, new BigDecimal("150.00"), new BigDecimal("150.00"))),
                    List.of(new QuoteItemResult(
                            "Pastilha de freio", 2, new BigDecimal("45.90"), new BigDecimal("91.80"))),
                    new BigDecimal("150.00"),
                    new BigDecimal("91.80"),
                    new BigDecimal("241.80")),
            Instant.parse("2026-10-27T14:30:00Z"));
    private static final TrackServiceOrderResult RECEIVED = new TrackServiceOrderResult(
            PUBLIC_ID,
            "SO-2026-000123",
            ServiceOrderStatus.RECEIVED,
            "Barulho ao frear",
            GOL,
            HISTORY.subList(0, 1),
            null,
            Instant.parse("2026-10-27T14:30:00Z"));
    private static final String NOT_FOUND = """
            {"code": "serviceOrder.notFound",
             "message": "Ordem de serviço 3f1c2b8e-7d4a-4c9e-9b1f-2a6d8e5c4b3a não encontrada."}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrackServiceOrderUseCase trackServiceOrderUseCase;

    @Test
    void shouldShowTheServiceOrderWithTheDetailedQuoteAndNoInternalField() throws Exception {
        when(trackServiceOrderUseCase.execute(new TrackServiceOrderQuery(PUBLIC_ID, DOCUMENT)))
                .thenReturn(AWAITING_APPROVAL);

        mockMvc.perform(track().header("X-Customer-Document", DOCUMENT))
                .andExpect(status().isOk())
                .andExpect(json("""
                        {"publicId": "3f1c2b8e-7d4a-4c9e-9b1f-2a6d8e5c4b3a", "number": "SO-2026-000123",
                         "status": "AWAITING_APPROVAL", "problemDescription": "Barulho ao frear",
                         "vehicle": {"licensePlate": "ABC1D23", "brand": "Volkswagen", "model": "Gol", "year": 2020,
                                     "color": "Prata"},
                         "statusHistory": [
                           {"status": "RECEIVED", "description": null, "date": "2026-10-27T14:30:00Z"},
                           {"status": "IN_DIAGNOSIS", "description": null, "date": "2026-10-27T15:00:00Z"},
                           {"status": "AWAITING_APPROVAL", "description": null, "date": "2026-10-27T16:05:00Z"}],
                         "quote": {
                           "services": [{"name": "Troca de pastilhas", "quantity": 1, "unitPrice": 150.00,
                                         "subtotal": 150.00}],
                           "parts": [{"name": "Pastilha de freio", "quantity": 2, "unitPrice": 45.90,
                                      "subtotal": 91.80}],
                           "servicesTotal": 150.00, "partsTotal": 91.80, "totalPrice": 241.80},
                         "dateCreated": "2026-10-27T14:30:00Z"}
                        """));
    }

    @Test
    void shouldAnswerTheQuoteAsNullBeforeItIsSent() throws Exception {
        when(trackServiceOrderUseCase.execute(new TrackServiceOrderQuery(PUBLIC_ID, DOCUMENT)))
                .thenReturn(RECEIVED);

        mockMvc.perform(track().header("X-Customer-Document", DOCUMENT))
                .andExpect(status().isOk())
                .andExpect(json(
                        """
                        {"status": "RECEIVED", "quote": null}
                        """,
                        JsonCompareMode.LENIENT));
    }

    @Test
    void shouldRequireTheCustomerDocumentHeader() throws Exception {
        mockMvc.perform(track())
                .andExpect(status().isBadRequest())
                .andExpect(json("""
                        {"code": "request.invalidFields", "message": "Existem campos inválidos na requisição.",
                         "details": [{"field": "X-Customer-Document", "message": "Parâmetro obrigatório."}]}
                        """));
        verifyNoInteractions(trackServiceOrderUseCase);
    }

    @Test
    void shouldValidateTheCustomerDocument() throws Exception {
        mockMvc.perform(track().header("X-Customer-Document", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(json("""
                        {"code": "request.invalidFields", "message": "Existem campos inválidos na requisição.",
                         "details": [{"field": "X-Customer-Document", "message": "CPF ou CNPJ inválido."}]}
                        """));
        verifyNoInteractions(trackServiceOrderUseCase);
    }

    @Test
    void shouldAnswer400ForAMalformedPublicId() throws Exception {
        mockMvc.perform(get("/customer/serviceOrders/{publicId}", "123").header("X-Customer-Document", DOCUMENT))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {"code": "request.invalidFields",
                         "details": [{"field": "publicId", "message": "Valor inválido."}]}
                        """,
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(trackServiceOrderUseCase);
    }

    @Test
    void shouldAnswer404WhenTheServiceOrderIsNotFoundForTheDocument() throws Exception {
        when(trackServiceOrderUseCase.execute(any()))
                .thenThrow(new NotFoundException("serviceOrder.notFound", PUBLIC_ID.toString()));

        mockMvc.perform(track().header("X-Customer-Document", DOCUMENT))
                .andExpect(status().isNotFound())
                .andExpect(json(NOT_FOUND));
    }

    private static MockHttpServletRequestBuilder track() {
        return get("/customer/serviceOrders/{publicId}", PUBLIC_ID);
    }

    private static ResultMatcher json(String expected) {
        return json(expected, JsonCompareMode.STRICT);
    }

    private static ResultMatcher json(String expected, JsonCompareMode mode) {
        return content().json(expected, mode);
    }
}
