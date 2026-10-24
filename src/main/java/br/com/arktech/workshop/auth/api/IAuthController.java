package br.com.arktech.workshop.auth.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "${openapi.auth.tag}")
@RequestMapping("/auth")
interface IAuthController {

    @SecurityRequirements
    @Operation(summary = "${openapi.auth.login}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "401", ref = "InvalidCredentials")
    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request);
}
