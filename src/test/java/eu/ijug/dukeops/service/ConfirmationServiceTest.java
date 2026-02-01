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

import eu.ijug.dukeops.entity.UserDto;
import eu.ijug.dukeops.entity.UserRole;
import eu.ijug.dukeops.i18n.TranslationProvider;
import eu.ijug.dukeops.util.LinkUtil;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfirmationServiceTest {

    @Test
    void dontSendConfirmationEmailToUnknownUser() {
        final var userService = mock(UserService.class);
        when(userService.getUserByEmail(any())).thenReturn(Optional.empty());

        final var confirmationService = new ConfirmationService(
                mock(MailService.class),
                mock(AuthenticationService.class),
                userService,
                mock(TranslationProvider.class)
        );

        try (var logCaptor = LogCaptor.forClass(ConfirmationService.class)) {
            confirmationService.sendConfirmationMail(Locale.ENGLISH, "unknown-user@example.com");
            assertThat(logCaptor.getWarnLogs())
                    .containsExactly("User with email 'unknown-user@example.com' not found.");
        }
    }

    @Test
    void confirmShouldFailWithNonExistingConfirmationId() {
        final var confirmationService = new ConfirmationService(
                mock(MailService.class),
                mock(AuthenticationService.class),
                mock(UserService.class),
                mock(TranslationProvider.class));

        final var result = confirmationService.confirmAndLogin(UUID.randomUUID().toString());
        assertThat(result).isFalse();
    }

    @Test
    void loginShouldFailWhenAuthenticationServiceFails() {
        final var authenticationService = mock(AuthenticationService.class);
        when(authenticationService.login(any())).thenReturn(false);

        final var translationProvider = mock(TranslationProvider.class);
        when(translationProvider.getTranslation(
                eq("service.ConfirmationService.email.subject"), any()))
                .thenReturn("Please confirm your email address");
        when(translationProvider.getTranslation(
                eq("service.ConfirmationService.email.message"), any(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    final var confirmationLink = invocation.getArgument(2, String.class);
                    final var validForValue = invocation.getArgument(3, String.class);
                    return "{0}\nValid for {1}"
                            .replace("{0}", confirmationLink)
                            .replace("{1}", validForValue);
                });
        when(translationProvider.getTranslation(
                eq("service.ConfirmationService.timeout"), any(), any()))
                .thenReturn("5 minutes");

        final var confirmationIdRef = new AtomicReference<String>();
        final var mailService = mock(MailService.class);
        doAnswer(invocation -> {
            final var message = invocation.getArgument(2, String.class);

            final var lines = message.split("\n");
            final var confirmationLink = lines[0].trim();
            final var confirmationId = confirmationLink.substring(
                    confirmationLink.lastIndexOf('=') + 1
            );

            confirmationIdRef.set(confirmationId);
            return null;
        }).when(mailService).sendMail(any(), any(), any());

        final var testUser = new UserDto(UUID.randomUUID(), null,null,"Test User", "test@example.com", UserRole.USER);
        final var userService = mock(UserService.class);
        when(userService.getUserByEmail(any())).thenReturn(Optional.of(testUser));

        LinkUtil.setBaseUrl("http://localhost:8080");

        final var confirmationService = new ConfirmationService(
                mailService,
                authenticationService,
                userService,
                translationProvider);
        confirmationService.sendConfirmationMail(Locale.ENGLISH, "test@example.com");

        final var confirmationId = confirmationIdRef.get();
        assertThat(confirmationId).isNotBlank();

        final var result = confirmationService.confirmAndLogin(confirmationId);
        assertThat(result).isFalse();
    }

}
