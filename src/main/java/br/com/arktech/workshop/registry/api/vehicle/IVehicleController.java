package br.com.arktech.workshop.registry.api.vehicle;

import br.com.arktech.workshop.shared.api.pagination.PageResponse;
import br.com.arktech.workshop.shared.api.security.AllowedRoles;
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

@Tag(name = "${openapi.vehicle.tag}")
@AllowedRoles({Role.ADMIN, Role.ATTENDANT})
@RequestMapping("/admin/vehicles")
interface IVehicleController {

    @Operation(summary = "${openapi.vehicle.create}")
    @ApiResponse(responseCode = "201")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "409", ref = "Conflict")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    VehicleResponse create(@Valid @RequestBody VehicleRequest request);

    @Operation(summary = "${openapi.vehicle.list}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @GetMapping
    PageResponse<VehicleResponse> list(
            @Parameter(description = "${openapi.vehicle.licensePlateFilter}", example = "ABC-1D23")
                    @RequestParam(required = false) @LicensePlate @Nullable String licensePlate,
            @Parameter(description = "${openapi.page.page}") @RequestParam(defaultValue = "0") @PositiveOrZero
                    int page,
            @Parameter(description = "${openapi.page.size}") @RequestParam(defaultValue = "20") @Positive @Max(100)
                    int size);

    @Operation(summary = "${openapi.vehicle.findById}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @GetMapping("/{id}")
    VehicleResponse findById(
            @Parameter(description = "${openapi.vehicle.id}", example = "1") @PathVariable Long id);

    @Operation(summary = "${openapi.vehicle.update}", description = "${openapi.vehicle.updateDescription}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "409", ref = "Conflict")
    @PutMapping("/{id}")
    VehicleResponse update(
            @Parameter(description = "${openapi.vehicle.id}", example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateVehicleRequest request);

    @Operation(summary = "${openapi.vehicle.delete}", description = "${openapi.vehicle.deleteDescription}")
    @ApiResponse(responseCode = "204")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "409", ref = "Conflict")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    void delete(@Parameter(description = "${openapi.vehicle.id}", example = "1") @PathVariable Long id);
}
