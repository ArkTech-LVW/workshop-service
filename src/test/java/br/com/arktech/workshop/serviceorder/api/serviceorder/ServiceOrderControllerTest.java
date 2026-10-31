package br.com.arktech.workshop.serviceorder.api.serviceorder;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.serviceorder.application.approveoriginalonly.ApproveOriginalOnlyCommand;
import br.com.arktech.workshop.serviceorder.application.approveoriginalonly.ApproveOriginalOnlyUseCase;
import br.com.arktech.workshop.serviceorder.application.approvequote.ApproveQuoteCommand;
import br.com.arktech.workshop.serviceorder.application.approvequote.ApproveQuoteUseCase;
import br.com.arktech.workshop.serviceorder.application.cancelserviceorder.CancelServiceOrderCommand;
import br.com.arktech.workshop.serviceorder.application.cancelserviceorder.CancelServiceOrderUseCase;
import br.com.arktech.workshop.serviceorder.application.completeserviceitem.CompleteServiceItemCommand;
import br.com.arktech.workshop.serviceorder.application.completeserviceitem.CompleteServiceItemUseCase;
import br.com.arktech.workshop.serviceorder.application.completeserviceorder.CompleteServiceOrderCommand;
import br.com.arktech.workshop.serviceorder.application.completeserviceorder.CompleteServiceOrderUseCase;
import br.com.arktech.workshop.serviceorder.application.deliverserviceorder.DeliverServiceOrderCommand;
import br.com.arktech.workshop.serviceorder.application.deliverserviceorder.DeliverServiceOrderUseCase;
import br.com.arktech.workshop.serviceorder.application.findserviceorder.FindServiceOrderQuery;
import br.com.arktech.workshop.serviceorder.application.findserviceorder.FindServiceOrderUseCase;
import br.com.arktech.workshop.serviceorder.application.listserviceorders.ListServiceOrdersQuery;
import br.com.arktech.workshop.serviceorder.application.listserviceorders.ListServiceOrdersUseCase;
import br.com.arktech.workshop.serviceorder.application.openserviceorder.OpenServiceOrderCommand;
import br.com.arktech.workshop.serviceorder.application.openserviceorder.OpenServiceOrderUseCase;
import br.com.arktech.workshop.serviceorder.application.serviceorder.PartItemResult;
import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceItemResult;
import br.com.arktech.workshop.serviceorder.application.rejectquote.RejectQuoteCommand;
import br.com.arktech.workshop.serviceorder.application.rejectquote.RejectQuoteUseCase;
import br.com.arktech.workshop.serviceorder.application.reopenquote.ReopenQuoteCommand;
import br.com.arktech.workshop.serviceorder.application.reopenquote.ReopenQuoteUseCase;
import br.com.arktech.workshop.serviceorder.application.sendquote.SendQuoteCommand;
import br.com.arktech.workshop.serviceorder.application.sendquote.SendQuoteUseCase;
import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceOrderResult;
import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceOrderSummaryResult;
import br.com.arktech.workshop.serviceorder.application.serviceorder.StatusChangeResult;
import br.com.arktech.workshop.serviceorder.application.startdiagnosis.StartDiagnosisCommand;
import br.com.arktech.workshop.serviceorder.application.startdiagnosis.StartDiagnosisUseCase;
import br.com.arktech.workshop.serviceorder.application.startexecution.StartExecutionCommand;
import br.com.arktech.workshop.serviceorder.application.startexecution.StartExecutionUseCase;
import br.com.arktech.workshop.serviceorder.domain.model.InvalidStatusTransitionException;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.shared.api.error.ApiErrorTestConfig;
import br.com.arktech.workshop.shared.application.registry.VehicleDetails;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

@WebMvcTest(controllers = ServiceOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiErrorTestConfig.class)
class ServiceOrderControllerTest {

    private static final UUID PUBLIC_ID = UUID.fromString("3f1c2b8e-7d4a-4c9e-9b1f-2a6d8e5c4b3a");
    private static final StatusChangeResult RECEIVED = new StatusChangeResult(
            null, ServiceOrderStatus.RECEIVED, 7L, null, Instant.parse("2026-10-27T14:30:00Z"));
    private static final StatusChangeResult DIAGNOSIS_STARTED = new StatusChangeResult(
            ServiceOrderStatus.RECEIVED,
            ServiceOrderStatus.IN_DIAGNOSIS,
            5L,
            "Pastilhas gastas",
            Instant.parse("2026-10-27T15:00:00Z"));
    private static final ServiceOrderResult OPENED = new ServiceOrderResult(
            1L,
            PUBLIC_ID,
            "SO-2026-000123",
            10L,
            20L,
            null,
            "Barulho ao frear",
            ServiceOrderStatus.RECEIVED,
            List.of(),
            List.of(),
            new BigDecimal("0.00"),
            new BigDecimal("0.00"),
            new BigDecimal("0.00"),
            new BigDecimal("0.00"),
            new BigDecimal("0.00"),
            null,
            List.of(RECEIVED),
            Instant.parse("2026-10-27T14:30:00Z"));
    private static final ServiceOrderResult IN_DIAGNOSIS = new ServiceOrderResult(
            1L,
            PUBLIC_ID,
            "SO-2026-000123",
            10L,
            20L,
            5L,
            "Barulho ao frear",
            ServiceOrderStatus.IN_DIAGNOSIS,
            List.of(new ServiceItemResult(1L, 30L, 2, new BigDecimal("150.00"), new BigDecimal("300.00"), null, null)),
            List.of(new PartItemResult(2L, 40L, 3, new BigDecimal("35.90"), new BigDecimal("107.70"), null)),
            new BigDecimal("300.00"),
            new BigDecimal("107.70"),
            new BigDecimal("407.70"),
            new BigDecimal("407.70"),
            new BigDecimal("0.00"),
            null,
            List.of(RECEIVED, DIAGNOSIS_STARTED),
            Instant.parse("2026-10-27T14:30:00Z"));
    private static final ServiceOrderResult AWAITING_APPROVAL = new ServiceOrderResult(
            1L,
            PUBLIC_ID,
            "SO-2026-000123",
            10L,
            20L,
            5L,
            "Barulho ao frear",
            ServiceOrderStatus.AWAITING_APPROVAL,
            List.of(new ServiceItemResult(1L, 30L, 2, new BigDecimal("150.00"), new BigDecimal("300.00"), null, null)),
            List.of(new PartItemResult(2L, 40L, 3, new BigDecimal("35.90"), new BigDecimal("107.70"), null)),
            new BigDecimal("300.00"),
            new BigDecimal("107.70"),
            new BigDecimal("407.70"),
            new BigDecimal("407.70"),
            new BigDecimal("0.00"),
            null,
            List.of(),
            Instant.parse("2026-10-27T14:30:00Z"));
    private static final ServiceOrderResult COMPLETED = new ServiceOrderResult(
            1L,
            PUBLIC_ID,
            "SO-2026-000123",
            10L,
            20L,
            5L,
            "Barulho ao frear",
            ServiceOrderStatus.COMPLETED,
            List.of(new ServiceItemResult(
                    1L,
                    30L,
                    2,
                    new BigDecimal("150.00"),
                    new BigDecimal("300.00"),
                    Instant.parse("2026-10-28T10:15:00Z"),
                    Instant.parse("2026-10-28T16:45:00Z"))),
            List.of(),
            new BigDecimal("300.00"),
            new BigDecimal("0.00"),
            new BigDecimal("300.00"),
            new BigDecimal("300.00"),
            new BigDecimal("0.00"),
            null,
            List.of(),
            Instant.parse("2026-10-27T14:30:00Z"));
    private static final ServiceOrderResult CANCELED = new ServiceOrderResult(
            1L,
            PUBLIC_ID,
            "SO-2026-000123",
            10L,
            20L,
            null,
            "Barulho ao frear",
            ServiceOrderStatus.CANCELED,
            List.of(),
            List.of(),
            new BigDecimal("0.00"),
            new BigDecimal("0.00"),
            new BigDecimal("0.00"),
            new BigDecimal("0.00"),
            new BigDecimal("0.00"),
            null,
            List.of(),
            Instant.parse("2026-10-27T14:30:00Z"));
    private static final String CUSTOMER_NOT_REGISTERED = "Nenhum cliente cadastrado com o CPF/CNPJ 529.982.247-25. "
            + "Cadastre o cliente antes de abrir a ordem de serviço.";
    private static final String OPEN_REQUEST = """
            {"customerDocument": "529.982.247-25",
             "vehicle": {"licensePlate": "abc-1d23", "brand": "Volkswagen", "model": "Gol", "year": 2020,
                         "color": "Prata"},
             "problemDescription": "Barulho ao frear"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpenServiceOrderUseCase openServiceOrderUseCase;

    @MockitoBean
    private ListServiceOrdersUseCase listServiceOrdersUseCase;

    @MockitoBean
    private FindServiceOrderUseCase findServiceOrderUseCase;

    @MockitoBean
    private StartDiagnosisUseCase startDiagnosisUseCase;

    @MockitoBean
    private SendQuoteUseCase sendQuoteUseCase;

    @MockitoBean
    private ApproveQuoteUseCase approveQuoteUseCase;

    @MockitoBean
    private ApproveOriginalOnlyUseCase approveOriginalOnlyUseCase;

    @MockitoBean
    private RejectQuoteUseCase rejectQuoteUseCase;

    @MockitoBean
    private ReopenQuoteUseCase reopenQuoteUseCase;

    @MockitoBean
    private StartExecutionUseCase startExecutionUseCase;

    @MockitoBean
    private CompleteServiceItemUseCase completeServiceItemUseCase;

    @MockitoBean
    private CompleteServiceOrderUseCase completeServiceOrderUseCase;

    @MockitoBean
    private DeliverServiceOrderUseCase deliverServiceOrderUseCase;

    @MockitoBean
    private CancelServiceOrderUseCase cancelServiceOrderUseCase;

    @Test
    void shouldOpenServiceOrder() throws Exception {
        when(openServiceOrderUseCase.execute(any())).thenReturn(OPENED);

        mockMvc.perform(open(OPEN_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(json("""
                        {"id": 1, "publicId": "3f1c2b8e-7d4a-4c9e-9b1f-2a6d8e5c4b3a", "number": "SO-2026-000123",
                         "customerId": 10, "vehicleId": 20, "mechanicId": null,
                         "problemDescription": "Barulho ao frear", "status": "RECEIVED", "services": [], "parts": [],
                         "servicesTotal": 0.00, "partsTotal": 0.00, "totalPrice": 0.00,
                         "approvedTotal": 0.00, "pendingTotal": 0.00, "canProceedWithoutChanges": null,
                         "statusHistory": [{"fromStatus": null, "toStatus": "RECEIVED", "changedByUserId": 7,
                                            "description": null, "changedAt": "2026-10-27T14:30:00Z"}],
                         "dateCreated": "2026-10-27T14:30:00Z"}
                        """));
        verify(openServiceOrderUseCase)
                .execute(new OpenServiceOrderCommand(
                        "529.982.247-25",
                        new VehicleDetails("abc-1d23", "Volkswagen", "Gol", 2020, "Prata"),
                        "Barulho ao frear"));
    }

    @Test
    void shouldRequireDocumentVehicleAndProblem() throws Exception {
        mockMvc.perform(open("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {
                          "code": "request.invalidFields",
                          "details": [
                            {"field": "customerDocument", "message": "Não pode estar em branco."},
                            {"field": "problemDescription", "message": "Não pode estar em branco."},
                            {"field": "vehicle", "message": "Campo obrigatório."}
                          ]
                        }
                        """,
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(openServiceOrderUseCase);
    }

    @Test
    void shouldValidateDocumentAndEachVehicleField() throws Exception {
        mockMvc.perform(open("""
                        {"customerDocument": "123.456.789-00",
                         "vehicle": {"licensePlate": "AB-12345", "brand": "%s", "model": "", "year": 1980},
                         "problemDescription": "%s"}
                        """.formatted("a".repeat(51), "a".repeat(2001))))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {
                          "code": "request.invalidFields",
                          "details": [
                            {"field": "customerDocument", "message": "CPF ou CNPJ inválido."},
                            {"field": "problemDescription", "message": "Deve ter no máximo 2000 caracteres."},
                            {"field": "vehicle.brand", "message": "Deve ter no máximo 50 caracteres."},
                            {"field": "vehicle.color", "message": "Não pode estar em branco."},
                            {"field": "vehicle.licensePlate", "message": "Placa inválida."},
                            {"field": "vehicle.model", "message": "Não pode estar em branco."},
                            {"field": "vehicle.year",
                             "message": "Ano inválido. Informe um ano entre 1981 e o ano que vem."}
                          ]
                        }
                        """,
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(openServiceOrderUseCase);
    }

    @Test
    void shouldAnswer404WhenTheCustomerIsNotRegistered() throws Exception {
        when(openServiceOrderUseCase.execute(any()))
                .thenThrow(new NotFoundException("serviceOrder.customerNotRegistered", "529.982.247-25"));

        mockMvc.perform(open(OPEN_REQUEST))
                .andExpect(status().isNotFound())
                .andExpect(json("""
                        {"code": "serviceOrder.customerNotRegistered", "message": "%s"}
                        """.formatted(CUSTOMER_NOT_REGISTERED)));
    }

    @Test
    void shouldAnswer409WhenThePlateBelongsToAnotherCustomer() throws Exception {
        when(openServiceOrderUseCase.execute(any()))
                .thenThrow(new ConflictException("vehicle.licensePlateOfAnotherCustomer", "ABC1D23"));

        mockMvc.perform(open(OPEN_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(json("""
                        {"code": "vehicle.licensePlateOfAnotherCustomer",
                         "message": "A placa ABC1D23 já está cadastrada para outro cliente."}
                        """));
    }

    @Test
    void shouldStartTheDiagnosisAndDetailTheQuote() throws Exception {
        when(startDiagnosisUseCase.execute(any())).thenReturn(IN_DIAGNOSIS);

        mockMvc.perform(post("/admin/serviceOrders/{id}/startDiagnosis", 1L))
                .andExpect(status().isOk())
                .andExpect(json("""
                        {"id": 1, "publicId": "3f1c2b8e-7d4a-4c9e-9b1f-2a6d8e5c4b3a", "number": "SO-2026-000123",
                         "customerId": 10, "vehicleId": 20, "mechanicId": 5,
                         "problemDescription": "Barulho ao frear", "status": "IN_DIAGNOSIS",
                         "services": [{"id": 1, "serviceOfferingId": 30, "quantity": 2, "unitPrice": 150.00,
                                       "subtotal": 300.00, "approvedAt": null, "completedAt": null}],
                         "parts": [{"id": 2, "partId": 40, "quantity": 3, "unitPrice": 35.90, "subtotal": 107.70,
                                    "approvedAt": null}],
                         "servicesTotal": 300.00, "partsTotal": 107.70, "totalPrice": 407.70,
                         "approvedTotal": 407.70, "pendingTotal": 0.00, "canProceedWithoutChanges": null,
                         "statusHistory": [
                           {"fromStatus": null, "toStatus": "RECEIVED", "changedByUserId": 7, "description": null,
                            "changedAt": "2026-10-27T14:30:00Z"},
                           {"fromStatus": "RECEIVED", "toStatus": "IN_DIAGNOSIS", "changedByUserId": 5,
                            "description": "Pastilhas gastas", "changedAt": "2026-10-27T15:00:00Z"}],
                         "dateCreated": "2026-10-27T14:30:00Z"}
                        """));
        verify(startDiagnosisUseCase).execute(new StartDiagnosisCommand(1L));
    }

    @Test
    void shouldAnswer422WhenTheServiceOrderIsNotReceived() throws Exception {
        when(startDiagnosisUseCase.execute(any()))
                .thenThrow(new InvalidStatusTransitionException(
                        ServiceOrderStatus.IN_DIAGNOSIS, ServiceOrderStatus.IN_DIAGNOSIS));

        mockMvc.perform(post("/admin/serviceOrders/{id}/startDiagnosis", 1L))
                .andExpect(status().isUnprocessableContent())
                .andExpect(json(
                        """
                        {"code": "serviceOrder.invalidStatusTransition",
                         "message": "Esta ação não é permitida no status atual da ordem de serviço."}
                        """,
                        JsonCompareMode.LENIENT));
    }

    @Test
    void shouldSendTheQuote() throws Exception {
        when(sendQuoteUseCase.execute(any())).thenReturn(AWAITING_APPROVAL);

        mockMvc.perform(post("/admin/serviceOrders/{id}/sendQuote", 1L))
                .andExpect(status().isOk())
                .andExpect(json(
                        """
                        {"id": 1, "status": "AWAITING_APPROVAL", "totalPrice": 407.70}
                        """,
                        JsonCompareMode.LENIENT));
        verify(sendQuoteUseCase).execute(new SendQuoteCommand(1L, null));
    }

    @Test
    void shouldSendTheSupplementaryQuoteSayingWhetherTheServiceCanGoOnWithoutIt() throws Exception {
        when(sendQuoteUseCase.execute(any())).thenReturn(AWAITING_APPROVAL);

        mockMvc.perform(post("/admin/serviceOrders/{id}/sendQuote", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"canProceedWithoutChanges": false}
                                """))
                .andExpect(status().isOk());
        verify(sendQuoteUseCase).execute(new SendQuoteCommand(1L, false));
    }

    @Test
    void shouldApproveOnlyTheOriginalQuote() throws Exception {
        when(approveOriginalOnlyUseCase.execute(any())).thenReturn(AWAITING_APPROVAL);

        mockMvc.perform(post("/admin/serviceOrders/{id}/approveOriginalOnly", 1L))
                .andExpect(status().isOk())
                .andExpect(json(
                        """
                        {"approvedTotal": 407.70, "pendingTotal": 0.00, "canProceedWithoutChanges": null}
                        """,
                        JsonCompareMode.LENIENT));
        verify(approveOriginalOnlyUseCase).execute(new ApproveOriginalOnlyCommand(1L));
    }

    @Test
    void shouldAnswer422WhenTheWorkshopDidNotAllowApprovingOnlyTheOriginalQuote() throws Exception {
        when(approveOriginalOnlyUseCase.execute(any()))
                .thenThrow(new BusinessRuleException("serviceOrder.originalOnlyNotAllowed"));

        mockMvc.perform(post("/admin/serviceOrders/{id}/approveOriginalOnly", 1L))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("serviceOrder.originalOnlyNotAllowed"));
    }

    @Test
    void shouldAnswer422WhenTheQuoteHasNoItems() throws Exception {
        when(sendQuoteUseCase.execute(any())).thenThrow(new BusinessRuleException("serviceOrder.quoteWithoutItems"));

        mockMvc.perform(post("/admin/serviceOrders/{id}/sendQuote", 1L))
                .andExpect(status().isUnprocessableContent())
                .andExpect(json("""
                        {"code": "serviceOrder.quoteWithoutItems",
                         "message": "O orçamento precisa ter pelo menos um serviço ou peça."}
                        """));
    }

    @Test
    void shouldAnswer422WhenTheServiceOrderIsNotInDiagnosis() throws Exception {
        when(sendQuoteUseCase.execute(any()))
                .thenThrow(new InvalidStatusTransitionException(
                        ServiceOrderStatus.RECEIVED, ServiceOrderStatus.AWAITING_APPROVAL));

        mockMvc.perform(post("/admin/serviceOrders/{id}/sendQuote", 1L))
                .andExpect(status().isUnprocessableContent())
                .andExpect(json("""
                        {"code": "serviceOrder.invalidStatusTransition",
                         "message": "Esta ação não é permitida no status atual da ordem de serviço."}
                        """));
    }

    @Test
    void shouldAnswer404WhenSendingTheQuoteOfAnUnknownServiceOrder() throws Exception {
        when(sendQuoteUseCase.execute(any())).thenThrow(new NotFoundException("serviceOrder.notFound", "1"));

        mockMvc.perform(post("/admin/serviceOrders/{id}/sendQuote", 1L))
                .andExpect(status().isNotFound())
                .andExpect(json("""
                        {"code": "serviceOrder.notFound", "message": "Ordem de serviço 1 não encontrada."}
                        """));
    }

    @Test
    void shouldApproveTheQuote() throws Exception {
        when(approveQuoteUseCase.execute(any())).thenReturn(AWAITING_APPROVAL);

        mockMvc.perform(post("/admin/serviceOrders/{id}/approve", 1L)).andExpect(status().isOk());

        verify(approveQuoteUseCase).execute(new ApproveQuoteCommand(1L));
    }

    @Test
    void shouldAnswer422NamingThePartWithoutEnoughStock() throws Exception {
        when(approveQuoteUseCase.execute(any()))
                .thenThrow(new BusinessRuleException("part.insufficientStock", "FILTRO-01", 1, 3));

        mockMvc.perform(post("/admin/serviceOrders/{id}/approve", 1L))
                .andExpect(status().isUnprocessableContent())
                .andExpect(json("""
                        {"code": "part.insufficientStock",
                         "message": "Estoque insuficiente para FILTRO-01: o disponível é 1 e foram pedidos 3."}
                        """));
    }

    @Test
    void shouldRejectTheQuoteWithTheReason() throws Exception {
        when(rejectQuoteUseCase.execute(any())).thenReturn(AWAITING_APPROVAL);

        mockMvc.perform(post("/admin/serviceOrders/{id}/reject", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Cliente achou o valor alto"}
                                """))
                .andExpect(status().isOk());

        verify(rejectQuoteUseCase).execute(new RejectQuoteCommand(1L, "Cliente achou o valor alto"));
    }

    @Test
    void shouldRequireTheRejectionReason() throws Exception {
        mockMvc.perform(post("/admin/serviceOrders/{id}/reject", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": " "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {"code": "request.invalidFields",
                         "details": [{"field": "reason", "message": "Não pode estar em branco."}]}
                        """,
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(rejectQuoteUseCase);
    }

    @Test
    void shouldReopenTheQuoteWithOrWithoutDescription() throws Exception {
        when(reopenQuoteUseCase.execute(any())).thenReturn(IN_DIAGNOSIS);

        mockMvc.perform(post("/admin/serviceOrders/{id}/reopenQuote", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "Cliente pediu peça mais barata"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/admin/serviceOrders/{id}/reopenQuote", 1L)).andExpect(status().isOk());

        verify(reopenQuoteUseCase).execute(new ReopenQuoteCommand(1L, "Cliente pediu peça mais barata"));
        verify(reopenQuoteUseCase).execute(new ReopenQuoteCommand(1L, null));
    }

    @Test
    void shouldStartTheExecution() throws Exception {
        when(startExecutionUseCase.execute(any())).thenReturn(IN_DIAGNOSIS);

        mockMvc.perform(post("/admin/serviceOrders/{id}/startExecution", 1L)).andExpect(status().isOk());

        verify(startExecutionUseCase).execute(new StartExecutionCommand(1L));
    }

    @Test
    void shouldCompleteAServiceShowingWhenItWasCompleted() throws Exception {
        when(completeServiceItemUseCase.execute(any())).thenReturn(COMPLETED);

        mockMvc.perform(post("/admin/serviceOrders/{id}/services/{itemId}/complete", 1L, 3L))
                .andExpect(status().isOk())
                .andExpect(json(
                        """
                        {"status": "COMPLETED",
                         "services": [{"id": 1, "serviceOfferingId": 30, "quantity": 2, "unitPrice": 150.00,
                                       "subtotal": 300.00, "completedAt": "2026-10-28T16:45:00Z"}]}
                        """,
                        JsonCompareMode.LENIENT));
        verify(completeServiceItemUseCase).execute(new CompleteServiceItemCommand(1L, 3L));
    }

    @Test
    void shouldAnswer422WhenTheServiceWasAlreadyCompleted() throws Exception {
        when(completeServiceItemUseCase.execute(any()))
                .thenThrow(new BusinessRuleException("serviceOrder.serviceItemAlreadyCompleted"));

        mockMvc.perform(post("/admin/serviceOrders/{id}/services/{itemId}/complete", 1L, 3L))
                .andExpect(status().isUnprocessableContent())
                .andExpect(json("""
                        {"code": "serviceOrder.serviceItemAlreadyCompleted",
                         "message": "Este serviço já foi concluído."}
                        """));
    }

    @Test
    void shouldAnswer422WhenTheServiceOrderIsNotInExecution() throws Exception {
        when(completeServiceItemUseCase.execute(any()))
                .thenThrow(new BusinessRuleException("serviceOrder.serviceItemCompletionNotAllowed"));

        mockMvc.perform(post("/admin/serviceOrders/{id}/services/{itemId}/complete", 1L, 3L))
                .andExpect(status().isUnprocessableContent())
                .andExpect(json("""
                        {"code": "serviceOrder.serviceItemCompletionNotAllowed",
                         "message": "Os serviços só podem ser concluídos enquanto a ordem de serviço está em execução."}
                        """));
    }

    @Test
    void shouldAnswer404WhenTheServiceIsNotInTheServiceOrder() throws Exception {
        when(completeServiceItemUseCase.execute(any()))
                .thenThrow(new NotFoundException("serviceOrder.itemNotFound", "3"));

        mockMvc.perform(post("/admin/serviceOrders/{id}/services/{itemId}/complete", 1L, 3L))
                .andExpect(status().isNotFound())
                .andExpect(json("""
                        {"code": "serviceOrder.itemNotFound", "message": "Item 3 não encontrado na ordem de serviço."}
                        """));
    }

    @Test
    void shouldCompleteTheServiceOrder() throws Exception {
        when(completeServiceOrderUseCase.execute(any())).thenReturn(COMPLETED);

        mockMvc.perform(post("/admin/serviceOrders/{id}/complete", 1L))
                .andExpect(status().isOk())
                .andExpect(json("""
                        {"status": "COMPLETED"}
                        """, JsonCompareMode.LENIENT));
        verify(completeServiceOrderUseCase).execute(new CompleteServiceOrderCommand(1L));
    }

    @Test
    void shouldAnswer422WhenCompletingWithPendingServices() throws Exception {
        when(completeServiceOrderUseCase.execute(any()))
                .thenThrow(new BusinessRuleException("serviceOrder.pendingServiceItems"));

        mockMvc.perform(post("/admin/serviceOrders/{id}/complete", 1L))
                .andExpect(status().isUnprocessableContent())
                .andExpect(json("""
                        {"code": "serviceOrder.pendingServiceItems",
                         "message": "Conclua todos os serviços para finalizar a ordem de serviço."}
                        """));
    }

    @Test
    void shouldDeliverTheServiceOrder() throws Exception {
        when(deliverServiceOrderUseCase.execute(any())).thenReturn(COMPLETED);

        mockMvc.perform(post("/admin/serviceOrders/{id}/deliver", 1L)).andExpect(status().isOk());

        verify(deliverServiceOrderUseCase).execute(new DeliverServiceOrderCommand(1L));
    }

    @Test
    void shouldCancelServiceOrder() throws Exception {
        when(cancelServiceOrderUseCase.execute(any())).thenReturn(CANCELED);

        mockMvc.perform(cancel("""
                        {"reason": "Cliente desistiu do serviço"}
                        """))
                .andExpect(status().isOk())
                .andExpect(json("""
                        {"id": 1, "publicId": "3f1c2b8e-7d4a-4c9e-9b1f-2a6d8e5c4b3a", "number": "SO-2026-000123",
                         "customerId": 10, "vehicleId": 20, "mechanicId": null,
                         "problemDescription": "Barulho ao frear", "status": "CANCELED", "services": [], "parts": [],
                         "servicesTotal": 0.00, "partsTotal": 0.00, "totalPrice": 0.00, "approvedTotal": 0.00,
                         "pendingTotal": 0.00, "canProceedWithoutChanges": null, "statusHistory": [],
                         "dateCreated": "2026-10-27T14:30:00Z"}
                        """));
        verify(cancelServiceOrderUseCase).execute(new CancelServiceOrderCommand(1L, "Cliente desistiu do serviço"));
    }

    @Test
    void shouldRequireAReasonToCancel() throws Exception {
        mockMvc.perform(cancel("""
                        {"reason": "  "}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {"code": "request.invalidFields",
                         "details": [{"field": "reason", "message": "Não pode estar em branco."}]}
                        """,
                        JsonCompareMode.LENIENT));
        mockMvc.perform(cancel("""
                        {"reason": "%s"}
                        """.formatted("a".repeat(2001))))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {"code": "request.invalidFields",
                         "details": [{"field": "reason", "message": "Deve ter no máximo 2000 caracteres."}]}
                        """,
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(cancelServiceOrderUseCase);
    }

    @Test
    void shouldAnswer422WhenTheStatusDoesNotAllowCancellation() throws Exception {
        when(cancelServiceOrderUseCase.execute(any()))
                .thenThrow(new InvalidStatusTransitionException(
                        ServiceOrderStatus.QUOTE_APPROVED, ServiceOrderStatus.CANCELED));

        mockMvc.perform(cancel("""
                        {"reason": "Cliente desistiu do serviço"}
                        """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(json("""
                        {"code": "serviceOrder.invalidStatusTransition",
                         "message": "Esta ação não é permitida no status atual da ordem de serviço."}
                        """));
    }

    @Test
    void shouldAnswer404WhenTheServiceOrderDoesNotExist() throws Exception {
        when(cancelServiceOrderUseCase.execute(any())).thenThrow(new NotFoundException("serviceOrder.notFound", "1"));

        mockMvc.perform(cancel("""
                        {"reason": "Cliente desistiu do serviço"}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(json("""
                        {"code": "serviceOrder.notFound", "message": "Ordem de serviço 1 não encontrada."}
                        """));
    }

    @Test
    void shouldListServiceOrdersWithTheGivenFilters() throws Exception {
        ServiceOrderSummaryResult summary = new ServiceOrderSummaryResult(
                1L,
                PUBLIC_ID,
                "SO-2026-000123",
                10L,
                20L,
                5L,
                ServiceOrderStatus.IN_DIAGNOSIS,
                new BigDecimal("407.70"),
                Instant.parse("2026-10-27T14:30:00Z"));
        ListServiceOrdersQuery query = new ListServiceOrdersQuery(
                ServiceOrderStatus.IN_DIAGNOSIS,
                "529.982.247-25",
                "abc-1d23",
                Instant.parse("2026-10-01T03:00:00Z"),
                Instant.parse("2026-10-31T23:59:59Z"),
                1,
                5);
        when(listServiceOrdersUseCase.execute(query)).thenReturn(new Page<>(List.of(summary), new Pagination(1, 5), 6));

        mockMvc.perform(get("/admin/serviceOrders")
                        .param("status", "IN_DIAGNOSIS")
                        .param("customerDocument", "529.982.247-25")
                        .param("licensePlate", "abc-1d23")
                        .param("openedFrom", "2026-10-01T00:00:00-03:00")
                        .param("openedTo", "2026-10-31T23:59:59Z")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(json("""
                        {"content": [{"id": 1, "publicId": "3f1c2b8e-7d4a-4c9e-9b1f-2a6d8e5c4b3a",
                                      "number": "SO-2026-000123", "customerId": 10, "vehicleId": 20,
                                      "mechanicId": 5, "status": "IN_DIAGNOSIS", "totalPrice": 407.70,
                                      "dateCreated": "2026-10-27T14:30:00Z"}],
                         "page": 1, "size": 5, "totalElements": 6, "totalPages": 2}
                        """));
    }

    @Test
    void shouldListWithoutFiltersUsingTheDefaultPage() throws Exception {
        ListServiceOrdersQuery query = new ListServiceOrdersQuery(null, null, null, null, null, 0, 20);
        when(listServiceOrdersUseCase.execute(query)).thenReturn(new Page<>(List.of(), new Pagination(0, 20), 0));

        mockMvc.perform(get("/admin/serviceOrders"))
                .andExpect(status().isOk())
                .andExpect(json("""
                        {"content": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0}
                        """));
    }

    @Test
    void shouldValidateFiltersAndPagination() throws Exception {
        mockMvc.perform(get("/admin/serviceOrders")
                        .param("customerDocument", "123.456.789-00")
                        .param("licensePlate", "AB-12345")
                        .param("page", "-1")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("request.invalidFields"))
                .andExpect(jsonPath("$.details[*].field")
                        .value(containsInAnyOrder("customerDocument", "licensePlate", "page", "size")));
        verifyNoInteractions(listServiceOrdersUseCase);
    }

    @ParameterizedTest
    @CsvSource({"status, OPEN", "openedFrom, 2026-10-01", "openedTo, 2026-10-31T23:59:59"})
    void shouldRejectUnknownStatusAndDatesWithoutOffset(String filter, String value) throws Exception {
        mockMvc.perform(get("/admin/serviceOrders").param(filter, value))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {"code": "request.invalidFields", "details": [{"field": "%s", "message": "Valor inválido."}]}
                        """.formatted(filter),
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(listServiceOrdersUseCase);
    }

    @Test
    void shouldFindTheWholeServiceOrder() throws Exception {
        when(findServiceOrderUseCase.execute(new FindServiceOrderQuery(1L))).thenReturn(IN_DIAGNOSIS);

        mockMvc.perform(get("/admin/serviceOrders/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.services[0].serviceOfferingId").value(30))
                .andExpect(jsonPath("$.parts[0].partId").value(40))
                .andExpect(jsonPath("$.totalPrice").value(407.70))
                .andExpect(jsonPath("$.statusHistory[*].toStatus").value(contains("RECEIVED", "IN_DIAGNOSIS")))
                .andExpect(jsonPath("$.statusHistory[1].changedAt").value("2026-10-27T15:00:00Z"));
    }

    @Test
    void shouldAnswer404ForAnUnknownServiceOrder() throws Exception {
        when(findServiceOrderUseCase.execute(new FindServiceOrderQuery(99L)))
                .thenThrow(new NotFoundException("serviceOrder.notFound", "99"));

        mockMvc.perform(get("/admin/serviceOrders/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("serviceOrder.notFound"));
    }

    private static MockHttpServletRequestBuilder cancel(String body) {
        return post("/admin/serviceOrders/1/cancel").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static MockHttpServletRequestBuilder open(String body) {
        return post("/admin/serviceOrders").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static ResultMatcher json(String expected) {
        return json(expected, JsonCompareMode.STRICT);
    }

    private static ResultMatcher json(String expected, JsonCompareMode mode) {
        return content().json(expected, mode);
    }
}
