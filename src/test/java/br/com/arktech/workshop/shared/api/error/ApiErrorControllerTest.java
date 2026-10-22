package br.com.arktech.workshop.shared.api.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.arktech.workshop.shared.domain.exception.NotFoundException;
import jakarta.servlet.RequestDispatcher;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = ApiErrorController.class)
@Import(ApiErrorTestConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class ApiErrorControllerTest {

    private static final Pattern REFERENCE_CODE = Pattern.compile("Código de referência: ([0-9a-f]{8})\\.");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldHideExceptionThrownOutsideDispatcherBehindReferenceCode(CapturedOutput output) throws Exception {
        String body = perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                        .requestAttr(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException("filter failure")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("server.unexpectedError"))
                .andExpect(jsonPath("$.details").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Matcher matcher = REFERENCE_CODE.matcher(body);
        assertThat(matcher.find()).as("reference code in %s", body).isTrue();
        assertThat(body).doesNotContain("filter failure");
        assertThat(output)
                .contains("Unexpected error [referenceCode=" + matcher.group(1) + "]")
                .contains("java.lang.IllegalStateException: filter failure");
    }

    @Test
    void shouldReturn500ForExceptionEvenWithAnotherStatus() throws Exception {
        perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                        .requestAttr(RequestDispatcher.ERROR_EXCEPTION, new NotFoundException("test.missingKey")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("server.unexpectedError"));
    }

    @Test
    void shouldKeepStatusSentWithoutException() throws Exception {
        perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404))
                .andExpect(status().isNotFound())
                .andExpect(json("""
                        {"code": "request.routeNotFound", "message": "Rota não encontrada.", "details": null}
                        """));
    }

    @Test
    void shouldUseRequestMethodForMethodNotAllowed() throws Exception {
        perform(post("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 405))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(json("""
                        {
                          "code": "request.methodNotSupported",
                          "message": "O método POST não é suportado nesta rota.",
                          "details": null
                        }
                        """));
    }

    @Test
    void shouldMapOtherClientErrorsToGenericCode() throws Exception {
        perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 403))
                .andExpect(status().isForbidden())
                .andExpect(json("""
                        {"code": "request.error", "message": "Não foi possível processar a requisição.", "details": null}
                        """));
    }

    @Test
    void shouldReturn500WhenNoErrorAttributesArePresent() throws Exception {
        perform(get("/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("server.unexpectedError"));
    }

    @Test
    void shouldAnswerJsonEvenWhenClientAsksForHtml() throws Exception {
        perform(get("/error").accept(MediaType.TEXT_HTML).requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("request.routeNotFound"));
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request).andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    private static ResultMatcher json(String expected) {
        return content().json(expected, JsonCompareMode.STRICT);
    }
}
