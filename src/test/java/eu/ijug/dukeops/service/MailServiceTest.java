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
package eu.ijug.dukeops.service;

import eu.ijug.dukeops.config.AppConfig;
import eu.ijug.dukeops.config.MailConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MailServiceTest {

    private MailConfig mailConfig;
    private JavaMailSenderImpl mailSender;
    private MailService mailService;

    @BeforeEach
    void setUp() {
        mailConfig = mock(MailConfig.class);
        when(mailConfig.from()).thenReturn("sender@example.com");

        final var appConfig = mock(AppConfig.class);
        when(appConfig.mail()).thenReturn(mailConfig);

        mailSender = mock(JavaMailSenderImpl.class);
        when(mailSender.createMimeMessage()).thenCallRealMethod();

        mailService = new MailService(appConfig, mailSender);
    }

    @Test
    void sendMailWithReplyTo() throws MessagingException, IOException {
        when(mailConfig.replyTo()).thenReturn("reply-to@example.com");

        final var mimeMessageRef = new AtomicReference<MimeMessage>();
        doAnswer(invocation -> {
            final var mimeMessage = invocation.getArgument(0, MimeMessage.class);
            mimeMessageRef.set(mimeMessage);
            return null;
        }).when(mailSender).send(any(MimeMessage.class));

        try (var logCaptor = LogCaptor.forClass(MailService.class)) {
            mailService.sendMail("test@example.com", "Test Subject", "Test Text");
            assertThat(logCaptor.getInfoLogs()).containsExactly(
                    "Mail with subject 'Test Subject' successfully sent to 'test@example.com'");
        }

        final var mimeMessage = mimeMessageRef.get();
        assertThat(mimeMessage).isNotNull();
        assertThat(mimeMessage.getFrom()).hasSize(1);
        assertThat(mimeMessage.getFrom()[0].toString()).isEqualTo("sender@example.com");
        assertThat(mimeMessage.getAllRecipients()).hasSize(1);
        assertThat(mimeMessage.getAllRecipients()[0].toString()).isEqualTo("test@example.com");
        assertThat(mimeMessage.getReplyTo()).hasSize(1);
        assertThat(mimeMessage.getReplyTo()[0].toString()).isEqualTo("reply-to@example.com");
        assertThat(mimeMessage.getSubject()).isEqualTo("Test Subject");
        assertThat(mimeMessage.getContent()).isEqualTo("Test Text");
    }

    @Test
    void sendMailWithoutReplyTo() throws MessagingException, IOException {
        when(mailConfig.replyTo()).thenReturn("");

        final var mimeMessageRef = new AtomicReference<MimeMessage>();
        doAnswer(invocation -> {
            final var mimeMessage = invocation.getArgument(0, MimeMessage.class);
            mimeMessageRef.set(mimeMessage);
            return null;
        }).when(mailSender).send(any(MimeMessage.class));

        try (var logCaptor = LogCaptor.forClass(MailService.class)) {
            mailService.sendMail("test@example.com", "Test Subject", "Test Text");
            assertThat(logCaptor.getInfoLogs()).containsExactly(
                    "Mail with subject 'Test Subject' successfully sent to 'test@example.com'");
        }

        final var mimeMessage = mimeMessageRef.get();
        assertThat(mimeMessage).isNotNull();
        assertThat(mimeMessage.getFrom()).hasSize(1);
        assertThat(mimeMessage.getFrom()[0].toString()).isEqualTo("sender@example.com");
        assertThat(mimeMessage.getAllRecipients()).hasSize(1);
        assertThat(mimeMessage.getAllRecipients()[0].toString()).isEqualTo("test@example.com");
        assertThat(mimeMessage.getReplyTo()).hasSize(1);
        assertThat(mimeMessage.getReplyTo()[0].toString()).isEqualTo("sender@example.com");
        assertThat(mimeMessage.getSubject()).isEqualTo("Test Subject");
        assertThat(mimeMessage.getContent()).isEqualTo("Test Text");
    }

    @Test
    void testExceptionHandling() {
        doThrow(new RuntimeException("Test Exception")).when(mailConfig).replyTo();

        final var mimeMessageRef = new AtomicReference<MimeMessage>();
        doAnswer(invocation -> {
            final var mimeMessage = invocation.getArgument(0, MimeMessage.class);
            mimeMessageRef.set(mimeMessage);
            return null;
        }).when(mailSender).send(any(MimeMessage.class));

        try (var logCaptor = LogCaptor.forClass(MailService.class)) {
            mailService.sendMail("test@example.com", "Test Subject", "Test Text");
            assertThat(logCaptor.getErrorLogs()).containsExactly(
                    "Unable to send mail with subject 'Test Subject' to 'test@example.com': Test Exception");
        }

        assertThat(mimeMessageRef.get()).isNull();
    }

}
