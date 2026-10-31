package br.com.arktech.workshop.serviceorder.api.serviceorder;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderStatus;
import br.com.arktech.workshop.shared.api.pagination.PageResponse;
import br.com.arktech.workshop.shared.api.security.AllowedRoles;
import br.com.arktech.workshop.shared.api.validation.CPFOrCNPJ;
import br.com.arktech.workshop.shared.api.validation.LicensePlate;
import br.com.arktech.workshop.shared.domain.security.Role;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "${openapi.serviceOrder.tag}")
@AllowedRoles({Role.ADMIN, Role.ATTENDANT})
@RequestMapping("/admin/serviceOrders")
interface IServiceOrderController {

    @Operation(summary = "${openapi.serviceOrder.open}", description = "${openapi.serviceOrder.openDescription}")
    @ApiResponse(responseCode = "201")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "409", ref = "Conflict")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    ServiceOrderResponse open(@Valid @RequestBody OpenServiceOrderRequest request);

    @Operation(summary = "${openapi.serviceOrder.list}", description = "${openapi.serviceOrder.listDescription}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @GetMapping
    PageResponse<ServiceOrderSummaryResponse> list(
            @Parameter(description = "${openapi.serviceOrder.statusFilter}", example = "RECEIVED")
                    @RequestParam(required = false) @Nullable ServiceOrderStatus status,
            @Parameter(description = "${openapi.serviceOrder.customerDocumentFilter}", example = "529.982.247-25")
                    @RequestParam(required = false) @CPFOrCNPJ @Nullable String customerDocument,
            @Parameter(description = "${openapi.serviceOrder.licensePlateFilter}", example = "ABC-1D23")
                    @RequestParam(required = false) @LicensePlate @Nullable String licensePlate,
            @Parameter(description = "${openapi.serviceOrder.openedFromFilter}", example = "2026-10-01T00:00:00Z")
                    @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) @Nullable Instant openedFrom,
            @Parameter(description = "${openapi.serviceOrder.openedToFilter}", example = "2026-10-31T23:59:59Z")
                    @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) @Nullable Instant openedTo,
            @Parameter(description = "${openapi.page.page}") @RequestParam(defaultValue = "0") @PositiveOrZero
                    int page,
            @Parameter(description = "${openapi.page.size}") @RequestParam(defaultValue = "20") @Positive @Max(100)
                    int size);

    @Operation(
            summary = "${openapi.serviceOrder.findById}",
            description = "${openapi.serviceOrder.findByIdDescription}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @GetMapping("/{id}")
    ServiceOrderResponse findById(
            @Parameter(description = "${openapi.serviceOrder.id}", example = "1") @PathVariable Long id);

    @Operation(
            summary = "${openapi.serviceOrder.startDiagnosis}",
            description = "${openapi.serviceOrder.startDiagnosisDescription}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "409", ref = "Conflict")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @AllowedRoles({Role.ADMIN, Role.MECHANIC})
    @PostMapping("/{id}/startDiagnosis")
    ServiceOrderResponse startDiagnosis(
            @Parameter(description = "${openapi.serviceOrder.id}", example = "1") @PathVariable Long id);

    @Operation(
            summary = "${openapi.serviceOrder.sendQuote}",
            description = "${openapi.serviceOrder.sendQuoteDescription}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "409", ref = "Conflict")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @AllowedRoles({Role.ADMIN, Role.ATTENDANT, Role.MECHANIC})
    @PostMapping("/{id}/sendQuote")
    ServiceOrderResponse sendQuote(
            @Parameter(description = "${openapi.serviceOrder.id}", example = "1") @PathVariable Long id,
            @RequestBody(required = false) @Nullable SendQuoteRequest request);

    @Operation(
            summary = "${openapi.serviceOrder.approveQuote}",
            description = "${openapi.serviceOrder.approveQuoteDescription}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "409", ref = "Conflict")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @PostMapping("/{id}/approve")
    ServiceOrderResponse approve(
            @Parameter(description = "${openapi.serviceOrder.id}", example = "1") @PathVariable Long id);

    @Operation(
            summary = "${openapi.serviceOrder.approveOriginalOnly}",
            description = "${openapi.serviceOrder.approveOriginalOnlyDescription}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "409", ref = "Conflict")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @PostMapping("/{id}/approveOriginalOnly")
    ServiceOrderResponse approveOriginalOnly(
            @Parameter(description = "${openapi.serviceOrder.id}", example = "1") @PathVariable Long id);

    @Operation(
            summary = "${openapi.serviceOrder.rejectQuote}",
            description = "${openapi.serviceOrder.rejectQuoteDescription}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "409", ref = "Conflict")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @PostMapping("/{id}/reject")
    ServiceOrderResponse reject(
            @Parameter(description = "${openapi.serviceOrder.id}", example = "1") @PathVariable Long id,
            @Valid @RequestBody RejectQuoteRequest request);

    @Operation(
            summary = "${openapi.serviceOrder.reopenQuote}",
            description = "${openapi.serviceOrder.reopenQuoteDescription}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "409", ref = "Conflict")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @AllowedRoles(Role.ADMIN)
    @PostMapping("/{id}/reopenQuote")
    ServiceOrderResponse reopenQuote(
            @Parameter(description = "${openapi.serviceOrder.id}", example = "1") @PathVariable Long id,
            @Valid @RequestBody(required = false) @Nullable ReopenQuoteRequest request);

    @Operation(
            summary = "${openapi.serviceOrder.startExecution}",
            description = "${openapi.serviceOrder.startExecutionDescription}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "409", ref = "Conflict")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @AllowedRoles({Role.ADMIN, Role.MECHANIC})
    @PostMapping("/{id}/startExecution")
    ServiceOrderResponse startExecution(
            @Parameter(description = "${openapi.serviceOrder.id}", example = "1") @PathVariable Long id);

    @Operation(
            summary = "${openapi.serviceOrder.completeService}",
            description = "${openapi.serviceOrder.completeServiceDescription}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "409", ref = "Conflict")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @AllowedRoles({Role.ADMIN, Role.MECHANIC})
    @PostMapping("/{id}/services/{itemId}/complete")
    ServiceOrderResponse completeService(
            @Parameter(description = "${openapi.serviceOrder.id}", example = "1") @PathVariable Long id,
            @Parameter(description = "${openapi.serviceOrderItem.itemId}", example = "1") @PathVariable Long itemId);

    @Operation(
            summary = "${openapi.serviceOrder.complete}",
            description = "${openapi.serviceOrder.completeDescription}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "409", ref = "Conflict")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @AllowedRoles({Role.ADMIN, Role.MECHANIC})
    @PostMapping("/{id}/complete")
    ServiceOrderResponse complete(
            @Parameter(description = "${openapi.serviceOrder.id}", example = "1") @PathVariable Long id);

    @Operation(summary = "${openapi.serviceOrder.deliver}", description = "${openapi.serviceOrder.deliverDescription}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "409", ref = "Conflict")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @PostMapping("/{id}/deliver")
    ServiceOrderResponse deliver(
            @Parameter(description = "${openapi.serviceOrder.id}", example = "1") @PathVariable Long id);

    @Operation(summary = "${openapi.serviceOrder.cancel}", description = "${openapi.serviceOrder.cancelDescription}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "409", ref = "Conflict")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @PostMapping("/{id}/cancel")
    ServiceOrderResponse cancel(
            @Parameter(description = "${openapi.serviceOrder.id}", example = "1") @PathVariable Long id,
            @Valid @RequestBody CancelServiceOrderRequest request);
}
