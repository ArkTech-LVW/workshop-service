package br.com.arktech.workshop.serviceorder.api.serviceorder;

import br.com.arktech.workshop.shared.api.security.AllowedRoles;
import br.com.arktech.workshop.shared.domain.security.Role;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @Operation(
            summary = "${openapi.serviceOrder.startDiagnosis}",
            description = "${openapi.serviceOrder.startDiagnosisDescription}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "404", ref = "NotFound")
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
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @AllowedRoles({Role.ADMIN, Role.ATTENDANT, Role.MECHANIC})
    @PostMapping("/{id}/sendQuote")
    ServiceOrderResponse sendQuote(
            @Parameter(description = "${openapi.serviceOrder.id}", example = "1") @PathVariable Long id);
}
