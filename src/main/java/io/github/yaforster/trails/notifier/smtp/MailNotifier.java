package io.github.yaforster.trails.notifier.smtp;

import io.github.yaforster.trails.sdk.composition.AdapterId;
import io.github.yaforster.trails.sdk.notification.Notifier;
import io.github.yaforster.trails.sdk.notification.TerminalRunNotification;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * Delivers a concise persisted terminal-run notification to configured SMTP recipients.
 */
final class MailNotifier implements Notifier {

    private static final AdapterId ADAPTER_ID = new AdapterId("smtp");
    private final JavaMailSender mailSender;
    private final MailProperties properties;

    MailNotifier(JavaMailSender mailSender, MailProperties properties) {
        this.mailSender = Objects.requireNonNull(mailSender, "SMTP mail sender is required");
        this.properties = Objects.requireNonNull(properties, "SMTP properties are required");
    }

    @Override
    public AdapterId adapterId() {
        return ADAPTER_ID;
    }

    @Override
    public void notify(TerminalRunNotification notification) {
        Objects.requireNonNull(notification, "Terminal notification is required");
        List<String> recipients = recipients(notification);
        if (recipients.isEmpty()) {
            return;
        }
        mailSender.send(message(notification, recipients));
    }

    private List<String> recipients(TerminalRunNotification notification) {
        LinkedHashSet<String> recipients = new LinkedHashSet<>(properties.recipients());
        notification.emailRecipients()
                    .ifPresent(requestRecipients -> recipients.addAll(requestRecipients.values()));
        return List.copyOf(recipients);
    }

    private MimeMessage message(TerminalRunNotification notification, List<String> recipients) {
        MailContent content = MailContent.from(notification);
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.sender());
            helper.setTo(recipients.toArray(String[]::new));
            helper.setSubject(content.subject());
            helper.setText(content.plainText(), content.html());
            return message;
        } catch (MessagingException exception) {
            throw new MailPreparationException("Could not prepare terminal notification", exception);
        }
    }

}
