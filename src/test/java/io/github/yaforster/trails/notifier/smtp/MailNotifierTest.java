package io.github.yaforster.trails.notifier.smtp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.yaforster.trails.core.data.TestRunTerminalStatus;
import io.github.yaforster.trails.sdk.notification.EmailRecipients;
import io.github.yaforster.trails.sdk.notification.TerminalRunNotification;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import io.github.yaforster.trails.core.data.ResultIndicator;
import jakarta.mail.Session;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Properties;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mail.javamail.JavaMailSender;

class MailNotifierTest {

    @Test
    void shouldSendOneSafeMessageToUnifiedStaticAndDynamicRecipients() throws Exception {
        JavaMailSender mailSender = mock();
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        MailNotifier notifier = new MailNotifier(mailSender, properties(List.of("alerts@example.test")));
        TerminalRunNotification notification = notification(
                Optional.of(URI.create("https://trails.example.test/runs/42")),
                Optional.of(new EmailRecipients(List.of("alerts@example.test", "owner@example.test"))));

        notifier.notify(notification);

        ArgumentCaptor<MimeMessage> message = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(message.capture());
        assertThat(
                List.of(message.getValue().getFrom()[0].toString(), message.getValue().getAllRecipients()[0].toString(),
                        message.getValue().getAllRecipients()[1].toString(), message.getValue().getSubject()))
                .containsExactly("sender@example.test", "alerts@example.test", "owner@example.test",
                        "[PASSED] Trails run #42");
    }

    @Test
    void shouldNotSendWhenNeitherStaticNorDynamicRecipientsExist() {
        JavaMailSender mailSender = mock();
        MailNotifier notifier = new MailNotifier(mailSender, properties(List.of()));

        notifier.notify(notification(Optional.empty(), Optional.empty()));

        verifyNoInteractions(mailSender);
    }

    @Test
    void shouldOmitResultLinkWhenTheNotificationHasNoPublicResultUri() {
        MailContent content = MailContent.from(notification(Optional.empty(), Optional.empty()));
        assertThat(content.plainText() + content.html()).doesNotContain("Result:", "href=", "View result");
    }

    @ParameterizedTest
    @CsvSource({"COMPLETED,SUCCESS,Passed", "COMPLETED,FAILURE,Failed", "COMPLETED,PARTIAL_SUCCESS,Partial success",
            "FAILED,FAILURE,Execution failed"})
    void shouldDeliverEquivalentAlternatives_whenOutcomeVaries(TestRunTerminalStatus status, ResultIndicator indicator,
            String label) throws Exception {
        JavaMailSender mailSender = mock();
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        TerminalRunNotification notification = new TerminalRunNotification(
                UUID.fromString("c9c0c958-d0f6-4b56-a35c-e8f1ebc828a2"), 42, status, indicator,
                Optional.of(URI.create("https://trails.example.test/runs/42?a=1&b=2")), Optional.empty());
        new MailNotifier(mailSender, properties(List.of("alerts@example.test"))).notify(notification);
        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        sent.getValue().writeTo(bytes);
        MimeMessage decoded = new MimeMessage(Session.getInstance(new Properties()),
                new ByteArrayInputStream(bytes.toByteArray()));
        Multipart alternatives = alternatives(decoded);
        org.assertj.core.api.SoftAssertions.assertSoftly(softly -> {
            try {
                softly.assertThat(alternatives.getCount()).isEqualTo(2);
                softly.assertThat(alternatives.getBodyPart(0).getContentType()).startsWith("text/plain;")
                        .contains("UTF-8");
                softly.assertThat(alternatives.getBodyPart(1).getContentType()).startsWith("text/html;")
                        .contains("UTF-8");
                softly.assertThat(alternatives.getBodyPart(0).getContent().toString())
                        .contains(label + " — Trails run #42", notification.executionId().toString(), "?a=1&b=2");
                String html = alternatives.getBodyPart(1).getContent().toString();
                softly.assertThat(org.springframework.web.util.HtmlUtils.htmlUnescape(html))
                        .contains(label + " — Trails run #42", notification.executionId().toString());
                softly.assertThat(html).contains("href=\"https://trails.example.test/runs/42?a=1&amp;b=2\"");
            } catch (Exception exception) {
                throw new AssertionError("Cannot decode message alternatives", exception);
            }
        });
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"javascript:alert(1)", "/runs/42"})
    void shouldRenderNonWebUrisAsText(String uri) {
        assertThat(MailContent.from(notification(Optional.of(URI.create(uri)), Optional.empty())).html())
                .contains("Result: " + uri).doesNotContain("href=");
    }

    private static Multipart alternatives(Part part) throws Exception {
        if (part.isMimeType("multipart/alternative")) {
            return (Multipart) part.getContent();
        }
        if (part.getContent() instanceof Multipart multipart) {
            return alternatives(multipart.getBodyPart(0));
        }
        throw new AssertionError("Missing multipart/alternative");
    }

    private static MailProperties properties(List<String> recipients) {
        return new MailProperties("smtp.example.test", 587, "sender@example.test", recipients, TLSMode.STARTTLS, null,
                null, null, null);
    }

    private static TerminalRunNotification notification(Optional<URI> resultUri,
            Optional<EmailRecipients> emailRecipients) {
        return new TerminalRunNotification(UUID.fromString("c9c0c958-d0f6-4b56-a35c-e8f1ebc828a2"), 42,
                TestRunTerminalStatus.COMPLETED, ResultIndicator.SUCCESS, resultUri, emailRecipients);
    }

}
