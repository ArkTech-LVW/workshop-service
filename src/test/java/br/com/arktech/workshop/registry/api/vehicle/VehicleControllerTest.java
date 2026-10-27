package br.com.arktech.workshop.registry.api.vehicle;

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

import br.com.arktech.workshop.registry.application.createvehicle.CreateVehicleCommand;
import br.com.arktech.workshop.registry.application.createvehicle.CreateVehicleUseCase;
import br.com.arktech.workshop.registry.application.deletevehicle.DeleteVehicleCommand;
import br.com.arktech.workshop.registry.application.deletevehicle.DeleteVehicleUseCase;
import br.com.arktech.workshop.registry.application.findvehicle.FindVehicleUseCase;
import br.com.arktech.workshop.registry.application.listvehicles.ListVehiclesQuery;
import br.com.arktech.workshop.registry.application.listvehicles.ListVehiclesUseCase;
import br.com.arktech.workshop.registry.application.updatevehicle.UpdateVehicleCommand;
import br.com.arktech.workshop.registry.application.updatevehicle.UpdateVehicleUseCase;
import br.com.arktech.workshop.registry.application.vehicle.VehicleResult;
import br.com.arktech.workshop.shared.api.error.ApiErrorTestConfig;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import java.time.Year;
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

@WebMvcTest(controllers = {VehicleController.class, CustomerVehicleController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiErrorTestConfig.class)
class VehicleControllerTest {

    private static final VehicleResult GOL =
            new VehicleResult(1L, 1L, "ABC1D23", "9BWZZZ372VT004251", "Volkswagen", "Gol", 2020, "Prata");
    private static final String GOL_JSON = """
            {"id": 1, "customerId": 1, "licensePlate": "ABC1D23", "chassis": "9BWZZZ372VT004251",
             "brand": "Volkswagen", "model": "Gol", "year": 2020, "color": "Prata"}
            """;
    private static final String GOL_REQUEST = """
            {"customerId": 1, "licensePlate": "abc-1d23", "chassis": "9BWZZZ372VT004251",
             "brand": "Volkswagen", "model": "Gol", "year": 2020, "color": "Prata"}
            """;
    private static final String UPDATE_REQUEST = """
            {"licensePlate": "abc-1d23", "chassis": "9BWZZZ372VT004251", "color": "Prata"}
            """;
    private static final String INVALID_YEAR = "Ano inválido. Informe um ano entre 1981 e o ano que vem.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateVehicleUseCase createVehicleUseCase;

    @MockitoBean
    private ListVehiclesUseCase listVehiclesUseCase;

    @MockitoBean
    private FindVehicleUseCase findVehicleUseCase;

    @MockitoBean
    private UpdateVehicleUseCase updateVehicleUseCase;

    @MockitoBean
    private DeleteVehicleUseCase deleteVehicleUseCase;

    @Test
    void shouldCreateVehicle() throws Exception {
        when(createVehicleUseCase.execute(any())).thenReturn(GOL);

        mockMvc.perform(post("/admin/vehicles").contentType(MediaType.APPLICATION_JSON).content(GOL_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(json(GOL_JSON));
        verify(createVehicleUseCase)
                .execute(new CreateVehicleCommand(
                        1L, "abc-1d23", "9BWZZZ372VT004251", "Volkswagen", "Gol", 2020, "Prata"));
    }

    @Test
    void shouldCreateVehicleWithoutChassis() throws Exception {
        when(createVehicleUseCase.execute(any())).thenReturn(GOL);

        mockMvc.perform(post("/admin/vehicles").contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerId": 1, "licensePlate": "ABC1D23", "brand": "Volkswagen", "model": "Gol",
                         "year": 2020, "color": "Prata"}
                        """))
                .andExpect(status().isCreated());
        verify(createVehicleUseCase)
                .execute(new CreateVehicleCommand(1L, "ABC1D23", null, "Volkswagen", "Gol", 2020, "Prata"));
    }

    @Test
    void shouldRequireAllFieldsButChassis() throws Exception {
        mockMvc.perform(post("/admin/vehicles").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {
                          "code": "request.invalidFields",
                          "details": [
                            {"field": "brand", "message": "Não pode estar em branco."},
                            {"field": "color", "message": "Não pode estar em branco."},
                            {"field": "customerId", "message": "Campo obrigatório."},
                            {"field": "licensePlate", "message": "Não pode estar em branco."},
                            {"field": "model", "message": "Não pode estar em branco."},
                            {"field": "year", "message": "Campo obrigatório."}
                          ]
                        }
                        """,
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(createVehicleUseCase);
    }

    @Test
    void shouldValidateNewVehicle() throws Exception {
        mockMvc.perform(post("/admin/vehicles").contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerId": 1, "licensePlate": "AB-12345", "chassis": "9BWZZZ370VT004251",
                         "brand": "%s", "model": "Gol", "year": 1980, "color": "Prata"}
                        """.formatted("a".repeat(51))))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {
                          "code": "request.invalidFields",
                          "details": [
                            {"field": "brand", "message": "Deve ter no máximo 50 caracteres."},
                            {"field": "chassis",
                             "message": "Chassi inválido. Informe os 17 caracteres do chassi, sem as letras I, O e Q."},
                            {"field": "licensePlate", "message": "Placa inválida."},
                            {"field": "year", "message": "%s"}
                          ]
                        }
                        """.formatted(INVALID_YEAR),
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(createVehicleUseCase);
    }

    @Test
    void shouldRejectYearAfterNextYear() throws Exception {
        mockMvc.perform(post("/admin/vehicles").contentType(MediaType.APPLICATION_JSON).content("""
                        {"customerId": 1, "licensePlate": "ABC1D23", "brand": "Volkswagen", "model": "Gol",
                         "year": %d, "color": "Prata"}
                        """.formatted(Year.now().getValue() + 2)))
                .andExpect(status().isBadRequest())
                .andExpect(json("""
                        {"details": [{"field": "year", "message": "%s"}]}
                        """.formatted(INVALID_YEAR), JsonCompareMode.LENIENT));
        verifyNoInteractions(createVehicleUseCase);
    }

    @Test
    void shouldAnswer404WhenCustomerDoesNotExist() throws Exception {
        when(createVehicleUseCase.execute(any())).thenThrow(new NotFoundException("customer.notFound", "1"));

        mockMvc.perform(post("/admin/vehicles").contentType(MediaType.APPLICATION_JSON).content(GOL_REQUEST))
                .andExpect(status().isNotFound())
                .andExpect(json("""
                        {"code": "customer.notFound"}
                        """, JsonCompareMode.LENIENT));
    }

    @Test
    void shouldAnswer409WhenLicensePlateIsTaken() throws Exception {
        when(createVehicleUseCase.execute(any()))
                .thenThrow(new ConflictException("vehicle.licensePlateAlreadyExists", "ABC1D23"));

        mockMvc.perform(post("/admin/vehicles").contentType(MediaType.APPLICATION_JSON).content(GOL_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(json("""
                        {"code": "vehicle.licensePlateAlreadyExists",
                         "message": "Já existe um veículo com a placa ABC1D23."}
                        """));
    }

    @Test
    void shouldAnswer409WhenChassisIsTaken() throws Exception {
        when(createVehicleUseCase.execute(any()))
                .thenThrow(new ConflictException("vehicle.chassisAlreadyExists", "9BWZZZ372VT004251"));

        mockMvc.perform(post("/admin/vehicles").contentType(MediaType.APPLICATION_JSON).content(GOL_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(json("""
                        {"code": "vehicle.chassisAlreadyExists",
                         "message": "Já existe um veículo com o chassi 9BWZZZ372VT004251."}
                        """));
    }

    @Test
    void shouldListWithDefaultPagination() throws Exception {
        when(listVehiclesUseCase.execute(new ListVehiclesQuery(null, null, 0, 20)))
                .thenReturn(new Page<>(List.of(GOL), new Pagination(0, 20), 1));

        mockMvc.perform(get("/admin/vehicles"))
                .andExpect(status().isOk())
                .andExpect(json("""
                        {"content": [%s], "page": 0, "size": 20, "totalElements": 1, "totalPages": 1}
                        """.formatted(GOL_JSON)));
    }

    @Test
    void shouldFilterByLicensePlate() throws Exception {
        when(listVehiclesUseCase.execute(new ListVehiclesQuery("abc-1d23", null, 0, 20)))
                .thenReturn(new Page<>(List.of(GOL), new Pagination(0, 20), 1));

        mockMvc.perform(get("/admin/vehicles").param("licensePlate", "abc-1d23"))
                .andExpect(status().isOk())
                .andExpect(json("""
                        {"content": [%s], "totalElements": 1}
                        """.formatted(GOL_JSON), JsonCompareMode.LENIENT));
    }

    @Test
    void shouldValidateLicensePlateFilterAndPagination() throws Exception {
        mockMvc.perform(get("/admin/vehicles")
                        .param("licensePlate", "123")
                        .param("page", "-1")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {
                          "code": "request.invalidFields",
                          "details": [
                            {"field": "licensePlate", "message": "Placa inválida."},
                            {"field": "page", "message": "Deve ser maior ou igual a zero."},
                            {"field": "size", "message": "Deve ser no máximo 100."}
                          ]
                        }
                        """,
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(listVehiclesUseCase);
    }

    @Test
    void shouldListVehiclesOfCustomer() throws Exception {
        when(listVehiclesUseCase.execute(new ListVehiclesQuery(null, 7L, 0, 20)))
                .thenReturn(new Page<>(List.of(GOL), new Pagination(0, 20), 1));

        mockMvc.perform(get("/admin/customers/7/vehicles"))
                .andExpect(status().isOk())
                .andExpect(json("""
                        {"content": [%s], "page": 0, "size": 20, "totalElements": 1, "totalPages": 1}
                        """.formatted(GOL_JSON)));
    }

    @Test
    void shouldAnswer404WhenListingVehiclesOfUnknownCustomer() throws Exception {
        when(listVehiclesUseCase.execute(any())).thenThrow(new NotFoundException("customer.notFound", "1234"));

        mockMvc.perform(get("/admin/customers/1234/vehicles"))
                .andExpect(status().isNotFound())
                .andExpect(json("""
                        {"code": "customer.notFound", "message": "Cliente 1234 não encontrado."}
                        """));
    }

    @Test
    void shouldFindVehicle() throws Exception {
        when(findVehicleUseCase.execute(any())).thenReturn(GOL);

        mockMvc.perform(get("/admin/vehicles/1")).andExpect(status().isOk()).andExpect(json(GOL_JSON));
    }

    @Test
    void shouldAnswer404ForUnknownVehicle() throws Exception {
        when(findVehicleUseCase.execute(any())).thenThrow(new NotFoundException("vehicle.notFound", "1234"));

        mockMvc.perform(get("/admin/vehicles/1234"))
                .andExpect(status().isNotFound())
                .andExpect(json("""
                        {"code": "vehicle.notFound", "message": "Veículo 1234 não encontrado."}
                        """));
    }

    @Test
    void shouldUpdateVehicle() throws Exception {
        when(updateVehicleUseCase.execute(any())).thenReturn(GOL);

        mockMvc.perform(put("/admin/vehicles/1").contentType(MediaType.APPLICATION_JSON).content(UPDATE_REQUEST))
                .andExpect(status().isOk())
                .andExpect(json(GOL_JSON));
        verify(updateVehicleUseCase).execute(new UpdateVehicleCommand(1L, "abc-1d23", "9BWZZZ372VT004251", "Prata"));
    }

    @Test
    void shouldUpdateVehicleWithoutChassis() throws Exception {
        when(updateVehicleUseCase.execute(any())).thenReturn(GOL);

        mockMvc.perform(put("/admin/vehicles/1").contentType(MediaType.APPLICATION_JSON).content("""
                        {"licensePlate": "ABC1D23", "color": "Prata"}
                        """))
                .andExpect(status().isOk());
        verify(updateVehicleUseCase).execute(new UpdateVehicleCommand(1L, "ABC1D23", null, "Prata"));
    }

    @Test
    void shouldRequirePlateAndColorOnUpdate() throws Exception {
        mockMvc.perform(put("/admin/vehicles/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {
                          "code": "request.invalidFields",
                          "details": [
                            {"field": "color", "message": "Não pode estar em branco."},
                            {"field": "licensePlate", "message": "Não pode estar em branco."}
                          ]
                        }
                        """,
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(updateVehicleUseCase);
    }

    @Test
    void shouldValidateUpdatedVehicle() throws Exception {
        mockMvc.perform(put("/admin/vehicles/1").contentType(MediaType.APPLICATION_JSON).content("""
                        {"licensePlate": "AB-12345", "chassis": "9BWZZZ370VT004251", "color": "%s"}
                        """.formatted("a".repeat(31))))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {
                          "code": "request.invalidFields",
                          "details": [
                            {"field": "chassis",
                             "message": "Chassi inválido. Informe os 17 caracteres do chassi, sem as letras I, O e Q."},
                            {"field": "color", "message": "Deve ter no máximo 30 caracteres."},
                            {"field": "licensePlate", "message": "Placa inválida."}
                          ]
                        }
                        """,
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(updateVehicleUseCase);
    }

    @Test
    void shouldAnswer409WhenUpdatingToAnotherVehiclesPlate() throws Exception {
        when(updateVehicleUseCase.execute(any()))
                .thenThrow(new ConflictException("vehicle.licensePlateAlreadyExists", "ABC1D23"));

        mockMvc.perform(put("/admin/vehicles/1").contentType(MediaType.APPLICATION_JSON).content(UPDATE_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(json("""
                        {"code": "vehicle.licensePlateAlreadyExists"}
                        """, JsonCompareMode.LENIENT));
    }

    @Test
    void shouldDeleteVehicle() throws Exception {
        mockMvc.perform(delete("/admin/vehicles/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(deleteVehicleUseCase).execute(new DeleteVehicleCommand(1L));
    }

    @Test
    void shouldAnswer409WhenVehicleIsInUse() throws Exception {
        doThrow(new ConflictException("vehicle.inUse", "1")).when(deleteVehicleUseCase).execute(any());

        mockMvc.perform(delete("/admin/vehicles/1"))
                .andExpect(status().isConflict())
                .andExpect(json("""
                        {"code": "vehicle.inUse",
                         "message": "O veículo 1 tem ordens de serviço ligadas e não pode ser excluído."}
                        """));
    }

    private static ResultMatcher json(String expected) {
        return json(expected, JsonCompareMode.STRICT);
    }

    private static ResultMatcher json(String expected, JsonCompareMode mode) {
        return content().json(expected, mode);
    }
}
