package br.com.arktech.workshop.auth.domain.port;

public interface PasswordHasher {

    String hash(String password);

    boolean matches(String password, String hash);
}
