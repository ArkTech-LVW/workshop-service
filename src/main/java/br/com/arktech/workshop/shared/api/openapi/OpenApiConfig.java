package br.com.arktech.workshop.shared.api.openapi;

import br.com.arktech.workshop.shared.api.error.ApiError;
import br.com.arktech.workshop.shared.api.error.ApiFieldError;
import br.com.arktech.workshop.shared.application.i18n.MessageResolver;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
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
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;

@Configuration
@PropertySource(value = "classpath:openapi/openapi.properties", encoding = "UTF-8")
class OpenApiConfig {

    private static final String TITLE = "Workshop Service API";
    private static final String DEFAULT_VERSION = "dev";
    private static final String BEARER_AUTH = "bearerAuth";
    private static final String JSON = org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
    private static final String API_ERROR_REF = "#/components/schemas/ApiError";
    private static final String INTERNAL_ERROR_REF = "#/components/responses/InternalError";

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
    OpenApiCustomizer internalErrorResponseCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().stream()
                    .flatMap(path -> path.readOperations().stream())
                    .forEach(operation ->
                            operation.getResponses().putIfAbsent("500", new ApiResponse().$ref(INTERNAL_ERROR_REF)));
        };
    }

    @Bean
    OperationCustomizer publicOperationCustomizer() {
        return (operation, handlerMethod) -> {
            SecurityRequirements requirements = handlerMethod.getMethodAnnotation(SecurityRequirements.class);
            if (requirements == null) {
                requirements = AnnotatedElementUtils.findMergedAnnotation(
                        handlerMethod.getBeanType(), SecurityRequirements.class);
            }
            if (requirements != null && requirements.value().length == 0) {
                operation.setSecurity(new ArrayList<>());
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

        components
                .addResponses("BadRequest", errorResponse(environment, "badRequest", invalidFields))
                .addResponses("NotFound", errorResponse(environment, "notFound", notFound))
                .addResponses("Conflict", errorResponse(environment, "conflict", conflict))
                .addResponses("BusinessRule", errorResponse(environment, "businessRule", businessRule))
                .addResponses("InternalError", errorResponse(environment, "internalError", internalError));
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
