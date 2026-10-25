package br.com.arktech.workshop.shared.api.security;

import br.com.arktech.workshop.shared.domain.security.Role;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AllowedRoles {

    Role[] value();
}
