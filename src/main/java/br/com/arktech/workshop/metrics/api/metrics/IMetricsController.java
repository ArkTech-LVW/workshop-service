package br.com.arktech.workshop.metrics.api.metrics;

import br.com.arktech.workshop.shared.api.security.AllowedRoles;
import br.com.arktech.workshop.shared.domain.security.Role;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "${openapi.metrics.tag}")
@AllowedRoles(Role.ADMIN)
@RequestMapping("/admin/metrics")
interface IMetricsController {

    @Operation(
            summary = "${openapi.metrics.averageExecutionTime}",
            description = "${openapi.metrics.averageExecutionTimeDescription}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @GetMapping("/averageExecutionTime")
    AverageExecutionTimeResponse averageExecutionTime(
            @Parameter(description = "${openapi.metrics.from}", example = "2026-10-01")
                    @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @Parameter(description = "${openapi.metrics.to}", example = "2026-10-31")
                    @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to);
}
