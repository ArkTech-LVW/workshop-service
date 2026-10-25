package br.com.arktech.workshop.registry.api.customer;

import br.com.arktech.workshop.shared.api.pagination.PageResponse;
import br.com.arktech.workshop.shared.api.security.AllowedRoles;
import br.com.arktech.workshop.shared.api.validation.CPFOrCNPJ;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "${openapi.customer.tag}")
@AllowedRoles(Role.ADMIN)
@RequestMapping("/admin/customers")
interface ICustomerController {

    @Operation(summary = "${openapi.customer.create}")
    @ApiResponse(responseCode = "201")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "409", ref = "Conflict")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    CustomerResponse create(@Valid @RequestBody CustomerRequest request);

    @Operation(summary = "${openapi.customer.list}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @GetMapping
    PageResponse<CustomerResponse> list(
            @Parameter(description = "${openapi.customer.documentFilter}", example = "529.982.247-25")
                    @RequestParam(required = false) @CPFOrCNPJ @Nullable String document,
            @Parameter(description = "${openapi.page.page}") @RequestParam(defaultValue = "0") @PositiveOrZero
                    int page,
            @Parameter(description = "${openapi.page.size}") @RequestParam(defaultValue = "20") @Positive @Max(100)
                    int size);

    @Operation(summary = "${openapi.customer.findById}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @GetMapping("/{id}")
    CustomerResponse findById(
            @Parameter(description = "${openapi.customer.id}", example = "1") @PathVariable Long id);

    @Operation(summary = "${openapi.customer.update}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "409", ref = "Conflict")
    @PutMapping("/{id}")
    CustomerResponse update(
            @Parameter(description = "${openapi.customer.id}", example = "1") @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request);

    @Operation(summary = "${openapi.customer.delete}", description = "${openapi.customer.deleteDescription}")
    @ApiResponse(responseCode = "204")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "409", ref = "Conflict")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    void delete(@Parameter(description = "${openapi.customer.id}", example = "1") @PathVariable Long id);
}
