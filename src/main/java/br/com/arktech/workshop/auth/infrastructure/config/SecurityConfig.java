package br.com.arktech.workshop.auth.infrastructure.config;

import br.com.arktech.workshop.auth.infrastructure.ratelimit.RateLimitFilter;
import br.com.arktech.workshop.auth.infrastructure.security.EndpointAuthorizationManager;
import br.com.arktech.workshop.auth.infrastructure.security.PublicRouteBearerTokenResolver;

import jakarta.servlet.DispatcherType;

import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
class SecurityConfig {

    private static final RequestMatcher PUBLIC_PATHS = new OrRequestMatcher(
            Stream.of("/actuator/health", "/actuator/health/**", "/docs", "/docs/**", "/swagger-ui/**")
                    .map(PathPatternRequestMatcher.withDefaults()::matcher)
                    .map(RequestMatcher.class::cast)
                    .toList());

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            EndpointAuthorizationManager endpointAuthorizationManager,
            RateLimitProperties rateLimitProperties,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver)
            throws Exception {
        AuthenticationEntryPoint authenticationEntryPoint =
                (request, response, ex) -> exceptionResolver.resolveException(request, response, null, ex);
        AccessDeniedHandler accessDeniedHandler =
                (request, response, ex) -> exceptionResolver.resolveException(request, response, null, ex);

        return http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(PUBLIC_PATHS)
                        .permitAll()
                        .anyRequest()
                        .access(endpointAuthorizationManager))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .bearerTokenResolver(
                                new PublicRouteBearerTokenResolver(PUBLIC_PATHS, endpointAuthorizationManager))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterAfter(
                        new RateLimitFilter(rateLimitProperties, exceptionResolver),
                        BearerTokenAuthenticationFilter.class)
                .build();
    }
}
