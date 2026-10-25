package br.com.arktech.workshop.auth.api.authentication;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record ChangePasswordRequest(
        @Schema(description = "${openapi.auth.currentPassword}") @NotBlank String currentPassword,
        @Schema(description = "${openapi.auth.newPassword}") @NotBlank @Size(min = 8, max = 128) String newPassword) {

    @Override
    public String toString() {
        return "ChangePasswordRequest[currentPassword=****, newPassword=****]";
    }
}
