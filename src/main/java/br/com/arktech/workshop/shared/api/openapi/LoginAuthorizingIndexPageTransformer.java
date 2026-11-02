package br.com.arktech.workshop.shared.api.openapi;

import java.io.IOException;
import java.io.InputStream;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;

class LoginAuthorizingIndexPageTransformer extends SwaggerIndexPageTransformer {

    private static final String BUNDLE_CALL = "window.ui = SwaggerUIBundle({";

    private static final String RESPONSE_INTERCEPTOR = """

                responseInterceptor: (response) => {
                  const path = new URL(response.url, window.location.href).pathname;
                  if (response.ok && path.endsWith('%s') && response.body && response.body.accessToken) {
                    window.ui.preauthorizeApiKey('%s', response.body.accessToken);
                  }
                  return response;
                },""";

    private final String loginPath;

    LoginAuthorizingIndexPageTransformer(
            SwaggerUiConfigProperties swaggerUiConfig,
            SwaggerUiOAuthProperties swaggerUiOAuthProperties,
            SwaggerWelcomeCommon swaggerWelcomeCommon,
            ObjectMapperProvider objectMapperProvider,
            String loginPath) {
        super(swaggerUiConfig, swaggerUiOAuthProperties, swaggerWelcomeCommon, objectMapperProvider);
        this.loginPath = loginPath;
    }

    @Override
    protected String defaultTransformations(SwaggerUiConfigParameters swaggerUiConfigParameters, InputStream inputStream)
            throws IOException {
        String script = super.defaultTransformations(swaggerUiConfigParameters, inputStream);
        return script.replace(
                BUNDLE_CALL, BUNDLE_CALL + RESPONSE_INTERCEPTOR.formatted(loginPath, OpenApiConfig.BEARER_AUTH));
    }
}
