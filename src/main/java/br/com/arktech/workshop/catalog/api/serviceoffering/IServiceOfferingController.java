package br.com.arktech.workshop.catalog.api.serviceoffering;

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
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "${openapi.serviceOffering.tag}")
@AllowedRoles(Role.ADMIN)
@RequestMapping("/admin/services")
interface IServiceOfferingController {

    @Operation(summary = "${openapi.serviceOffering.create}")
    @ApiResponse(responseCode = "201")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    ServiceOfferingResponse create(@Valid @RequestBody ServiceOfferingRequest request);

    @Operation(summary = "${openapi.serviceOffering.list}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @GetMapping
    PageResponse<ServiceOfferingResponse> list(
            @Parameter(description = "${openapi.serviceOffering.nameFilter}", example = "óleo")
                    @RequestParam(required = false) @Size(max = 150, message = "{validation.maxLength}") @Nullable
                    String name,
            @Parameter(description = "${openapi.serviceOffering.activeFilter}", example = "true")
                    @RequestParam(required = false) @Nullable Boolean active,
            @Parameter(description = "${openapi.page.page}") @RequestParam(defaultValue = "0") @PositiveOrZero
                    int page,
            @Parameter(description = "${openapi.page.size}") @RequestParam(defaultValue = "20") @Positive @Max(100)
                    int size);

    @Operation(summary = "${openapi.serviceOffering.findById}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @GetMapping("/{id}")
    ServiceOfferingResponse findById(
            @Parameter(description = "${openapi.serviceOffering.id}", example = "1") @PathVariable Long id);

    @Operation(
            summary = "${openapi.serviceOffering.update}",
            description = "${openapi.serviceOffering.updateDescription}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @PutMapping("/{id}")
    ServiceOfferingResponse update(
            @Parameter(description = "${openapi.serviceOffering.id}", example = "1") @PathVariable Long id,
            @Valid @RequestBody ServiceOfferingRequest request);

    @Operation(summary = "${openapi.serviceOffering.activate}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @PostMapping("/{id}/activate")
    ServiceOfferingResponse activate(
            @Parameter(description = "${openapi.serviceOffering.id}", example = "1") @PathVariable Long id);

    @Operation(
            summary = "${openapi.serviceOffering.deactivate}",
            description = "${openapi.serviceOffering.deactivateDescription}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @PostMapping("/{id}/deactivate")
    ServiceOfferingResponse deactivate(
            @Parameter(description = "${openapi.serviceOffering.id}", example = "1") @PathVariable Long id);
}
