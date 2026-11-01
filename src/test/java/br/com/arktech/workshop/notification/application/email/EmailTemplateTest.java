package br.com.arktech.workshop.notification.application.email;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.arktech.workshop.notification.application.email.EmailTemplate.RenderedEmail;
import br.com.arktech.workshop.shared.application.notification.Notification;
import br.com.arktech.workshop.shared.application.notification.NotificationType;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EmailTemplateTest {

    private static final String PUBLIC_ID = "7f3c2a1e-5b4d-4c6e-8f9a-0b1c2d3e4f5a";

    private final EmailTemplate emailTemplate = TestEmailTemplates.create();

    @ParameterizedTest
    @EnumSource(NotificationType.class)
    void shouldRenderTheHtmlOfEveryTypeWithTheLayoutAndTheParameters(NotificationType type) {
        RenderedEmail email =
                emailTemplate.render(new Notification(10L, type, List.of(PUBLIC_ID, "R$ 1.234,50", "Achou caro")));

        assertThat(email.htmlBody())
                .startsWith("<!DOCTYPE html>")
                .contains("ArkTech Oficina", email.subject(), PUBLIC_ID, "R$ 1.234,50", "Ordem de serviço")
                .doesNotContain("??", "th:text", "th:replace", "xmlns:th");
        assertThat(email.body()).contains(PUBLIC_ID).doesNotContain("<");
    }

    @Test
    void shouldShowTheRejectionReason() {
        RenderedEmail email = emailTemplate.render(new Notification(
                10L, NotificationType.SERVICE_ORDER_QUOTE_REJECTED, List.of(PUBLIC_ID, "R$ 1.234,50", "Achou caro")));

        assertThat(email.htmlBody()).contains("Motivo", "Achou caro", "Orçamento rejeitado");
    }

    @Test
    void shouldEscapeTheParameters() {
        RenderedEmail email = emailTemplate.render(new Notification(
                10L,
                NotificationType.SERVICE_ORDER_QUOTE_REJECTED,
                List.of(PUBLIC_ID, "R$ 1.234,50", "<script>alert(1)</script>")));

        assertThat(email.htmlBody()).doesNotContain("<script>").contains("&lt;script&gt;");
    }
}
