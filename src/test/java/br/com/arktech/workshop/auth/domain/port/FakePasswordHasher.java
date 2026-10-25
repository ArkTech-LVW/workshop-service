package br.com.arktech.workshop.auth.domain.port;

public class FakePasswordHasher implements PasswordHasher {

    private int matchCount;

    @Override
    public String hash(String password) {
        return "hashed:" + password;
    }

    @Override
    public boolean matches(String password, String hash) {
        matchCount++;
        return hash.equals(hash(password));
    }

    public int matchCount() {
        return matchCount;
    }
}
