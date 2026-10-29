package br.com.arktech.workshop.serviceorder.api.serviceorderitem;

import br.com.arktech.workshop.shared.api.security.AllowedRoles;
import br.com.arktech.workshop.shared.domain.security.Role;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "${openapi.serviceOrder.tag}")
@AllowedRoles({Role.ADMIN, Role.ATTENDANT, Role.MECHANIC})
@RequestMapping("/admin/serviceOrders/{id}")
interface IServiceOrderItemController {

    @Operation(
            summary = "${openapi.serviceOrderItem.addService}",
            description = "${openapi.serviceOrderItem.addServiceDescription}")
    @ApiResponse(responseCode = "201")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/services")
    ServiceItemResponse addService(
            @Parameter(description = "${openapi.serviceOrder.id}", example = "1") @PathVariable Long id,
            @Valid @RequestBody AddServiceItemRequest request);

    @Operation(
            summary = "${openapi.serviceOrderItem.removeService}",
            description = "${openapi.serviceOrderItem.removeDescription}")
    @ApiResponse(responseCode = "204")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/services/{itemId}")
    void removeService(
            @Parameter(description = "${openapi.serviceOrder.id}", example = "1") @PathVariable Long id,
            @Parameter(description = "${openapi.serviceOrderItem.itemId}", example = "1") @PathVariable Long itemId);

    @Operation(
            summary = "${openapi.serviceOrderItem.addPart}",
            description = "${openapi.serviceOrderItem.addPartDescription}")
    @ApiResponse(responseCode = "201")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/parts")
    PartItemResponse addPart(
            @Parameter(description = "${openapi.serviceOrder.id}", example = "1") @PathVariable Long id,
            @Valid @RequestBody AddPartItemRequest request);

    @Operation(
            summary = "${openapi.serviceOrderItem.removePart}",
            description = "${openapi.serviceOrderItem.removeDescription}")
    @ApiResponse(responseCode = "204")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/parts/{itemId}")
    void removePart(
            @Parameter(description = "${openapi.serviceOrder.id}", example = "1") @PathVariable Long id,
            @Parameter(description = "${openapi.serviceOrderItem.itemId}", example = "1") @PathVariable Long itemId);
}
