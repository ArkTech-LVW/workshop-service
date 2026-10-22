package br.com.arktech.workshop.shared.api.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = ErrorTestController.class)
@Import(GlobalExceptionHandlerTest.ErrorHandlingConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    private static final Pattern REFERENCE_CODE = Pattern.compile("Código de referência: ([0-9a-f]{8})\\.");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldMapNotFoundTo404WithArgs() throws Exception {
        perform(get("/test/errors/notFound"))
                .andExpect(status().isNotFound())
                .andExpect(json("""
                        {"code": "test.notFound", "message": "Recurso de teste 42 não encontrado.", "details": null}
                        """));
    }

    @Test
    void shouldMapConflictTo409() throws Exception {
        perform(get("/test/errors/conflict"))
                .andExpect(status().isConflict())
                .andExpect(json("""
                        {"code": "test.conflict", "message": "Recurso de teste em conflito.", "details": null}
                        """));
    }

    @Test
    void shouldMapBusinessRuleTo422() throws Exception {
        perform(get("/test/errors/businessRule"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(json("""
                        {"code": "test.businessRule", "message": "Regra de teste violada.", "details": null}
                        """));
    }

    @Test
    void shouldMapModuleSpecificBusinessRuleTo422() throws Exception {
        perform(get("/test/errors/invalidStatusTransition"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(json("""
                        {
                          "code": "test.invalidStatusTransition",
                          "message": "Transição de status de teste inválida: FINISHED.",
                          "details": null
                        }
                        """));
    }

    @Test
    void shouldListEveryInvalidFieldOfTheBody() throws Exception {
        perform(postJson("/test/errors/body", """
                        {"name": "", "items": [{"name": " ", "quantity": 1}]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.length()").value(3))
                .andExpect(content().json("""
                        {
                          "code": "request.invalidFields",
                          "message": "Existem campos inválidos na requisição.",
                          "details": [
                            {"field": "name", "message": "Não pode estar em branco."},
                            {"field": "name", "message": "Deve ter entre 3 e 10 caracteres."},
                            {"field": "items[0].name", "message": "Não pode estar em branco."}
                          ]
                        }
                        """, JsonCompareMode.LENIENT));
    }

    @Test
    void shouldInterpolateConstraintAttributes() throws Exception {
        perform(postJson("/test/errors/body", """
                        {"name": "ab", "items": []}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(json(invalidFields("name", "Deve ter entre 3 e 10 caracteres.")));
    }

    @Test
    void shouldReportMethodValidationByParameterName() throws Exception {
        perform(get("/test/errors/param").param("quantity", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(json(invalidFields("quantity", "Deve ser maior que zero.")));
    }

    @Test
    void shouldRejectMalformedJson() throws Exception {
        perform(postJson("/test/errors/body", "{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(json("""
                        {"code": "request.malformedBody", "message": "O corpo da requisição está malformado.", "details": null}
                        """));
    }

    @Test
    void shouldReportPathOfInvalidJsonValue() throws Exception {
        perform(postJson("/test/errors/body", """
                        {"name": "valid", "items": [{"name": "item", "quantity": "abc"}]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(json(invalidFields("items[0].quantity", "Valor inválido.")));
    }

    @Test
    void shouldReportRequestParamTypeMismatch() throws Exception {
        perform(get("/test/errors/param").param("quantity", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(json(invalidFields("quantity", "Valor inválido.")));
    }

    @Test
    void shouldReportPathVariableTypeMismatch() throws Exception {
        perform(get("/test/errors/path/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(json(invalidFields("id", "Valor inválido.")));
    }

    @Test
    void shouldReportMissingRequestParam() throws Exception {
        perform(get("/test/errors/param"))
                .andExpect(status().isBadRequest())
                .andExpect(json(invalidFields("quantity", "Parâmetro obrigatório.")));
    }

    @Test
    void shouldReportUnknownRoute() throws Exception {
        perform(get("/test/errors/doesNotExist"))
                .andExpect(status().isNotFound())
                .andExpect(json("""
                        {"code": "request.routeNotFound", "message": "Rota não encontrada.", "details": null}
                        """));
    }

    @Test
    void shouldReportUnsupportedMethodKeepingAllowHeader() throws Exception {
        perform(post("/test/errors/notFound"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(HttpHeaders.ALLOW, "GET"))
                .andExpect(json("""
                        {
                          "code": "request.methodNotSupported",
                          "message": "O método POST não é suportado nesta rota.",
                          "details": null
                        }
                        """));
    }

    @Test
    void shouldReportUnsupportedMediaType() throws Exception {
        perform(post("/test/errors/body").contentType(MediaType.TEXT_PLAIN).content("name"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(json("""
                        {"code": "request.unsupportedMediaType", "message": "Tipo de conteúdo não suportado.", "details": null}
                        """));
    }

    @Test
    void shouldReportNotAcceptableAsJson() throws Exception {
        perform(get("/test/errors/ok").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable())
                .andExpect(json("""
                        {"code": "request.notAcceptable", "message": "Formato de resposta não suportado.", "details": null}
                        """));
    }

    @Test
    void shouldHideUnexpectedErrorBehindReferenceCode(CapturedOutput output) throws Exception {
        String body = perform(get("/test/errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("server.unexpectedError"))
                .andExpect(jsonPath("$.details").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Matcher matcher = REFERENCE_CODE.matcher(body);
        assertThat(matcher.find()).as("reference code in %s", body).isTrue();
        assertThat(body).doesNotContain("sensitive internal detail");
        assertThat(output)
                .contains("Unexpected error [referenceCode=" + matcher.group(1) + "]")
                .contains("java.lang.IllegalStateException: sensitive internal detail")
                .contains("at br.com.arktech.workshop.shared.api.error.ErrorTestController.unexpected");
    }

    @Test
    void shouldFailWhenValidationKeyIsMissing(CapturedOutput output) throws Exception {
        perform(postJson("/test/errors/missingKey", """
                        {"name": ""}
                        """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("server.unexpectedError"));

        assertThat(output).contains("test.missingKey");
    }

    @Test
    void shouldFailWhenValidationMessageIsLiteralText(CapturedOutput output) throws Exception {
        perform(postJson("/test/errors/literalMessage", """
                        {"name": ""}
                        """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("server.unexpectedError"));

        assertThat(output).contains("Validation message must be a {key}: texto literal");
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    private static MockHttpServletRequestBuilder postJson(String uri, String body) {
        return post(uri).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static ResultMatcher json(String expected) {
        return content().json(expected, JsonCompareMode.STRICT);
    }

    private static String invalidFields(String field, String message) {
        return """
                {
                  "code": "request.invalidFields",
                  "message": "Existem campos inválidos na requisição.",
                  "details": [{"field": "%s", "message": "%s"}]
                }
                """.formatted(field, message);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @ComponentScan({
        "br.com.arktech.workshop.shared.application.i18n",
        "br.com.arktech.workshop.shared.infrastructure.i18n",
        "br.com.arktech.workshop.shared.infrastructure.validation"
    })
    static class ErrorHandlingConfig {}
}
