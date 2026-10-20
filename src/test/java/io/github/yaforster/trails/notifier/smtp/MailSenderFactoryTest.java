package io.github.yaforster.trails.notifier.smtp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class MailSenderFactoryTest {

    @Test
    void shouldRequireStartTlsAndConfigureFiniteTimeouts() {
        JavaMailSenderImpl sender = MailSenderFactory.create(properties(TLSMode.STARTTLS));

        assertThat(sender.getJavaMailProperties()).containsEntry("mail.smtp.starttls.enable", "true")
                .containsEntry("mail.smtp.starttls.required", "true")
                .containsEntry("mail.smtp.ssl.checkserveridentity", "true")
                .containsEntry("mail.smtp.connectiontimeout", "1000").containsEntry("mail.smtp.timeout", "2000")
                .containsEntry("mail.smtp.writetimeout", "3000").doesNotContainKey("mail.smtp.ssl.enable");
    }

    @Test
    void shouldUseTlsFromConnectionStartWhenConfigured() {
        JavaMailSenderImpl sender = MailSenderFactory.create(properties(TLSMode.IMPLICIT));

        assertThat(sender.getJavaMailProperties()).containsEntry("mail.smtp.ssl.enable", "true")
                .containsEntry("mail.smtp.ssl.checkserveridentity", "true")
                .doesNotContainKey("mail.smtp.starttls.enable");
    }

    private static MailProperties properties(TLSMode tls) {
        return new MailProperties("smtp.example.test", 587, "sender@example.test", List.of(), tls,
                new Authentication("mailer", "secret"), Duration.ofSeconds(1), Duration.ofSeconds(2),
                Duration.ofSeconds(3));
    }

}
