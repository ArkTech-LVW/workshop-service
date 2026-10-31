package br.com.arktech.workshop.inventory.api.stockadjustmentrequest;

import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequestStatus;
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

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "${openapi.stockAdjustmentRequest.tag}")
@AllowedRoles(Role.ADMIN)
@RequestMapping("/admin")
interface IStockAdjustmentRequestController {

    @Operation(
            summary = "${openapi.stockAdjustmentRequest.create}",
            description = "${openapi.stockAdjustmentRequest.createDescription}")
    @ApiResponse(responseCode = "201")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @AllowedRoles({Role.ADMIN, Role.ATTENDANT})
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/parts/{partId}/stockAdjustmentRequests")
    StockAdjustmentRequestResponse create(
            @Parameter(description = "${openapi.part.id}", example = "1") @PathVariable Long partId,
            @Valid @RequestBody CreateStockAdjustmentRequestRequest request);

    @Operation(summary = "${openapi.stockAdjustmentRequest.list}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @AllowedRoles(Role.ADMIN)
    @GetMapping("/stockAdjustmentRequests")
    PageResponse<StockAdjustmentRequestResponse> list(
            @Parameter(description = "${openapi.stockAdjustmentRequest.statusFilter}", example = "PENDING")
                    @RequestParam(required = false) @Nullable StockAdjustmentRequestStatus status,
            @Parameter(description = "${openapi.page.page}") @RequestParam(defaultValue = "0") @PositiveOrZero
                    int page,
            @Parameter(description = "${openapi.page.size}") @RequestParam(defaultValue = "20") @Positive @Max(100)
                    int size);

    @Operation(
            summary = "${openapi.stockAdjustmentRequest.approve}",
            description = "${openapi.stockAdjustmentRequest.approveDescription}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @PostMapping("/stockAdjustmentRequests/{id}/approve")
    StockAdjustmentRequestResponse approve(
            @Parameter(description = "${openapi.stockAdjustmentRequest.id}", example = "1") @PathVariable Long id);

    @Operation(
            summary = "${openapi.stockAdjustmentRequest.reject}",
            description = "${openapi.stockAdjustmentRequest.rejectDescription}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @PostMapping("/stockAdjustmentRequests/{id}/reject")
    StockAdjustmentRequestResponse reject(
            @Parameter(description = "${openapi.stockAdjustmentRequest.id}", example = "1") @PathVariable Long id,
            @Valid @RequestBody RejectStockAdjustmentRequestRequest request);
}
