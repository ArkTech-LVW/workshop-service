package br.com.arktech.workshop.auth.api.user;

import br.com.arktech.workshop.shared.domain.security.Role;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

record CreateUserRequest(
        @Schema(description = "${openapi.user.name}", example = "Ana Souza")
                @NotBlank @Size(max = 150, message = "{validation.maxLength}") String name,
        @Schema(description = "${openapi.user.email}", example = "ana@workshop.com")
                @NotBlank @Email @Size(max = 254, message = "{validation.maxLength}") String email,
        @Schema(description = "${openapi.user.initialPassword}", example = "senha1234")
                @NotBlank @Size(min = 8, max = 128) String password,
        @Schema(description = "${openapi.user.roles}") @NotEmpty Set<@NotNull Role> roles) {

    @Override
    public String toString() {
        return "CreateUserRequest[name=" + name + ", email=" + email + ", password=****, roles=" + roles + "]";
    }
}
