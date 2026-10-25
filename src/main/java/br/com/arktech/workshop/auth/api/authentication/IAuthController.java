package br.com.arktech.workshop.auth.api.authentication;

import br.com.arktech.workshop.shared.api.security.Authenticated;
import br.com.arktech.workshop.shared.api.security.PublicEndpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "${openapi.auth.tag}")
@RequestMapping("/auth")
interface IAuthController {

    @PublicEndpoint
    @Operation(summary = "${openapi.auth.login}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "401", ref = "InvalidCredentials")
    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request);

    @Authenticated
    @Operation(summary = "${openapi.auth.changePassword}")
    @ApiResponse(responseCode = "204")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/password")
    void changePassword(@Valid @RequestBody ChangePasswordRequest request);
}
