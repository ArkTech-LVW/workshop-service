package br.com.arktech.workshop.shared.api.openapi;

import br.com.arktech.workshop.shared.api.error.ApiError;
import br.com.arktech.workshop.shared.api.error.ApiFieldError;
import br.com.arktech.workshop.shared.api.security.EndpointAccess;
import br.com.arktech.workshop.shared.application.i18n.MessageResolver;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;

@Configuration
@PropertySource(value = "classpath:openapi/openapi.properties", encoding = "UTF-8")
class OpenApiConfig {

    private static final String TITLE = "Workshop Service API";
    private static final String DEFAULT_VERSION = "dev";
    private static final String BEARER_AUTH = "bearerAuth";
    private static final String JSON = org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
    private static final String API_ERROR_REF = "#/components/schemas/ApiError";
    private static final String INTERNAL_ERROR_REF = "#/components/responses/InternalError";
    private static final String UNAUTHENTICATED_REF = "#/components/responses/Unauthenticated";
    private static final String ACCESS_DENIED_REF = "#/components/responses/AccessDenied";
    private static final String TOO_MANY_REQUESTS_REF = "#/components/responses/TooManyRequests";

    @Bean
    OpenAPI openApi(
            ObjectProvider<BuildProperties> buildProperties,
            @Value("classpath:openapi/description.md") Resource description,
            MessageResolver messageResolver,
            Environment environment)
            throws IOException {
        Components components = new Components()
                .addSecuritySchemes(
                        BEARER_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"));
        ModelConverters.getInstance(true).readAll(ApiError.class).forEach(components::addSchemas);
        addErrorResponses(components, messageResolver, environment);

        return new OpenAPI()
                .info(new Info()
                        .title(TITLE)
                        .version(versionOf(buildProperties.getIfAvailable()))
                        .description(description.getContentAsString(StandardCharsets.UTF_8)))
                .components(components)
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    @Bean
    OpenApiCustomizer globalErrorResponsesCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().stream()
                    .flatMap(path -> path.readOperations().stream())
                    .forEach(operation -> {
                        operation.getResponses().putIfAbsent("429", new ApiResponse().$ref(TOO_MANY_REQUESTS_REF));
                        operation.getResponses().putIfAbsent("500", new ApiResponse().$ref(INTERNAL_ERROR_REF));
                    });
        };
    }

    @Bean
    OperationCustomizer endpointAccessCustomizer() {
        return (operation, handlerMethod) -> {
            EndpointAccess access = EndpointAccess.of(handlerMethod);
            if (access instanceof EndpointAccess.Public) {
                operation.setSecurity(new ArrayList<>());
                return operation;
            }
            if (operation.getResponses() == null) {
                operation.setResponses(new ApiResponses());
            }
            operation.getResponses().putIfAbsent("401", new ApiResponse().$ref(UNAUTHENTICATED_REF));
            if (!(access instanceof EndpointAccess.AnyUser)) {
                operation.getResponses().putIfAbsent("403", new ApiResponse().$ref(ACCESS_DENIED_REF));
            }
            return operation;
        };
    }

    private static void addErrorResponses(
            Components components, MessageResolver messageResolver, Environment environment) {
        ApiError invalidFields = new ApiError(
                "request.invalidFields",
                messageResolver.get("request.invalidFields"),
                List.of(
                        new ApiFieldError("name", messageResolver.get("jakarta.validation.constraints.NotBlank.message")),
                        new ApiFieldError(
                                "items[0].quantity",
                                messageResolver.get("jakarta.validation.constraints.Positive.message"))));
        ApiError notFound = ApiError.of("customer.notFound", environment.getRequiredProperty("openapi.example.notFound"));
        ApiError conflict = ApiError.of(
                "customer.documentAlreadyExists", environment.getRequiredProperty("openapi.example.conflict"));
        ApiError businessRule = ApiError.of(
                "serviceOrder.invalidStatusTransition", environment.getRequiredProperty("openapi.example.businessRule"));
        ApiError internalError =
                ApiError.of("server.unexpectedError", messageResolver.get("server.unexpectedError", "a1b2c3d4"));
        ApiError invalidCredentials =
                ApiError.of("auth.invalidCredentials", messageResolver.get("auth.invalidCredentials"));
        ApiError unauthenticated = ApiError.of("auth.unauthenticated", messageResolver.get("auth.unauthenticated"));
        ApiError accessDenied = ApiError.of("auth.accessDenied", messageResolver.get("auth.accessDenied"));
        ApiError tooManyRequests =
                ApiError.of("request.tooManyRequests", messageResolver.get("request.tooManyRequests"));
        Header retryAfter = new Header()
                .description(environment.getRequiredProperty("openapi.response.retryAfter"))
                .schema(new IntegerSchema().example(12));

        components
                .addResponses("BadRequest", errorResponse(environment, "badRequest", invalidFields))
                .addResponses("NotFound", errorResponse(environment, "notFound", notFound))
                .addResponses("Conflict", errorResponse(environment, "conflict", conflict))
                .addResponses("BusinessRule", errorResponse(environment, "businessRule", businessRule))
                .addResponses("InternalError", errorResponse(environment, "internalError", internalError))
                .addResponses(
                        "InvalidCredentials", errorResponse(environment, "invalidCredentials", invalidCredentials))
                .addResponses("Unauthenticated", errorResponse(environment, "unauthenticated", unauthenticated))
                .addResponses("AccessDenied", errorResponse(environment, "accessDenied", accessDenied))
                .addResponses(
                        "TooManyRequests",
                        errorResponse(environment, "tooManyRequests", tooManyRequests)
                                .addHeaderObject(HttpHeaders.RETRY_AFTER, retryAfter));
    }

    private static ApiResponse errorResponse(Environment environment, String name, ApiError example) {
        MediaType mediaType = new MediaType().schema(new Schema<>().$ref(API_ERROR_REF)).example(asMap(example));
        return new ApiResponse()
                .description(environment.getRequiredProperty("openapi.response." + name))
                .content(new Content().addMediaType(JSON, mediaType));
    }

    private static Map<String, @Nullable Object> asMap(ApiError error) {
        Map<String, @Nullable Object> map = new LinkedHashMap<>();
        map.put("code", error.code());
        map.put("message", error.message());
        map.put("details", error.details());
        return map;
    }

    private static String versionOf(@Nullable BuildProperties buildProperties) {
        String version = buildProperties != null ? buildProperties.getVersion() : null;
        return version != null ? version : DEFAULT_VERSION;
    }
}
