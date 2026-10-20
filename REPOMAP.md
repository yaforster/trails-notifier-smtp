# Trails SMTP Notifier repository map

```text
adapter/trails-notifier-smtp/
|-- pom.xml                                Artifact, mail/Thymeleaf dependencies, and build checks
|-- format/java-formatter.xml              Java formatting rules
|-- DEVELOPMENT.md                         Build, formatting, and Sonatype release instructions
|-- src/
|   |-- main/
|   |   |-- java/.../notifier/smtp/
|   |   |   |-- MailNotifier.java           Recipient merging and multipart SMTP delivery
|   |   |   |-- MailContent.java            Outcome labels, plain text, and Thymeleaf rendering
|   |   |   |-- MailSenderFactory.java      TLS-protected JavaMail sender configuration
|   |   |   |-- MailProperties.java         Validated SMTP settings and timeouts
|   |   |   |-- Authentication.java         SMTP credentials
|   |   |   |-- TLSMode.java                Supported TLS modes
|   |   |   `-- MailNotifierAutoConfiguration.java  Boot adapter registration
|   |   `-- resources/
|   |       |-- META-INF/spring/            Boot auto-configuration import
|   |       `-- templates/trails-notifier-smtp/
|   |           `-- terminal-run.html      Email markup, inline styles, and conditional result link
|   `-- test/java/.../notifier/smtp/         Settings, TLS, MIME alternatives, and context tests
|-- README.md                              Customer configuration guidance
`-- REPOMAP.md                             This map
```

The notifier owns only TLS-protected SMTP delivery and its `trails.notifier.smtp` configuration.

Email presentation is defined by `templates/trails-notifier-smtp/mail_body.html`. `MailContent` supplies escaped
values for Thymeleaf and creates the equivalent plain-text summary for clients that do not support styled mails.
`MailNotifier` assembles both alternatives into one message.
The template is loaded from the adapter classpath independently of the host template engine.
