package io.github.yaforster.trails.notifier.smtp;

import io.github.yaforster.trails.core.data.TestRunTerminalStatus;
import io.github.yaforster.trails.sdk.notification.TerminalRunNotification;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/** Compact, equivalent representations of the persisted notification summary. */
record MailContent(String subject, String plainText, String html) {

    private static final TemplateEngine TEMPLATES = templates();

    static MailContent from(TerminalRunNotification notification) {
        long testRunId = notification.testRunId();
        String resultText = notification.resultUri()
                                        .map(uri -> "\nResult: " + uri)
                                        .orElse("");
        Outcome outcome = outcome(notification);
        String heading = buildHeading(outcome, testRunId);
        String details = "Execution: " + notification.executionId();
        Context context = new Context();
        context.setVariable("color", outcome.color());
        context.setVariable("heading", heading);
        context.setVariable("details", details);
        context.setVariable("hasResult", notification.resultUri()
                                                     .isPresent());
        context.setVariable("resultUri", getResultUri(notification));
        context.setVariable("linkResult", getLinkToResultFlag(notification));
        String html = TEMPLATES.process("mail_body", context);
        Result result = new Result(outcome, heading, details, html);
        return new MailContent(buildMailSubject(testRunId, result.outcome()),
                buildMailText(result.heading(), result.details(), resultText), result.html());
    }

    private record Result(Outcome outcome, String heading, String details, String html) {
    }

    private static String buildHeading(Outcome outcome, long testRunId) {
        return outcome.label() + " — Trails run #" + testRunId;
    }

    private static String buildMailSubject(long testRunId, Outcome outcome) {
        String resultLabel = outcome.label()
                                    .toUpperCase(Locale.ROOT);
        return "[" + resultLabel + "] Trails run #" + testRunId;
    }

    private static String buildMailText(String heading, String details, String resultText) {
        return heading + "\n" + details + resultText;
    }

    private static Boolean getLinkToResultFlag(TerminalRunNotification notification) {
        return notification.resultUri()
                           .map(MailContent::isWebUri)
                           .orElse(false);
    }

    private static String getResultUri(TerminalRunNotification notification) {
        return notification.resultUri()
                           .map(URI::toString)
                           .orElse("");
    }

    private static boolean isWebUri(URI uri) {
        return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                && uri.getHost() != null;
    }

    private static TemplateEngine templates() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/trails-notifier-smtp/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private static Outcome outcome(TerminalRunNotification notification) {
        if (notification.status() == TestRunTerminalStatus.FAILED) {
            return new Outcome("Execution failed", "#b3261e");
        }
        return switch (notification.resultIndicator()) {
            case SUCCESS -> new Outcome("Passed", "#137333");
            case PARTIAL_SUCCESS -> new Outcome("Partial success", "#8a4b00");
            case FAILURE -> new Outcome("Failed", "#b3261e");
        };
    }

    private record Outcome(String label, String color) {
    }
}
