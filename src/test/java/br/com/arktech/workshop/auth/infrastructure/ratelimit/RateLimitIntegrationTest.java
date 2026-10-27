package br.com.arktech.workshop.auth.infrastructure.ratelimit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.IntegrationTest;
import br.com.arktech.workshop.auth.application.port.TokenIssuer;
import br.com.arktech.workshop.auth.domain.model.User;
import br.com.arktech.workshop.auth.domain.port.PasswordHasher;
import br.com.arktech.workshop.auth.domain.repository.UserRepository;
import br.com.arktech.workshop.shared.domain.email.Email;
import br.com.arktech.workshop.shared.domain.security.Role;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
@TestPropertySource(properties = {
    "security.rate-limit.general.capacity=4",
    "security.rate-limit.login-per-email-and-ip.capacity=3",
    "security.rate-limit.routes.login.capacity=6",
    "security.rate-limit.routes.public.path=/test/security/public",
    "security.rate-limit.routes.public.capacity=2",
    "security.rate-limit.routes.public.period=PT1M"
})
class RateLimitIntegrationTest {

    private static final String PASSWORD = "Sup3r-Secret!";
    private static final String TOO_MANY_REQUESTS = """
            {
              "code": "request.tooManyRequests",
              "message": "Muitas requisições. Aguarde um pouco e tente novamente."
            }
            """;
    private static final AtomicInteger NEXT_IP = new AtomicInteger(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private TokenIssuer tokenIssuer;

    @Test
    void shouldRejectLoginOverTheEmailLimitEvenWithTheRightPassword() throws Exception {
        String email = saveUser();
        String ip = newIp();
        failLogin(email, ip, 3);

        login(email, PASSWORD, ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(content().json(TOO_MANY_REQUESTS, JsonCompareMode.STRICT));
    }

    @Test
    void shouldNotLimitTheSameEmailFromAnotherIp() throws Exception {
        String email = saveUser();
        String attackerIp = newIp();
        failLogin(email, attackerIp, 3);
        login(email, PASSWORD, attackerIp).andExpect(status().isTooManyRequests());

        login(email, PASSWORD, newIp()).andExpect(status().isOk());
    }

    @Test
    void shouldNotLimitAnotherEmailFromTheSameIp() throws Exception {
        String ip = newIp();
        failLogin(saveUser(), ip, 3);

        login(saveUser(), PASSWORD, ip).andExpect(status().isOk());
    }

    @Test
    void shouldLimitLoginPerIpAcrossEmails() throws Exception {
        String ip = newIp();
        for (int i = 0; i < 6; i++) {
            login(unknownEmail(), PASSWORD, ip).andExpect(status().isUnauthorized());
        }

        login(unknownEmail(), PASSWORD, ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
        login(unknownEmail(), PASSWORD, newIp()).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldLimitOtherRoutesPerLoggedUser() throws Exception {
        String ip = newIp();
        String token = tokenFor(randomUserId());
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(from(ip, me(token))).andExpect(status().isOk());
        }

        mockMvc.perform(from(ip, me(token)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(content().json(TOO_MANY_REQUESTS, JsonCompareMode.STRICT));
        mockMvc.perform(from(ip, me(tokenFor(randomUserId())))).andExpect(status().isOk());
    }

    @Test
    void shouldLimitAnonymousRequestsPerIp() throws Exception {
        String ip = newIp();
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(from(ip, get("/test/errors/ok"))).andExpect(status().isOk());
        }

        mockMvc.perform(from(ip, get("/test/errors/ok"))).andExpect(status().isTooManyRequests());
        mockMvc.perform(from(newIp(), get("/test/errors/ok"))).andExpect(status().isOk());
    }

    @Test
    void shouldApplyTheRouteLimitInsteadOfTheGeneralOne() throws Exception {
        String ip = newIp();
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(from(ip, get("/test/security/public"))).andExpect(status().isOk());
        }

        mockMvc.perform(from(ip, get("/test/security/public"))).andExpect(status().isTooManyRequests());
        mockMvc.perform(from(ip, get("/test/errors/ok"))).andExpect(status().isOk());
    }

    @Test
    void shouldNotLimitHealthCheck() throws Exception {
        String ip = newIp();
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(from(ip, get("/actuator/health"))).andExpect(status().isOk());
        }
    }

    private String saveUser() {
        String email = "user-" + UUID.randomUUID() + "@workshop.com";
        userRepository.save(new User("Ana", new Email(email), PASSWORD, Set.of(Role.ATTENDANT), passwordHasher));
        return email;
    }

    private void failLogin(String email, String ip, int times) throws Exception {
        for (int i = 0; i < times; i++) {
            login(email, "wrong-password", ip).andExpect(status().isUnauthorized());
        }
    }

    private ResultActions login(String email, String password, String ip) throws Exception {
        String body = """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
        return mockMvc.perform(from(ip, post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body)));
    }

    private MockHttpServletRequestBuilder me(String token) {
        return get("/test/security/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private String tokenFor(Long userId) {
        return tokenIssuer.issue(userId, Set.of(Role.ATTENDANT)).value();
    }

    private static MockHttpServletRequestBuilder from(String ip, MockHttpServletRequestBuilder request) {
        return request.with(mockRequest -> {
            mockRequest.setRemoteAddr(ip);
            return mockRequest;
        });
    }

    private static String newIp() {
        int n = NEXT_IP.getAndIncrement();
        return "10.0." + (n / 256) + "." + (n % 256);
    }

    private static String unknownEmail() {
        return "nobody-" + UUID.randomUUID() + "@workshop.com";
    }

    private static Long randomUserId() {
        return ThreadLocalRandom.current().nextLong(1_000_000, Long.MAX_VALUE);
    }
}
