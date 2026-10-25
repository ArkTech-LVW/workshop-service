package br.com.arktech.workshop.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("local-admin")
public record LocalAdminProperties(String name, String email, String password) {

    @Override
    public String toString() {
        return "LocalAdminProperties[name=" + name + ", email=" + email + ", password=****]";
    }
}
