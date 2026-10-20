package io.github.yaforster.trails.notifier.smtp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MailPropertiesTest {

    @Test
    void shouldCanonicalizeSenderAndStaticRecipients() {
        MailProperties properties = properties(" Sender@Example.Test ", List.of(" Alerts@Example.Test "),
                TLSMode.STARTTLS, Optional.empty());

        assertThat(properties.sender()).isEqualTo("Sender@example.test");
        assertThat(properties.recipients()).containsExactly("Alerts@example.test");
    }

    @Test
    void shouldRejectPartialAuthentication() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Authentication("mailer", null))
                .withMessageContaining("must contain a valid username and password.");
    }

    @Test
    void shouldRejectBlankAuthenticationCredentials() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Authentication(" ", "secret"))
                .withMessageContaining("must contain a valid username and password.");
    }

    @Test
    void shouldRejectMissingTlsMode() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> properties("sender@example.test", List.of(), null, Optional.empty()))
                .withMessage("trails.notifier.smtp.tls is required");
    }

    @Test
    void shouldRejectOutOfRangePort() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MailProperties("smtp.example.test", 65_536, "sender@example.test", List.of(),
                        TLSMode.STARTTLS, null, null, null, null))
                .withMessage("trails.notifier.smtp.port must be between 1 and 65535");
    }

    @Test
    void shouldRejectNonPositiveTimeout() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MailProperties("smtp.example.test", 587, "sender@example.test", List.of(),
                        TLSMode.STARTTLS, null, Duration.ZERO, null, null))
                .withMessageContaining("connection-timeout must be positive");
    }

    private static MailProperties properties(String sender, List<String> recipients, TLSMode tls,
            Optional<Authentication> authentication) {
        return new MailProperties("smtp.example.test", 587, sender, recipients, tls, authentication.orElse(null), null,
                null, null);
    }

}
