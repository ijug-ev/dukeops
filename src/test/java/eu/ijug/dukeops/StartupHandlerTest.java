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
package eu.ijug.dukeops;

import eu.ijug.dukeops.domain.user.control.UserService;
import eu.ijug.dukeops.domain.user.entity.UserDto;
import eu.ijug.dukeops.domain.user.entity.UserRole;
import eu.ijug.dukeops.infra.communication.mail.MailConfig;
import eu.ijug.dukeops.infra.config.AppConfig;
import eu.ijug.dukeops.infra.config.InstanceConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StartupHandlerTest {

    @Test
    void shouldCreateAdminIfNoneExistsAndEmailIsSet() {
        final var appConfig = createAppConfig("admin@example.com");
        final var userService = mockUserService("");

        final var startupHandler = new StartupHandler(appConfig, userService);
        startupHandler.onApplicationReady();

        verify(userService).storeUser(argThat(user ->
                user.email().equals("admin@example.com") &&
                        user.role() == UserRole.ADMIN
        ));
    }

    @Test
    void shouldSkipCreationIfAdminAlreadyExists() {
        final var appConfig = createAppConfig("admin@example.com");
        final var userService = mockUserService("admin@example.com");

        final var startupHandler = new StartupHandler(appConfig, userService);
        startupHandler.onApplicationReady();

        verify(userService, never()).storeUser(any());
    }

    @Test
    void shouldSkipCreationIfNoEmailSet() {
        final var appConfig = createAppConfig("");
        final var userService = mockUserService("admin@example.com");

        final var startupHandler = new StartupHandler(appConfig, userService);
        startupHandler.onApplicationReady();

        verify(userService, never()).storeUser(any());
    }

    private UserService mockUserService(final @NotNull String adminEmail) {
        final Optional<UserDto> adminUser = adminEmail.isBlank() ?
                Optional.empty() :
                Optional.of(
                        new UserDto(null, null, null,
                        "Instance Admin", adminEmail, UserRole.ADMIN));
        final var userService = mock(UserService.class);
        when(userService.getUserByEmail(adminEmail)).thenReturn(adminUser);
        return userService;
    }

    private AppConfig createAppConfig(final @NotNull String adminEmail) {
        final var version = "0.0.0";
        final var buildTime = "0000-00-00";
        final var baseUrl = "http://localhost:8080";
        final var instanceConfig = new InstanceConfig(adminEmail);
        final var mailConfig = new MailConfig("", "");
        return new AppConfig(version, buildTime, baseUrl, mailConfig, instanceConfig);
    }

}
