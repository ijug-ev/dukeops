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
import eu.ijug.dukeops.domain.user.control.UserService;
import eu.ijug.dukeops.infra.communication.mail.MailService;
import eu.ijug.dukeops.infra.ui.vaadin.i18n.TranslationProvider;
import eu.ijug.dukeops.infra.ui.vaadin.control.LinkUtil;
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

/**
 * <p>Spring-managed service responsible for handling email-based login confirmations.</p>
 *
 * <p>The service generates time-limited confirmation links, sends localized confirmation emails,
 * and validates confirmation requests to authenticate users.</p>
 */
@Service
public class ConfirmationService {

    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(ConfirmationService.class);
    private static final @NotNull Duration CONFIRMATION_TIMEOUT = Duration.ofMinutes(5);
    private static final @NotNull String CONFIRMATION_PATH = "/confirm";

    private final @NotNull MailService mailService;
    private final @NotNull AuthenticationService authenticationService;
    private final @NotNull UserService userService;
    private final @NotNull TranslationProvider translationProvider;

    /**
     * <p>Creates a new confirmation service using the required collaborators.</p>
     *
     * @param mailService the mail service used to send confirmation emails
     * @param authenticationService the authentication service used to log in users
     * @param userService the user service used to resolve users by email address
     * @param translationProvider the translation provider used to localize email content
     */
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

    /**
     * <p>In-memory cache storing confirmation data for pending login confirmations.</p>
     *
     * <p>Entries expire automatically after a fixed timeout to limit the validity of confirmation
     * links and to protect against abuse.</p>
     */
    private final @NotNull Cache<@NotNull String, @NotNull ConfirmationData> confirmationCache = Caffeine.newBuilder()
            .expireAfterWrite(CONFIRMATION_TIMEOUT)
            .maximumSize(1_000) // prevent memory overflow (DDOS attack)
            .build();

    /**
     * <p>Sends a login confirmation email to the specified user.</p>
     *
     * <p>If no user with the given email address exists, the method logs a warning and returns
     * without sending an email.</p>
     *
     * @param locale the locale used to localize the email content
     * @param email the email address of the user to send the confirmation to
     */
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

    /**
     * <p>Generates an absolute confirmation link for the given confirmation data.</p>
     *
     * @param confirmationData the confirmation data containing the identifier
     * @return a URL pointing to the confirmation endpoint
     */
    private @NotNull String generateConfirmationLink(final @NotNull ConfirmationData confirmationData) {
        return UriComponentsBuilder.fromUriString(LinkUtil.getBaseUrl())
                .path(CONFIRMATION_PATH)
                .queryParam("id", confirmationData.id())
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }

    /**
     * <p>Returns a localized text describing the confirmation timeout duration.</p>
     *
     * <p>The text is intended for inclusion in confirmation emails and UI messages.</p>
     *
     * @param locale the locale to use, or {@code null} to fall back to the default locale
     * @return a localized timeout description
     */
    public @NotNull String getConfirmationTimeoutText(final @Nullable Locale locale) {
        final var params = Map.of("timeout", CONFIRMATION_TIMEOUT.toMinutes());
        return translationProvider.getTranslation(
                "service.ConfirmationService.timeout", locale, params);
    }

    /**
     * <p>Validates the given confirmation identifier and logs in the associated user.</p>
     *
     * <p>If the confirmation identifier is valid and the login succeeds, the confirmation entry
     * is removed from the cache to prevent reuse.</p>
     *
     * @param confirmationId the confirmation identifier received from the confirmation link
     * @return {@code true} if the confirmation was valid and the user was logged in successfully,
     *         {@code false} otherwise
     */
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

    /**
     * <p>Internal data structure holding confirmation identifiers and associated email addresses.</p>
     */
    private record ConfirmationData(
            @NotNull String id,
            @NotNull String email) { }
}
