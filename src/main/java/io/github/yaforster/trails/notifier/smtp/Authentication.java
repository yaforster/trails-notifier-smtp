package io.github.yaforster.trails.notifier.smtp;

import org.apache.commons.lang3.StringUtils;

/**
 * SMTP authentication credentials. Both values must be non-blank; when authentication is disabled, the optional
 * configuration section is absent instead of constructing this value.
 *
 * @param username
 *            SMTP authentication principal, or {@code null} when authentication is disabled
 * @param password
 *            SMTP authentication secret, or {@code null} when authentication is disabled
 */
public record Authentication(String username, String password) {

    public static final String INVALID_CONFIGURATION_ERROR_MESSAGE = "trails.notifier.smtp.authentication must contain a valid username and password.";

    /** Validates that SMTP authentication is either fully configured or disabled. */
    public Authentication {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            throw new IllegalArgumentException(INVALID_CONFIGURATION_ERROR_MESSAGE);
        }
    }

}
