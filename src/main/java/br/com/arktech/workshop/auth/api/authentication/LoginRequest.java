package br.com.arktech.workshop.auth.api.authentication;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

record LoginRequest(
        @Schema(description = "${openapi.auth.email}", example = "admin@workshop.local") @NotBlank @Email String email,
        @Schema(description = "${openapi.auth.password}") @NotBlank String password) {

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=****]";
    }
}
