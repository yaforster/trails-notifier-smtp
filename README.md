# Trails SMTP Notifier

`io.github.yaforster:trails-notifier-smtp` supplies an asynchronous SMTP notifier for persisted terminal Trails test
runs. This SMTP notifier implements the Notifier interface to send out Mails after a test run. Failed attempts at 
sending Mails will not alter the result of the persisted test run result or the results of other notifiers.

## Customer wrapper

Align this adapter with he `trails-bom` version in your wrapper application.
The notifier uses only the `trails.notifier.smtp` configuration namespace.

```yaml
trails:
  notifier:
    smtp:
      host: smtp.example.test
      port: 587
      sender: trails@example.test
      tls: starttls
      authentication:
        username: trails
        password: ${TRAILS_NOTIFIER_SMTP_AUTHENTICATION_PASSWORD}
      connection-timeout: 10s
      read-timeout: 10s
      write-timeout: 10s
      # Optional static recipients. Request-scoped recipients are added at execution time.
      recipients:
        - somebody@example.test
```

`host`, `port`, `sender`, and `tls` are required. `tls: starttls` requires server-advertised STARTTLS and certificate
verification; `tls: implicit` starts TLS with the connection, normally on port 465. Plaintext SMTP is unsupported.
Authentication is optional, but its username and password must be configured together. Every timeout is positive and
defaults to ten seconds when omitted.

Static and request-scoped recipients are canonically merged. The adapter sends nothing when their union is empty. It
never logs recipients, message content, SMTP response text, or credentials. Messages contain only the outcome,
the persisted test-run ID, the execution ID, and an optional public result URI.

## Email presentation

Edit `src/main/resources/templates/trails-notifier-smtp/mail_body.html` to change the email markup and inline styles.
The adapter renders this classpath resource with Thymeleaf, independently of the host application's template engine.
Standard HTML provides IDE editing and preview support; `th:text`, `th:href`, and `th:if` supply escaped notification
values and conditional result links at runtime. Rebuild the adapter after template changes.

Each email includes UTF-8 plain-text and HTML alternatives. The compact HTML uses inline styling, textual outcome
labels, and no images or external resources; it remains readable with CSS disabled. Plain-text clients receive the
same summary and result URI. Absolute HTTP(S) result URIs become links; other URIs remain visible as text.

Subjects lead with `[PASSED]`, `[PARTIAL SUCCESS]`, `[FAILED]`, or `[EXECUTION FAILED]`, followed by `Trails run #xxx`.
The body repeats the outcome prominently (green, amber, or red), offers a result link when available, and shows the
execution ID below. Only the persisted `SUCCESS` result is labeled Passed: a completed execution can still have
failed or partially successful test actions. Host failures are explicitly labeled Execution failed.

This notifier requires the SDK notification contract containing `resultIndicator`; rebuild the SDK and notifier together.
