/*
 * DukeOps - iJUG Self-Service Portal
 * Copyright (C) Marcus Fihlon and the individual contributors to DukeOps.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package eu.ijug.dukeops.infra.communication.mail;

import eu.ijug.dukeops.infra.config.AppConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public final class MailService {

    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(MailService.class);

    private final @NotNull MailConfig mailConfig;
    private final @NotNull JavaMailSender mailSender;

    public MailService(final @NotNull AppConfig appConfig,
                       final @NotNull JavaMailSender mailSender) {
        super();
        this.mailConfig = appConfig.mail();
        this.mailSender = mailSender;
    }

    public void sendMail(final @NotNull String email,
                         final @NotNull String subject,
                         final @NotNull String text) {
        try {
            final var mimeMessage = mailSender.createMimeMessage();
            final var helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());

            helper.setTo(email);
            helper.setFrom(mailConfig.from());

            final var replyTo = mailConfig.replyTo();
            if (!replyTo.isBlank()) {
                helper.setReplyTo(replyTo);
            }

            helper.setSubject(subject);
            helper.setText(text, false);

            mailSender.send(mimeMessage);

            LOGGER.info("Mail with subject '{}' successfully sent to '{}'",
                    subject, email);
        } catch (final Exception e) {
            LOGGER.error("Unable to send mail with subject '{}' to '{}': {}",
                    subject, email, e.getMessage());
        }
    }
}
