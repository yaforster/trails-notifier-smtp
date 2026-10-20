package io.github.yaforster.trails.notifier.smtp;

import java.util.Properties;
import org.springframework.mail.javamail.JavaMailSenderImpl;

final class MailSenderFactory {

    public static final String PROP_ENABLE_AUTH = "mail.smtp.auth";
    public static final String PROP_CONNECTION_TIMEOUT = "mail.smtp.connectiontimeout";
    public static final String PROP_READ_TIMEOUT = "mail.smtp.timeout";
    public static final String PROP_WRITE_TIMEOUT = "mail.smtp.writetimeout";
    public static final String PROP_ENABLE_STARTTLS = "mail.smtp.starttls.enable";
    public static final String PROP_REQUIRE_STARTTLS = "mail.smtp.starttls.required";
    public static final String PROP_CHECK_SERVER_IDENTITY = "mail.smtp.ssl.checkserveridentity";
    public static final String PROP_ENABLE_SSL = "mail.smtp.ssl.enable";

    private MailSenderFactory() {}

    static JavaMailSenderImpl create(MailProperties properties) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(properties.host());
        sender.setPort(properties.port());
        properties.smtpAuthentication().ifPresent(authentication -> {
            sender.setUsername(authentication.username());
            sender.setPassword(authentication.password());
        });
        Properties props = createJavaMailProperties(properties);
        sender.setJavaMailProperties(props);
        return sender;
    }

    private static Properties createJavaMailProperties(MailProperties properties) {
        Properties mail = new Properties();
        mail.setProperty(PROP_ENABLE_AUTH, Boolean.toString(properties.smtpAuthentication().isPresent()));
        mail.setProperty(PROP_CONNECTION_TIMEOUT, String.valueOf(properties.connectionTimeoutMillis()));
        mail.setProperty(PROP_READ_TIMEOUT, String.valueOf(properties.readTimeoutMillis()));
        mail.setProperty(PROP_WRITE_TIMEOUT, String.valueOf(properties.writeTimeoutMillis()));
        TLSMode tlsMode = properties.tls();
        configureTLS(tlsMode, mail);
        return mail;
    }

    private static void configureTLS(TLSMode tls, Properties mail) {
        if (tls == TLSMode.STARTTLS) {
            mail.setProperty(PROP_ENABLE_STARTTLS, "true");
            mail.setProperty(PROP_REQUIRE_STARTTLS, "true");
            mail.setProperty(PROP_CHECK_SERVER_IDENTITY, "true");
            return;
        }
        mail.setProperty(PROP_ENABLE_SSL, "true");
        mail.setProperty(PROP_CHECK_SERVER_IDENTITY, "true");
    }

}
