package io.github.yaforster.trails.notifier.smtp;

import io.github.yaforster.trails.sdk.notification.EmailRecipients;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Validated configuration for TLS-protected SMTP notification delivery. Spring Boot binds these values from the
 * {@code trails.notifier.smtp} configuration namespace. The host, sender, port, and TLS mode are required. Omitted
 * timeouts default to ten seconds; omitted authentication disables SMTP authentication, and omitted recipients produce
 * no static recipients.
 *
 * @param host
 *            non-blank SMTP server host name
 * @param port
 *            SMTP server port, from {@value #MIN_PORT_NUMBER} through {@value #MAX_PORT_NUMBER}
 * @param sender
 *            valid sender email address; surrounding whitespace is removed and the address is canonicalized
 * @param recipients
 *            optional static recipient addresses; these are merged with request-scoped recipients when a notification
 *            is sent
 * @param tls
 *            required transport security mode; plaintext SMTP is not supported
 * @param authentication
 *            binder-facing optional credentials; when configured, username and password must both be non-blank
 * @param connectionTimeout
 *            positive upper bound for opening a TCP connection, defaulting to ten seconds and limited to
 *            {@link Integer#MAX_VALUE} milliseconds
 * @param readTimeout
 *            positive upper bound for reading an SMTP response, with the same default and limit as
 *            {@code connectionTimeout}
 * @param writeTimeout
 *            positive upper bound for writing SMTP data, with the same default and limit as {@code connectionTimeout}
 */
@ConfigurationProperties("trails.notifier.smtp")
public record MailProperties(String host, Integer port, String sender, List<String> recipients, TLSMode tls,
        Authentication authentication, Duration connectionTimeout, Duration readTimeout, Duration writeTimeout) {

    public static final int MAX_PORT_NUMBER = 65535;
    public static final int MIN_PORT_NUMBER = 1;
    private static final String PROPERTY_PREFIX = "trails.notifier.smtp.";
    private static final String CONNECTION_TIMEOUT_PROPERTY = "connection-timeout";
    private static final String READ_TIMEOUT_PROPERTY = "read-timeout";
    private static final String WRITE_TIMEOUT_PROPERTY = "write-timeout";
    private static final String REQUIRED_PROPERTY_MESSAGE = "%s is required";
    private static final String INVALID_EMAIL_MESSAGE = "%s must be a valid email address";
    private static final String INVALID_TIMEOUT_MESSAGE = "trails.notifier.smtp.%s must be positive and no greater than %dms";
    private static final String INVALID_HOST_MESSAGE = "trails.notifier.smtp.host is required and must not contain whitespace";
    private static final String INVALID_PORT_MESSAGE = "trails.notifier.smtp.port must be between 1 and 65535";
    private static final String TLS_REQUIRED_MESSAGE = "trails.notifier.smtp.tls is required";
    private static final long MAX_TIMEOUT_MILLIS = Integer.MAX_VALUE;
    private static final Duration MAX_TIMEOUT = Duration.ofMillis(MAX_TIMEOUT_MILLIS);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Validates required settings, canonicalizes email addresses, and applies default timeouts.
     *
     * @param host
     *            SMTP server host
     * @param port
     *            SMTP server port
     * @param sender
     *            sender email address
     * @param recipients
     *            optional static recipients
     * @param tls
     *            required transport security mode
     * @param authentication
     *            optional binder-provided SMTP credentials
     * @param connectionTimeout
     *            connection timeout, or {@code null} to use the default
     * @param readTimeout
     *            read timeout, or {@code null} to use the default
     * @param writeTimeout
     *            write timeout, or {@code null} to use the default
     */
    public MailProperties {
        host = requiredHost(host);
        port = requiredPort(port);
        sender = requiredSender(sender);
        recipients = staticRecipients(recipients);
        tls = requiredTlsMode(tls);
        connectionTimeout = timeout(connectionTimeout, CONNECTION_TIMEOUT_PROPERTY);
        readTimeout = timeout(readTimeout, READ_TIMEOUT_PROPERTY);
        writeTimeout = timeout(writeTimeout, WRITE_TIMEOUT_PROPERTY);
    }

    /**
     * Returns configured credentials when SMTP authentication is enabled. The Spring configuration binder supplies
     * {@code null} when the optional {@code authentication} section is absent; consumers should use this method rather
     * than the binder-facing record component.
     *
     * @return configured credentials, or empty when SMTP authentication is disabled
     */
    public Optional<Authentication> smtpAuthentication() {
        return Optional.ofNullable(authentication);
    }

    private static String requiredHost(String value) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(INVALID_HOST_MESSAGE);
        }
        return value;
    }

    private static int requiredPort(Integer value) {
        if (value == null || value < MIN_PORT_NUMBER || value > MAX_PORT_NUMBER) {
            throw new IllegalArgumentException(INVALID_PORT_MESSAGE);
        }
        return value;
    }

    private static String requiredSender(String value) {
        return emailAddress(value);
    }

    private static List<String> staticRecipients(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return new EmailRecipients(values).values();
    }

    private static TLSMode requiredTlsMode(TLSMode value) {
        if (value == null) {
            throw new IllegalArgumentException(TLS_REQUIRED_MESSAGE);
        }
        return value;
    }

    private static String emailAddress(String value) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(REQUIRED_PROPERTY_MESSAGE.formatted("trails.notifier.smtp.sender"));
        }
        try {
            return new EmailRecipients(List.of(value)).values().getFirst();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(INVALID_EMAIL_MESSAGE.formatted("trails.notifier.smtp.sender"),
                    exception);
        }
    }

    private static Duration timeout(Duration value, String property) {
        Duration timeout = DEFAULT_TIMEOUT;
        if (value != null) {
            timeout = value;
        }
        if (isInvalidTimeoutValue(timeout)) {
            throw new IllegalArgumentException(
                    INVALID_TIMEOUT_MESSAGE.formatted(PROPERTY_PREFIX + property, MAX_TIMEOUT_MILLIS));
        }
        return timeout;
    }

    private static boolean isInvalidTimeoutValue(Duration timeout) {
        return timeout.isNegative() || timeout.isZero() || timeout.compareTo(MAX_TIMEOUT) > 0;
    }

    private static int toMillis(Duration timeout) {
        return Math.toIntExact(timeout.toMillis());
    }

    int connectionTimeoutMillis() {
        return toMillis(connectionTimeout);
    }

    int readTimeoutMillis() {
        return toMillis(readTimeout);
    }

    int writeTimeoutMillis() {
        return toMillis(writeTimeout);
    }

}
