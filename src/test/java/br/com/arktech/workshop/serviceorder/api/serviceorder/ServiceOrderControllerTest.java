package br.com.arktech.workshop.serviceorder.api.serviceorder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.serviceorder.application.openserviceorder.OpenServiceOrderCommand;
import br.com.arktech.workshop.serviceorder.application.openserviceorder.OpenServiceOrderUseCase;
import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceOrderResult;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.shared.api.error.ApiErrorTestConfig;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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

@WebMvcTest(controllers = ServiceOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiErrorTestConfig.class)
class ServiceOrderControllerTest {

    private static final UUID PUBLIC_ID = UUID.fromString("3f1c2b8e-7d4a-4c9e-9b1f-2a6d8e5c4b3a");
    private static final ServiceOrderResult OPENED = new ServiceOrderResult(
            1L,
            PUBLIC_ID,
            "SO-2026-000123",
            10L,
            20L,
            null,
            "Barulho ao frear",
            ServiceOrderStatus.RECEIVED,
            new BigDecimal("0.00"),
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

    @Test
    void shouldOpenServiceOrder() throws Exception {
        when(openServiceOrderUseCase.execute(any())).thenReturn(OPENED);

        mockMvc.perform(open(OPEN_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(json("""
                        {"id": 1, "publicId": "3f1c2b8e-7d4a-4c9e-9b1f-2a6d8e5c4b3a", "number": "SO-2026-000123",
                         "customerId": 10, "vehicleId": 20, "mechanicId": null,
                         "problemDescription": "Barulho ao frear", "status": "RECEIVED", "totalPrice": 0.00,
                         "dateCreated": "2026-10-27T14:30:00Z"}
                        """));
        verify(openServiceOrderUseCase)
                .execute(new OpenServiceOrderCommand(
                        "529.982.247-25",
                        new OpenServiceOrderCommand.Vehicle("abc-1d23", "Volkswagen", "Gol", 2020, "Prata"),
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
