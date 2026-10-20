package io.github.yaforster.trails.notifier.smtp;

/** TLS connection modes supported by the SMTP notifier. Plaintext transport is not supported. */
public enum TLSMode {
    /** Upgrade the SMTP connection using STARTTLS and require certificate verification. */
    STARTTLS,

    /** Negotiate TLS immediately when opening the SMTP connection. */
    IMPLICIT
}
