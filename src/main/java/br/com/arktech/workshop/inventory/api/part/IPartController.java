package br.com.arktech.workshop.inventory.api.part;

import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.shared.api.pagination.PageResponse;
import br.com.arktech.workshop.shared.api.security.AllowedRoles;
import br.com.arktech.workshop.shared.domain.security.Role;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
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

@Tag(name = "${openapi.part.tag}")
@AllowedRoles(Role.ADMIN)
@RequestMapping("/admin/parts")
interface IPartController {

    @Operation(summary = "${openapi.part.create}", description = "${openapi.part.createDescription}")
    @ApiResponse(responseCode = "201")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "409", ref = "Conflict")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    PartResponse create(@Valid @RequestBody PartRequest request);

    @Operation(summary = "${openapi.part.list}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @GetMapping
    PageResponse<PartResponse> list(
            @Parameter(description = "${openapi.part.nameFilter}", example = "óleo")
                    @RequestParam(required = false) @Nullable String name,
            @Parameter(description = "${openapi.part.skuFilter}", example = "oleo-5w30-1l")
                    @RequestParam(required = false)
                    @Pattern(regexp = PartRequest.SKU_PATTERN, message = "{sku.invalid}")
                    @Nullable String sku,
            @Parameter(description = "${openapi.part.typeFilter}", example = "SUPPLY")
                    @RequestParam(required = false) @Nullable PartType type,
            @Parameter(description = "${openapi.page.page}") @RequestParam(defaultValue = "0") @PositiveOrZero
                    int page,
            @Parameter(description = "${openapi.page.size}") @RequestParam(defaultValue = "20") @Positive @Max(100)
                    int size);

    @Operation(summary = "${openapi.part.findById}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @GetMapping("/{id}")
    PartResponse findById(@Parameter(description = "${openapi.part.id}", example = "1") @PathVariable Long id);
}
