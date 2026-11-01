package br.com.arktech.workshop.shared.api.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.shared.application.i18n.MessageResolver;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    void shouldAuthorizeSwaggerUiWithTokenFromLogin() {
        ResponseEntity<String> response = restClient
                .get()
                .uri("/swagger-ui/swagger-initializer.js")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("window.ui = SwaggerUIBundle({")
                .contains("responseInterceptor: (response) => {")
                .contains("path.endsWith('/auth/login')")
                .contains("window.ui.preauthorizeApiKey('bearerAuth', response.body.accessToken)")
                .contains("\"persistAuthorization\" : true");
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
    void shouldDocumentCustomerManagement() {
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
    void shouldDocumentVehicleManagement() {
        String vehicles = "$.paths['/admin/vehicles']";
        String vehicle = "$.paths['/admin/vehicles/{id}']";
        String customerVehicles = "$.paths['/admin/customers/{customerId}/vehicles']";

        assertThat(spec.read(vehicles + ".post.tags", List.class)).containsExactly("Veículos");
        assertThat(spec.read(vehicles + ".post.responses", Map.class))
                .containsOnlyKeys("201", "400", "401", "403", "404", "409", "429", "500");
        assertThat(spec.read(vehicles + ".get.parameters[*].name", List.class))
                .containsExactly("licensePlate", "page", "size");
        assertThat(spec.read(vehicle + ".get.responses", Map.class))
                .containsOnlyKeys("200", "401", "403", "404", "429", "500");
        assertThat(spec.read(vehicle + ".put.responses", Map.class))
                .containsOnlyKeys("200", "400", "401", "403", "404", "409", "429", "500");
        assertThat(spec.read(vehicle + ".delete.responses", Map.class))
                .containsOnlyKeys("204", "401", "403", "404", "409", "429", "500");
        assertThat(spec.read(customerVehicles + ".get.tags", List.class)).containsExactly("Veículos");
        assertThat(spec.read(customerVehicles + ".get.parameters[*].name", List.class))
                .containsExactly("customerId", "page", "size");
        assertThat(spec.read(customerVehicles + ".get.responses", Map.class))
                .containsOnlyKeys("200", "400", "401", "403", "404", "429", "500");
    }

    @Test
    void shouldDocumentCustomerServiceOrderRoutesAsPublicInTheirOwnTag() {
        String serviceOrder = "$.paths['/customer/serviceOrders/{publicId}']";
        String approve = "$.paths['/customer/serviceOrders/{publicId}/approve']";
        String reject = "$.paths['/customer/serviceOrders/{publicId}/reject']";

        for (String operation : List.of(serviceOrder + ".get", approve + ".post", reject + ".post")) {
            assertThat(spec.read(operation + ".tags", List.class)).containsExactly("Acompanhamento da OS pelo cliente");
            assertThat(spec.read(operation + ".security", List.class)).isEmpty();
            assertThat(spec.read(operation + ".parameters[*].name", List.class))
                    .containsExactly("publicId", "X-Customer-Document");
            assertThat(spec.read(operation + ".parameters[1].in", String.class)).isEqualTo("header");
            assertThat(spec.read(operation + ".parameters[1].example", String.class)).isEqualTo("529.982.247-25");
        }
        assertThat(spec.read(serviceOrder + ".get.responses", Map.class))
                .containsOnlyKeys("200", "400", "404", "429", "500");
        assertThat(spec.read(approve + ".post.responses", Map.class))
                .containsOnlyKeys("204", "400", "404", "409", "422", "429", "500");
        assertThat(spec.read(reject + ".post.responses", Map.class))
                .containsOnlyKeys("204", "400", "404", "409", "422", "429", "500");
    }

    @Test
    void shouldDescribeCompleteFlow() {
        assertThat(spec.read("$.info.description", String.class))
                .contains("## Fluxo completo")
                .contains("POST /auth/login")
                .contains("stockMovements")
                .contains("X-Customer-Document")
                .contains("/services/{itemId}/start")
                .contains("/deliver");
    }

    @Test
    void shouldListTagsInFlowOrderWithDescriptions() {
        List<String> flowOrder = List.of(
                "Autenticação",
                "Usuários",
                "Clientes",
                "Veículos",
                "Serviços",
                "Peças e insumos",
                "Movimentações de estoque",
                "Pedidos de ajuste de estoque",
                "Ordens de serviço",
                "Acompanhamento da OS pelo cliente",
                "Métricas");
        List<String> tagNames = spec.read("$.tags[*].name");

        assertThat(tagNames.subList(0, flowOrder.size())).containsExactlyElementsOf(flowOrder);
        assertThat(spec.read("$.tags[0:" + flowOrder.size() + "].description", List.class))
                .hasSize(flowOrder.size())
                .doesNotContainNull();
        assertThat(businessOperations().stream()
                        .flatMap(operation -> this.<String>listOf(operation.body().get("tags")).stream())
                        .distinct())
                .allMatch(flowOrder::contains);
    }

    @Test
    void shouldAppendAllowedRolesToDescription() {
        assertThat(spec.read("$.paths['/test/docs/admin'].get.description", String.class))
                .isEqualTo("**Papéis:** ADMIN");
        assertThat(spec.read(TEST_OPERATION + ".description", String.class))
                .isEqualTo("**Papéis:** qualquer usuário logado");
        assertThat(spec.read("$.paths['/test/docs/public'].get", Map.class)).doesNotContainKey("description");
        assertThat(spec.read("$.paths['/admin/serviceOrders/{id}/sendQuote'].post.description", String.class))
                .startsWith("De IN_DIAGNOSIS")
                .endsWith("\n\n**Papéis:** ADMIN, ATTENDANT, MECHANIC");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            /admin/customers           | post   | ADMIN, ATTENDANT
            /admin/customers/{id}      | delete | ADMIN
            /admin/serviceOrders       | get    | ADMIN, ATTENDANT, MECHANIC
            /admin/serviceOrders/{id}  | get    | ADMIN, ATTENDANT, MECHANIC
            /admin/serviceOrders       | post   | ADMIN, ATTENDANT
            /admin/services            | get    | ADMIN, ATTENDANT, MECHANIC
            /admin/services/{id}       | get    | ADMIN, ATTENDANT, MECHANIC
            /admin/services            | post   | ADMIN
            /admin/parts               | get    | ADMIN, ATTENDANT, MECHANIC
            /admin/parts/lowStock      | get    | ADMIN, ATTENDANT, MECHANIC
            /admin/parts/{id}          | get    | ADMIN, ATTENDANT, MECHANIC
            /admin/parts               | post   | ADMIN
            /admin/vehicles            | get    | ADMIN, ATTENDANT, MECHANIC
            /admin/vehicles/{id}       | get    | ADMIN, ATTENDANT, MECHANIC
            /admin/vehicles            | post   | ADMIN, ATTENDANT
            """)
    void shouldDocumentTheRolesOfEachReviewedRoute(String path, String method, String roles) {
        assertThat(spec.read("$.paths['%s'].%s.description".formatted(path, method), String.class))
                .endsWith("**Papéis:** " + roles);
    }

    @Test
    void shouldOfferInitialAndSupplementaryExamplesToSendQuote() {
        String examples =
                "$.paths['/admin/serviceOrders/{id}/sendQuote'].post.requestBody.content['application/json'].examples";

        assertThat(spec.read(examples, Map.class)).containsOnlyKeys("initial", "supplementary");
        assertThat(spec.read(examples + ".initial.value", Map.class)).isEmpty();
        assertThat(spec.read(examples + ".supplementary.value.canProceedWithoutChanges", Boolean.class))
                .isTrue();
    }

    @Test
    void shouldGiveExampleToEveryParameterAndBodyField() {
        List<String> missing = new ArrayList<>();

        for (NamedOperation operation : businessOperations()) {
            for (Map<String, Object> parameter : this.<Map<String, Object>>listOf(operation.body().get("parameters"))) {
                Map<String, Object> schema = mapOf(parameter.get("schema"));
                if (!parameter.containsKey("example") && !hasExample(schema)) {
                    missing.add(operation.name() + " parameter " + parameter.get("name"));
                }
            }
            Map<String, Object> requestBody = mapOf(operation.body().get("requestBody"));
            schemasOf(requestBody)
                    .forEach(schema -> collectMissingExamples(schema, operation.name() + " body", new HashSet<>(), missing));
            mapOf(operation.body().get("responses")).forEach((status, response) -> {
                if (status.startsWith("2")) {
                    schemasOf(mapOf(response)).forEach(schema -> collectMissingExamples(
                            schema, operation.name() + " response " + status, new HashSet<>(), missing));
                }
            });
        }

        assertThat(missing).as("fields without example").isEmpty();
    }

    @Test
    void shouldHideErrorController() {
        Map<String, Object> paths = spec.read("$.paths");

        assertThat(paths).doesNotContainKey("/error");
    }

    private List<NamedOperation> businessOperations() {
        Map<String, Object> paths = spec.read("$.paths");
        List<NamedOperation> operations = new ArrayList<>();
        paths.forEach((path, item) -> {
            if (!path.startsWith("/test/")) {
                mapOf(item).forEach((method, operation) -> operations.add(
                        new NamedOperation(method.toUpperCase(Locale.ROOT) + " " + path, mapOf(operation))));
            }
        });
        return operations;
    }

    private static List<Map<String, Object>> schemasOf(Map<String, Object> bodyOrResponse) {
        return mapOf(bodyOrResponse.get("content")).values().stream()
                .map(mediaType -> mapOf(mapOf(mediaType).get("schema")))
                .toList();
    }

    private void collectMissingExamples(
            Map<String, Object> schema, String location, Set<String> visited, List<String> missing) {
        if (schema.get("$ref") instanceof String ref && !visited.add(ref)) {
            return;
        }
        Map<String, Object> resolved = resolve(schema);
        if (isArray(resolved)) {
            collectMissingExamples(mapOf(resolved.get("items")), location + "[]", new HashSet<>(visited), missing);
            return;
        }
        mapOf(resolved.get("properties")).forEach((name, value) -> {
            Map<String, Object> property = mapOf(value);
            Map<String, Object> target = resolve(property);
            String field = location + "." + name;
            if (target.containsKey("properties")) {
                collectMissingExamples(property, field, new HashSet<>(visited), missing);
            } else if (isArray(target)) {
                Map<String, Object> items = mapOf(target.get("items"));
                if (resolve(items).containsKey("properties")) {
                    collectMissingExamples(items, field + "[]", new HashSet<>(visited), missing);
                } else if (!hasExample(property) && !hasExample(target) && !hasExample(resolve(items))) {
                    missing.add(field);
                }
            } else if (!hasExample(property) && !hasExample(target)) {
                missing.add(field);
            }
        });
    }

    private Map<String, Object> resolve(Map<String, Object> schema) {
        Map<String, Object> resolved = schema;
        while (resolved.get("$ref") instanceof String ref) {
            String name = ref.substring(ref.lastIndexOf('/') + 1);
            resolved = mapOf(spec.read("$.components.schemas['" + name + "']"));
        }
        return resolved;
    }

    private static boolean isArray(Map<String, Object> schema) {
        Object type = schema.get("type");
        return "array".equals(type) || (type instanceof List<?> types && types.contains("array"));
    }

    private static boolean hasExample(Map<String, Object> schema) {
        return Stream.of("example", "examples", "enum", "default", "const").anyMatch(schema::containsKey);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapOf(@Nullable Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> listOf(@Nullable Object value) {
        return value instanceof List<?> list ? (List<T>) list : List.of();
    }

    private record NamedOperation(String name, Map<String, Object> body) {}
}
