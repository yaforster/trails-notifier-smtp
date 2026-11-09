package io.github.yaforster.trails.notifier.smtp;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.yaforster.trails.sdk.notification.Notifier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;

class TrailsMailNotifierAutoConfigurationTest {

    @Test
    void shouldRegisterOneSmtpNotifierAndItsDedicatedMailSender() {
        contextRunner().run(context -> {
            assertThat(context).hasSingleBean(Notifier.class);
            assertThat(context).hasSingleBean(JavaMailSender.class);
            assertThat(context).hasSingleBean(MailProperties.class);
            assertThat(context.getBean(MailProperties.class)
                              .smtpAuthentication()).isEmpty();
        });
    }

    @Test
    void shouldBindConfiguredAuthenticationCredentials() {
        contextRunner().withPropertyValues("trails.notifier.smtp.authentication.username=mailer",
                "trails.notifier.smtp.authentication.password=secret")
                       .run(context -> {
                           assertThat(context).hasNotFailed();
                           assertThat(context.getBean(MailProperties.class)
                                             .smtpAuthentication()).contains(new Authentication("mailer", "secret"));
                       });
    }

    @Test
    void shouldFailWhenSenderIsMissing() {
        contextRunner().withPropertyValues("trails.notifier.smtp.sender=")
                       .run(context -> {
                           assertThat(context.getStartupFailure()).hasRootCauseMessage(
                                   "trails.notifier.smtp.sender is required");
                       });
    }

    private static ApplicationContextRunner contextRunner() {
        return new ApplicationContextRunner().withConfiguration(
                AutoConfigurations.of(MailNotifierAutoConfiguration.class))
                                             .withPropertyValues("trails.notifier.smtp.host=smtp.example.test",
                                                     "trails.notifier.smtp.port=587",
                                                     "trails.notifier.smtp.sender=sender@example.test",
                                                     "trails.notifier.smtp.tls=starttls");
    }

}
