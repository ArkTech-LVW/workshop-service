package br.com.arktech.workshop.auth.api.user;

import br.com.arktech.workshop.auth.application.user.UserResult;
import br.com.arktech.workshop.shared.domain.security.Role;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

record UserResponse(
        @Schema(description = "${openapi.user.id}", example = "1") Long id,
        @Schema(description = "${openapi.user.name}", example = "Ana Souza") String name,
        @Schema(description = "${openapi.user.email}", example = "ana@workshop.com") String email,
        @Schema(description = "${openapi.user.active}") boolean active,
        @Schema(description = "${openapi.user.roles}") Set<Role> roles) {

    static UserResponse from(UserResult result) {
        return new UserResponse(result.id(), result.name(), result.email(), result.active(), result.roles());
    }
}
