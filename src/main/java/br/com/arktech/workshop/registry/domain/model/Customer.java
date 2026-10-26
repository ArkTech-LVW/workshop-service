package br.com.arktech.workshop.registry.domain.model;

import br.com.arktech.workshop.shared.domain.BaseEntity;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.document.PersonType;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.phone.Phone;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "customer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer extends BaseEntity {

    private String name;

    private CPFCNPJ document;

    @Enumerated(EnumType.STRING)
    private PersonType personType;

    private Email email;

    private @Nullable Phone phone;

    public Customer(String name, CPFCNPJ document, Email email, @Nullable Phone phone) {
        this.name = name;
        this.document = document;
        this.personType = document.personType();
        this.email = email;
        this.phone = phone;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void changeDocument(CPFCNPJ document) {
        this.document = document;
        this.personType = document.personType();
    }

    public void changeContact(Email email, @Nullable Phone phone) {
        this.email = email;
        this.phone = phone;
    }
}
