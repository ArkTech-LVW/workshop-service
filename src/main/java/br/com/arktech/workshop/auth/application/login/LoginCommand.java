package br.com.arktech.workshop.auth.application.login;

public record LoginCommand(String email, String password) {

    @Override
    public String toString() {
        return "LoginCommand[email=" + email + ", password=****]";
    }
}
