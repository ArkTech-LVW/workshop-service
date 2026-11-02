package br.com.arktech.workshop.auth.api.authentication;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record ChangePasswordRequest(
        @Schema(description = "${openapi.auth.currentPassword}", example = "admin123") @NotBlank String currentPassword,
        @Schema(description = "${openapi.auth.newPassword}", example = "novaSenha123")
                @NotBlank @Size(min = 8, max = 128) String newPassword) {

    @Override
    public String toString() {
        return "ChangePasswordRequest[currentPassword=****, newPassword=****]";
    }
}
