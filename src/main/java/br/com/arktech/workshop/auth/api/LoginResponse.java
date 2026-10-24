package br.com.arktech.workshop.auth.api;

import br.com.arktech.workshop.auth.application.login.LoginResult;

import io.swagger.v3.oas.annotations.media.Schema;

record LoginResponse(
        @Schema(description = "${openapi.auth.accessToken}") String accessToken,
        @Schema(description = "${openapi.auth.tokenType}", example = "Bearer") String tokenType,
        @Schema(description = "${openapi.auth.expiresIn}", example = "3600") long expiresIn) {

    private static final String BEARER = "Bearer";

    static LoginResponse from(LoginResult result) {
        return new LoginResponse(result.accessToken(), BEARER, result.expiresIn().toSeconds());
    }
}
