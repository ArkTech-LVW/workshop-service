package br.com.arktech.workshop.inventory.api.part;

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

import br.com.arktech.workshop.inventory.application.createpart.CreatePartCommand;
import br.com.arktech.workshop.inventory.application.createpart.CreatePartUseCase;
import br.com.arktech.workshop.inventory.application.deletepart.DeletePartCommand;
import br.com.arktech.workshop.inventory.application.deletepart.DeletePartUseCase;
import br.com.arktech.workshop.inventory.application.findpart.FindPartUseCase;
import br.com.arktech.workshop.inventory.application.listparts.ListPartsQuery;
import br.com.arktech.workshop.inventory.application.listparts.ListPartsUseCase;
import br.com.arktech.workshop.inventory.application.part.PartResult;
import br.com.arktech.workshop.inventory.application.updatepart.UpdatePartCommand;
import br.com.arktech.workshop.inventory.application.updatepart.UpdatePartUseCase;
import br.com.arktech.workshop.inventory.domain.model.MeasurementUnit;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.shared.api.error.ApiErrorTestConfig;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
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

@WebMvcTest(controllers = PartController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiErrorTestConfig.class)
class PartControllerTest {

    private static final PartResult OIL = new PartResult(
            1L,
            "OLEO-5W30-1L",
            "Óleo 5W30 1L",
            PartType.SUPPLY,
            MeasurementUnit.LITER,
            new BigDecimal("45.90"),
            0,
            10);
    private static final String OIL_JSON = """
            {"id": 1, "sku": "OLEO-5W30-1L", "name": "Óleo 5W30 1L", "type": "SUPPLY", "unit": "LITER",
             "salePrice": 45.90, "stockQuantity": 0, "minimumStock": 10}
            """;
    private static final String OIL_REQUEST = """
            {"sku": "oleo-5w30-1l", "name": "Óleo 5W30 1L", "type": "SUPPLY", "unit": "LITER",
             "salePrice": 45.90, "minimumStock": 10}
            """;
    private static final String MONEY_INVALID =
            "Valor inválido. Informe um valor maior ou igual a zero, com até 10 dígitos inteiros e duas casas decimais.";
    private static final String SKU_INVALID =
            "SKU inválido. Use até 50 letras, números, ponto, hífen, sublinhado ou barra.";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreatePartUseCase createPartUseCase;

    @MockitoBean
    private ListPartsUseCase listPartsUseCase;

    @MockitoBean
    private FindPartUseCase findPartUseCase;

    @MockitoBean
    private UpdatePartUseCase updatePartUseCase;

    @MockitoBean
    private DeletePartUseCase deletePartUseCase;

    @Test
    void shouldCreatePart() throws Exception {
        when(createPartUseCase.execute(any())).thenReturn(OIL);

        mockMvc.perform(post("/admin/parts").contentType(MediaType.APPLICATION_JSON).content(OIL_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(json(OIL_JSON));
        verify(createPartUseCase)
                .execute(new CreatePartCommand(
                        "oleo-5w30-1l",
                        "Óleo 5W30 1L",
                        PartType.SUPPLY,
                        MeasurementUnit.LITER,
                        new BigDecimal("45.90"),
                        10));
    }

    @Test
    void shouldIgnoreStockQuantityOnCreate() throws Exception {
        when(createPartUseCase.execute(any())).thenReturn(OIL);

        mockMvc.perform(post("/admin/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku": "OLEO-5W30-1L", "name": "Óleo 5W30 1L", "type": "SUPPLY", "unit": "LITER",
                                 "salePrice": 45.90, "minimumStock": 10, "stockQuantity": 50}
                                """))
                .andExpect(status().isCreated())
                .andExpect(json(OIL_JSON));
    }

    @Test
    void shouldRequireAllFields() throws Exception {
        mockMvc.perform(post("/admin/parts").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {
                          "code": "request.invalidFields",
                          "details": [
                            {"field": "minimumStock", "message": "Campo obrigatório."},
                            {"field": "name", "message": "Não pode estar em branco."},
                            {"field": "salePrice", "message": "Campo obrigatório."},
                            {"field": "sku", "message": "Campo obrigatório."},
                            {"field": "type", "message": "Campo obrigatório."},
                            {"field": "unit", "message": "Campo obrigatório."}
                          ]
                        }
                        """,
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(createPartUseCase);
    }

    @Test
    void shouldValidateNewPart() throws Exception {
        mockMvc.perform(post("/admin/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku": "OLEO 5W30", "name": "%s", "type": "SUPPLY", "unit": "LITER",
                                 "salePrice": -1, "minimumStock": -1}
                                """.formatted("a".repeat(151))))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {
                          "code": "request.invalidFields",
                          "details": [
                            {"field": "minimumStock", "message": "Deve ser maior ou igual a zero."},
                            {"field": "name", "message": "Deve ter no máximo 150 caracteres."},
                            {"field": "salePrice", "message": "%s"},
                            {"field": "sku", "message": "%s"}
                          ]
                        }
                        """.formatted(MONEY_INVALID, SKU_INVALID),
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(createPartUseCase);
    }

    @Test
    void shouldRejectSalePriceWithMoreThanTwoDecimalPlaces() throws Exception {
        mockMvc.perform(post("/admin/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku": "F-1", "name": "Filtro", "type": "PART", "unit": "UNIT",
                                 "salePrice": 10.555, "minimumStock": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {"code": "request.invalidFields",
                         "details": [{"field": "salePrice", "message": "%s"}]}
                        """.formatted(MONEY_INVALID),
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(createPartUseCase);
    }

    @Test
    void shouldRejectSkuLongerThanFiftyCharacters() throws Exception {
        mockMvc.perform(post("/admin/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku": "%s", "name": "Filtro", "type": "PART", "unit": "UNIT",
                                 "salePrice": 10, "minimumStock": 0}
                                """.formatted("A".repeat(51))))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {"code": "request.invalidFields", "details": [{"field": "sku", "message": "%s"}]}
                        """.formatted(SKU_INVALID),
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(createPartUseCase);
    }

    @Test
    void shouldRejectUnknownTypeAndUnit() throws Exception {
        mockMvc.perform(post("/admin/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku": "F-1", "name": "Filtro", "type": "TOOL", "unit": "UNIT",
                                 "salePrice": 10, "minimumStock": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {"code": "request.invalidFields", "details": [{"field": "type", "message": "Valor inválido."}]}
                        """,
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(createPartUseCase);
    }

    @Test
    void shouldAnswer409ForDuplicatedSku() throws Exception {
        when(createPartUseCase.execute(any()))
                .thenThrow(new ConflictException("part.skuAlreadyExists", "OLEO-5W30-1L"));

        mockMvc.perform(post("/admin/parts").contentType(MediaType.APPLICATION_JSON).content(OIL_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(json(
                        """
                        {"code": "part.skuAlreadyExists",
                         "message": "Já existe uma peça ou insumo com o SKU OLEO-5W30-1L."}
                        """,
                        JsonCompareMode.LENIENT));
    }

    @Test
    void shouldListWithDefaultPagination() throws Exception {
        when(listPartsUseCase.execute(new ListPartsQuery(null, null, null, 0, 20)))
                .thenReturn(new Page<>(List.of(OIL), new Pagination(0, 20), 1));

        mockMvc.perform(get("/admin/parts"))
                .andExpect(status().isOk())
                .andExpect(json("""
                        {"content": [%s], "page": 0, "size": 20, "totalElements": 1, "totalPages": 1}
                        """.formatted(OIL_JSON)));
    }

    @Test
    void shouldFilterByNameSkuAndType() throws Exception {
        when(listPartsUseCase.execute(new ListPartsQuery("óleo", "oleo-5w30-1l", PartType.SUPPLY, 0, 20)))
                .thenReturn(new Page<>(List.of(OIL), new Pagination(0, 20), 1));

        mockMvc.perform(get("/admin/parts")
                        .param("name", "óleo")
                        .param("sku", "oleo-5w30-1l")
                        .param("type", "SUPPLY"))
                .andExpect(status().isOk())
                .andExpect(json("""
                        {"content": [%s], "totalElements": 1}
                        """.formatted(OIL_JSON), JsonCompareMode.LENIENT));
    }

    @Test
    void shouldValidateFiltersAndPagination() throws Exception {
        mockMvc.perform(get("/admin/parts")
                        .param("sku", "OLEO 5W30")
                        .param("page", "-1")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {
                          "code": "request.invalidFields",
                          "details": [
                            {"field": "page", "message": "Deve ser maior ou igual a zero."},
                            {"field": "size", "message": "Deve ser no máximo 100."},
                            {"field": "sku", "message": "%s"}
                          ]
                        }
                        """.formatted(SKU_INVALID),
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(listPartsUseCase);
    }

    @Test
    void shouldRejectUnknownTypeFilter() throws Exception {
        mockMvc.perform(get("/admin/parts").param("type", "TOOL"))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {"code": "request.invalidFields", "details": [{"field": "type", "message": "Valor inválido."}]}
                        """,
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(listPartsUseCase);
    }

    @Test
    void shouldFindPart() throws Exception {
        when(findPartUseCase.execute(any())).thenReturn(OIL);

        mockMvc.perform(get("/admin/parts/1")).andExpect(status().isOk()).andExpect(json(OIL_JSON));
    }

    @Test
    void shouldAnswer404ForUnknownPart() throws Exception {
        when(findPartUseCase.execute(any())).thenThrow(new NotFoundException("part.notFound", "1234"));

        mockMvc.perform(get("/admin/parts/1234"))
                .andExpect(status().isNotFound())
                .andExpect(json(
                        """
                        {"code": "part.notFound", "message": "Peça ou insumo 1234 não encontrado."}
                        """,
                        JsonCompareMode.LENIENT));
    }

    @Test
    void shouldUpdatePartIgnoringStockQuantity() throws Exception {
        when(updatePartUseCase.execute(any())).thenReturn(OIL);

        mockMvc.perform(put("/admin/parts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku": "oleo-5w30-1l", "name": "Óleo 5W30 1L", "type": "SUPPLY", "unit": "LITER",
                                 "salePrice": 45.90, "minimumStock": 10, "stockQuantity": 50}
                                """))
                .andExpect(status().isOk())
                .andExpect(json(OIL_JSON));
        verify(updatePartUseCase)
                .execute(new UpdatePartCommand(
                        1L,
                        "oleo-5w30-1l",
                        "Óleo 5W30 1L",
                        PartType.SUPPLY,
                        MeasurementUnit.LITER,
                        new BigDecimal("45.90"),
                        10));
    }

    @Test
    void shouldValidateUpdatedPart() throws Exception {
        mockMvc.perform(put("/admin/parts/1").contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku": "OLEO 5W30", "name": "", "type": "SUPPLY", "unit": "LITER",
                         "salePrice": 1.005, "minimumStock": -1}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(json(
                        """
                        {
                          "code": "request.invalidFields",
                          "details": [
                            {"field": "minimumStock", "message": "Deve ser maior ou igual a zero."},
                            {"field": "name", "message": "Não pode estar em branco."},
                            {"field": "salePrice", "message": "%s"},
                            {"field": "sku", "message": "%s"}
                          ]
                        }
                        """.formatted(MONEY_INVALID, SKU_INVALID),
                        JsonCompareMode.LENIENT));
        verifyNoInteractions(updatePartUseCase);
    }

    @Test
    void shouldAnswer404And409OnUpdate() throws Exception {
        when(updatePartUseCase.execute(any()))
                .thenThrow(new NotFoundException("part.notFound", "1234"))
                .thenThrow(new ConflictException("part.skuAlreadyExists", "OLEO-5W30-1L"));

        mockMvc.perform(put("/admin/parts/1234").contentType(MediaType.APPLICATION_JSON).content(OIL_REQUEST))
                .andExpect(status().isNotFound())
                .andExpect(json("""
                        {"code": "part.notFound"}
                        """, JsonCompareMode.LENIENT));
        mockMvc.perform(put("/admin/parts/1").contentType(MediaType.APPLICATION_JSON).content(OIL_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(json("""
                        {"code": "part.skuAlreadyExists"}
                        """, JsonCompareMode.LENIENT));
    }

    @Test
    void shouldDeletePart() throws Exception {
        mockMvc.perform(delete("/admin/parts/1")).andExpect(status().isNoContent());

        verify(deletePartUseCase).execute(new DeletePartCommand(1L));
    }

    @Test
    void shouldAnswer409WhenPartIsInUse() throws Exception {
        doThrow(new ConflictException("part.inUse", "1")).when(deletePartUseCase).execute(any());

        mockMvc.perform(delete("/admin/parts/1"))
                .andExpect(status().isConflict())
                .andExpect(json(
                        """
                        {"code": "part.inUse",
                         "message": "A peça ou insumo 1 tem movimentações de estoque ou ordens de serviço ligadas e não pode ser excluído."}
                        """,
                        JsonCompareMode.LENIENT));
    }

    @Test
    void shouldAnswer404WhenDeletingUnknownPart() throws Exception {
        doThrow(new NotFoundException("part.notFound", "1234")).when(deletePartUseCase).execute(any());

        mockMvc.perform(delete("/admin/parts/1234"))
                .andExpect(status().isNotFound())
                .andExpect(json("""
                        {"code": "part.notFound"}
                        """, JsonCompareMode.LENIENT));
    }

    private static ResultMatcher json(String expected) {
        return json(expected, JsonCompareMode.STRICT);
    }

    private static ResultMatcher json(String expected, JsonCompareMode mode) {
        return content().json(expected, mode);
    }
}
