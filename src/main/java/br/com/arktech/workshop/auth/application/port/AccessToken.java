package br.com.arktech.workshop.auth.application.port;

import java.time.Duration;

public record AccessToken(String value, Duration expiresIn) {
}
