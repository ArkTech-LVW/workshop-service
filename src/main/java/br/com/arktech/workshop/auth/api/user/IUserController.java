package br.com.arktech.workshop.auth.api.user;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "${openapi.user.tag}")
@AllowedRoles(Role.ADMIN)
@RequestMapping("/admin/users")
interface IUserController {

    @Operation(summary = "${openapi.user.create}")
    @ApiResponse(responseCode = "201")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "409", ref = "Conflict")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    UserResponse create(@Valid @RequestBody CreateUserRequest request);

    @Operation(summary = "${openapi.user.list}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @GetMapping
    PageResponse<UserResponse> list(
            @Parameter(description = "${openapi.page.page}") @RequestParam(defaultValue = "0") @PositiveOrZero
                    int page,
            @Parameter(description = "${openapi.page.size}") @RequestParam(defaultValue = "20") @Positive @Max(100)
                    int size);

    @Operation(summary = "${openapi.user.findById}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @GetMapping("/{id}")
    UserResponse findById(@Parameter(description = "${openapi.user.id}", example = "1") @PathVariable Long id);

    @Operation(summary = "${openapi.user.update}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @PutMapping("/{id}")
    UserResponse update(
            @Parameter(description = "${openapi.user.id}", example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request);

    @Operation(summary = "${openapi.user.activate}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @PostMapping("/{id}/activate")
    UserResponse activate(@Parameter(description = "${openapi.user.id}", example = "1") @PathVariable Long id);

    @Operation(summary = "${openapi.user.deactivate}", description = "${openapi.user.deactivateDescription}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @PostMapping("/{id}/deactivate")
    UserResponse deactivate(@Parameter(description = "${openapi.user.id}", example = "1") @PathVariable Long id);
}
