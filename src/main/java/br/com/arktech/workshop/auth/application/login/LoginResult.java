package br.com.arktech.workshop.auth.application.login;

import java.time.Duration;

public record LoginResult(String accessToken, Duration expiresIn) {
}
