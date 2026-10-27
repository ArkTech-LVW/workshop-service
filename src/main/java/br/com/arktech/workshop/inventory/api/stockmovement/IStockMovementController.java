package br.com.arktech.workshop.inventory.api.stockmovement;

import br.com.arktech.workshop.shared.api.pagination.PageResponse;
import br.com.arktech.workshop.shared.api.security.AllowedRoles;
import br.com.arktech.workshop.shared.domain.security.Role;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "${openapi.stockMovement.tag}")
@AllowedRoles(Role.ADMIN)
@RequestMapping("/admin/parts/{partId}/stockMovements")
interface IStockMovementController {

    @Operation(summary = "${openapi.stockMovement.create}", description = "${openapi.stockMovement.createDescription}")
    @ApiResponse(responseCode = "201")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    StockMovementResponse create(
            @Parameter(description = "${openapi.part.id}", example = "1") @PathVariable Long partId,
            @Valid @RequestBody StockMovementRequest request);

    @Operation(summary = "${openapi.stockMovement.list}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @GetMapping
    PageResponse<StockMovementResponse> list(
            @Parameter(description = "${openapi.part.id}", example = "1") @PathVariable Long partId,
            @Parameter(description = "${openapi.page.page}") @RequestParam(defaultValue = "0") @PositiveOrZero
                    int page,
            @Parameter(description = "${openapi.page.size}") @RequestParam(defaultValue = "20") @Positive @Max(100)
                    int size);
}
