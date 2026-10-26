package br.com.arktech.workshop.shared.api.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.shared.application.i18n.MessageResolver;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

@IntegrationTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class OpenApiDocsTest {

    private static final String TEST_OPERATION = "$.paths['/test/docs'].get";

    @LocalServerPort
    private int port;

    @Autowired
    private MessageResolver messageResolver;

    private RestClient restClient;

    private String specJson;

    private DocumentContext spec;

    @BeforeEach
    void setUp() {
        restClient = RestClient.create("http://localhost:" + port);
        ResponseEntity<String> response =
                restClient.get().uri("/docs/openapi.json").retrieve().toEntity(String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(MediaType.APPLICATION_JSON.isCompatibleWith(response.getHeaders().getContentType())).isTrue();
        specJson = Objects.requireNonNull(response.getBody());
        spec = JsonPath.parse(specJson);
    }

    @Test
    void shouldServeSwaggerUiAtDocs() {
        ResponseEntity<Void> redirect = restClient.get().uri("/docs").retrieve().toBodilessEntity();
        assertThat(redirect.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        URI location = Objects.requireNonNull(redirect.getHeaders().getLocation());

        ResponseEntity<String> response = restClient.get().uri(location.toString()).retrieve().toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(MediaType.TEXT_HTML.isCompatibleWith(response.getHeaders().getContentType())).isTrue();
        assertThat(response.getBody()).contains("swagger-ui");
    }

    @Test
    void shouldDescribeApiWithErrorsSection() {
        assertThat(spec.read("$.info.title", String.class)).isEqualTo("Workshop Service API");
        assertThat(spec.read("$.info.version", String.class)).isNotBlank();
        assertThat(spec.read("$.info.description", String.class))
                .contains("## Erros")
                .contains("request.invalidFields")
                .contains("server.unexpectedError");
    }

    @Test
    void shouldExposeApiErrorSchemas() {
        Map<String, Object> schemas = spec.read("$.components.schemas");

        assertThat(schemas).containsKeys("ApiError", "ApiFieldError");
    }

    @Test
    void shouldRequireBearerJwtGlobally() {
        assertThat(spec.read("$.components.securitySchemes.bearerAuth", Map.class))
                .containsEntry("type", "http")
                .containsEntry("scheme", "bearer")
                .containsEntry("bearerFormat", "JWT");
        assertThat(spec.read("$.security[*].bearerAuth", List.class)).hasSize(1);
    }

    @Test
    void shouldMarkPublicEndpointAsPublic() {
        String publicOperation = "$.paths['/test/docs/public'].get";

        assertThat(spec.read(publicOperation + ".security", List.class)).isEmpty();
        assertThat(spec.read(publicOperation + ".responses", Map.class)).containsOnlyKeys("200", "429", "500");
        assertThat(spec.read(TEST_OPERATION, Map.class)).doesNotContainKey("security");
    }

    @Test
    void shouldAdd401ToAuthenticatedEndpoint() {
        assertThat(spec.read(TEST_OPERATION + ".responses", Map.class)).containsOnlyKeys("200", "401", "429", "500");
        assertThat(spec.read(TEST_OPERATION + ".responses.401.$ref", String.class))
                .isEqualTo("#/components/responses/Unauthenticated");
    }

    @Test
    void shouldAdd401And403ToRoleEndpoint() {
        String adminOperation = "$.paths['/test/docs/admin'].get";

        assertThat(spec.read(adminOperation + ".responses", Map.class)).containsOnlyKeys("200", "401", "403", "429", "500");
        assertThat(spec.read(adminOperation + ".responses.403.$ref", String.class))
                .isEqualTo("#/components/responses/AccessDenied");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "BadRequest",
                "NotFound",
                "Conflict",
                "BusinessRule",
                "InternalError",
                "InvalidCredentials",
                "Unauthenticated",
                "AccessDenied",
                "TooManyRequests"
            })
    void shouldDefineReusableErrorResponseWithExample(String name) {
        String content = "$.components.responses." + name + ".content['application/json']";

        assertThat(spec.read(content + ".schema.$ref", String.class)).isEqualTo("#/components/schemas/ApiError");
        assertThat(spec.read(content + ".example", Map.class))
                .containsKeys("code", "message")
                .doesNotContainValue(null);
        assertThat(spec.read(content + ".example.code", String.class)).isNotBlank();
        assertThat(spec.read(content + ".example.message", String.class)).isNotBlank();
    }

    @Test
    void shouldAddTooManyRequestsWithRetryAfterToEveryOperation() {
        String tooManyRequests = "$.components.responses.TooManyRequests";

        assertThat(spec.read(TEST_OPERATION + ".responses.429.$ref", String.class))
                .isEqualTo("#/components/responses/TooManyRequests");
        assertThat(spec.read("$.paths['/auth/login'].post.responses.429.$ref", String.class))
                .isEqualTo("#/components/responses/TooManyRequests");
        assertThat(spec.read(tooManyRequests + ".content['application/json'].example.code", String.class))
                .isEqualTo("request.tooManyRequests");
        assertThat(spec.read(tooManyRequests + ".headers.Retry-After.schema.type", String.class))
                .isEqualTo("integer");
    }

    @Test
    void shouldTakeBadRequestExampleFromMessageCatalog() {
        String example = "$.components.responses.BadRequest.content['application/json'].example";

        assertThat(spec.read(example + ".message", String.class))
                .isEqualTo(messageResolver.get("request.invalidFields"));
        assertThat(spec.read(example + ".details[*].field", List.class)).containsExactly("name", "items[0].quantity");
    }

    @Test
    void shouldAddInternalErrorToEveryOperation() {
        assertThat(spec.read(TEST_OPERATION + ".responses.500.$ref", String.class))
                .isEqualTo("#/components/responses/InternalError");
    }

    @Test
    void shouldResolveTextsFromOpenApiProperties() {
        assertThat(spec.read(TEST_OPERATION + ".summary", String.class)).isEqualTo("Erro inesperado");
        assertThat(spec.read("$.components.schemas.DocsTestResponse.properties.value.description", String.class))
                .isEqualTo("Recurso não encontrado");
        assertThat(spec.read("$.components.responses.NotFound.description", String.class))
                .isEqualTo("Recurso não encontrado");
        assertThat(specJson).as("unresolved ${...} placeholder").doesNotContain("${");
    }

    @Test
    void shouldDocumentLoginAsPublicWithInvalidCredentials() {
        String login = "$.paths['/auth/login'].post";

        assertThat(spec.read(login + ".security", List.class)).isEmpty();
        assertThat(spec.read(login + ".tags", List.class)).containsExactly("Autenticação");
        assertThat(spec.read(login + ".responses.401.$ref", String.class))
                .isEqualTo("#/components/responses/InvalidCredentials");
        assertThat(spec.read(login + ".responses.400.$ref", String.class))
                .isEqualTo("#/components/responses/BadRequest");
    }

    @Test
    void shouldDocumentUserManagementAsAdminOnly() {
        String create = "$.paths['/admin/users'].post";
        String deactivate = "$.paths['/admin/users/{id}/deactivate'].post";

        assertThat(spec.read(create + ".tags", List.class)).containsExactly("Usuários");
        assertThat(spec.read(create + ".responses", Map.class))
                .containsOnlyKeys("201", "400", "401", "403", "409", "429", "500");
        assertThat(spec.read(deactivate + ".responses", Map.class))
                .containsOnlyKeys("200", "401", "403", "404", "422", "429", "500");
        assertThat(spec.read("$.paths['/admin/users'].get.parameters[*].name", List.class))
                .containsExactly("page", "size");
    }

    @Test
    void shouldDocumentPasswordChangeForAnyLoggedUser() {
        String changePassword = "$.paths['/auth/password'].put";

        assertThat(spec.read(changePassword + ".responses", Map.class))
                .containsOnlyKeys("204", "400", "401", "422", "429", "500");
    }

    @Test
    void shouldDocumentCustomerManagementAsAdminOnly() {
        String customers = "$.paths['/admin/customers']";
        String customer = "$.paths['/admin/customers/{id}']";

        assertThat(spec.read(customers + ".post.tags", List.class)).containsExactly("Clientes");
        assertThat(spec.read(customers + ".post.responses", Map.class))
                .containsOnlyKeys("201", "400", "401", "403", "409", "429", "500");
        assertThat(spec.read(customers + ".get.parameters[*].name", List.class))
                .containsExactly("document", "page", "size");
        assertThat(spec.read(customer + ".get.responses", Map.class))
                .containsOnlyKeys("200", "401", "403", "404", "429", "500");
        assertThat(spec.read(customer + ".put.responses", Map.class))
                .containsOnlyKeys("200", "400", "401", "403", "404", "409", "429", "500");
        assertThat(spec.read(customer + ".delete.responses", Map.class))
                .containsOnlyKeys("204", "401", "403", "404", "409", "429", "500");
    }

    @Test
    void shouldDocumentVehicleManagementAsAdminOnly() {
        String vehicles = "$.paths['/admin/vehicles']";
        String vehicle = "$.paths['/admin/vehicles/{id}']";

        assertThat(spec.read(vehicles + ".post.tags", List.class)).containsExactly("Veículos");
        assertThat(spec.read(vehicles + ".post.responses", Map.class))
                .containsOnlyKeys("201", "400", "401", "403", "404", "409", "429", "500");
        assertThat(spec.read(vehicle + ".get.responses", Map.class))
                .containsOnlyKeys("200", "401", "403", "404", "429", "500");
    }

    @Test
    void shouldHideErrorController() {
        Map<String, Object> paths = spec.read("$.paths");

        assertThat(paths).doesNotContainKey("/error");
    }
}
