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
package eu.ijug.dukeops.domain.authentication.control;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import eu.ijug.dukeops.i18n.TranslationProvider;
import eu.ijug.dukeops.service.MailService;
import eu.ijug.dukeops.service.UserService;
import eu.ijug.dukeops.util.LinkUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public final class ConfirmationService {

    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(ConfirmationService.class);
    private static final @NotNull Duration CONFIRMATION_TIMEOUT = Duration.ofMinutes(5);
    private static final @NotNull String CONFIRMATION_PATH = "/confirm";

    private final @NotNull MailService mailService;
    private final @NotNull AuthenticationService authenticationService;
    private final @NotNull UserService userService;
    private final @NotNull TranslationProvider translationProvider;

    public ConfirmationService(final @NotNull MailService mailService,
                               final @NotNull AuthenticationService authenticationService,
                               final @NotNull UserService userService,
                               final @NotNull TranslationProvider translationProvider) {
        super();
        this.mailService = mailService;
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.translationProvider = translationProvider;
    }

    private final @NotNull Cache<@NotNull String, @NotNull ConfirmationData> confirmationCache = Caffeine.newBuilder()
            .expireAfterWrite(CONFIRMATION_TIMEOUT)
            .maximumSize(1_000) // prevent memory overflow (DDOS attack)
            .build();

    public void sendConfirmationMail(final @NotNull Locale locale,
                                     final @NotNull String email) {
        if (userService.getUserByEmail(email).isEmpty()) {
            LOGGER.warn("User with email '{}' not found.", email);
            return;
        }

        final var confirmationId = UUID.randomUUID().toString();
        final var confirmationData = new ConfirmationData(confirmationId, email);
        confirmationCache.put(confirmationId, confirmationData);

        final var confirmationLink = generateConfirmationLink(confirmationData);
        final var confirmationTimeout = getConfirmationTimeoutText(locale);

        final var subject = translationProvider.getTranslation("service.ConfirmationService.email.subject", locale);
        final var message = translationProvider.getTranslation("service.ConfirmationService.email.message", locale,
                confirmationLink, confirmationTimeout);

        mailService.sendMail(email, subject, message);
    }

    private @NotNull String generateConfirmationLink(final @NotNull ConfirmationData confirmationData) {
        return UriComponentsBuilder.fromUriString(LinkUtil.getBaseUrl())
                .path(CONFIRMATION_PATH)
                .queryParam("id", confirmationData.id())
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }

    public @NotNull String getConfirmationTimeoutText(final @Nullable Locale locale) {
        final var params = Map.of("timeout", CONFIRMATION_TIMEOUT.toMinutes());
        return translationProvider.getTranslation(
                "service.ConfirmationService.timeout", locale, params);
    }

    public boolean confirmAndLogin(final @NotNull String confirmationId) {
        final var confirmationData = confirmationCache.getIfPresent(confirmationId);
        if (confirmationData != null) {
            final var email = confirmationData.email();
            if (authenticationService.login(email)) {
                confirmationCache.invalidate(confirmationId);
                return true;
            }
        }
        return false;
    }

    private record ConfirmationData(
            @NotNull String id,
            @NotNull String email) { }
}
