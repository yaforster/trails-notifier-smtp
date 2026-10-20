package io.github.yaforster.trails.notifier.smtp;

import io.github.yaforster.trails.sdk.notification.Notifier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Automatically registers the SMTP notifier and its dedicated JavaMail sender when this adapter is on the application
 * classpath. Configure the sender through {@link MailProperties}; the adapter uses its own sender bean so SMTP settings
 * do not alter other mail integrations in the host.
 */
@AutoConfiguration
@EnableConfigurationProperties(MailProperties.class)
public class MailNotifierAutoConfiguration {

    public MailNotifierAutoConfiguration() {}

    @Bean
    JavaMailSender trailsMailSender(MailProperties properties) {
        return MailSenderFactory.create(properties);
    }

    @Bean
    Notifier trailsMailNotifier(@Qualifier("trailsMailSender") JavaMailSender mailSender, MailProperties properties) {
        return new MailNotifier(mailSender, properties);
    }

}
