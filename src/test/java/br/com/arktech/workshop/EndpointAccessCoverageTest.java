package br.com.arktech.workshop;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arktech.workshop.shared.api.security.EndpointAccess;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

// Every endpoint must declare who can access it (@PublicEndpoint, @Authenticated or @AllowedRoles).
// An undeclared endpoint is closed to everyone at runtime; this test makes the build fail instead.
@IntegrationTest
class EndpointAccessCoverageTest {

    // Test-only endpoint that proves undeclared endpoints are closed (EndpointAuthorizationTest).
    private static final String KNOWN_UNDECLARED = "[GET] /test/security/undeclared";

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void shouldDeclareAccessOnEveryEndpoint() {
        List<String> undeclared = applicationEndpoints().entrySet().stream()
                .filter(endpoint -> EndpointAccess.of(endpoint.getValue()) instanceof EndpointAccess.Undeclared)
                .map(endpoint -> describe(endpoint.getKey()))
                .sorted()
                .toList();

        assertThat(undeclared)
                .as("endpoints without @PublicEndpoint, @Authenticated or @AllowedRoles")
                .containsExactly(KNOWN_UNDECLARED);
    }

    private Map<RequestMappingInfo, HandlerMethod> applicationEndpoints() {
        return handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(endpoint -> endpoint.getValue().getBeanType().getPackageName()
                        .startsWith(WorkshopApplication.class.getPackageName()))
                // /error is reached on the ERROR dispatch, which the SecurityConfig lets through.
                .filter(endpoint -> !ErrorController.class.isAssignableFrom(endpoint.getValue().getBeanType()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static String describe(RequestMappingInfo info) {
        return info.getMethodsCondition().getMethods() + " " + String.join(", ", info.getPatternValues());
    }
}
