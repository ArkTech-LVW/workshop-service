package br.com.arktech.workshop.shared.infrastructure.security;

import br.com.arktech.workshop.shared.application.security.ClientAddress;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
class RequestClientAddress implements ClientAddress {

    @Override
    public String ip() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            throw new IllegalStateException("No HTTP request in the current thread");
        }

        return attributes.getRequest().getRemoteAddr();
    }
}
