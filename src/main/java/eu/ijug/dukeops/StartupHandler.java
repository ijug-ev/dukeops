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
import eu.ijug.dukeops.infra.config.AppConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * <p>Handles application startup tasks that need to be executed once the Spring Boot application
 * has fully initialized.</p>
 *
 * <p>This component listens for the {@link ApplicationReadyEvent} and performs initialization
 * logic that depends on a fully available application context, such as database access and
 * configuration resolution.</p>
 */
@Component
public class StartupHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartupHandler.class);

    private final @NotNull AppConfig appConfig;
    private final @NotNull UserService userService;

    /**
     * <p>Creates a new {@code StartupHandler} with the required configuration and services.</p>
     *
     * @param appConfig the application configuration providing instance-level settings
     * @param userService the service used to manage user persistence and lookup
     */
    public StartupHandler(final @NotNull AppConfig appConfig,
                          final @NotNull UserService userService) {
        this.appConfig = appConfig;
        this.userService = userService;
    }


    /**
     * <p>Callback method that is invoked once the Spring Boot application has fully started.</p>
     *
     * <p>This method delegates to initialization routines that must only run after the
     * application context, database connections, and all managed beans are ready.</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        createInitialAdmin();
    }

    /**
     * <p>Creates an initial administrator account if one is configured and does not already exist.</p>
     *
     * <p>The administrator email address is read from the application configuration, normalized
     * (trimmed and lowercased), and then checked against existing users. If no user with this
     * email exists, a new administrator account is created.</p>
     *
     * <p>If the configuration does not define an administrator email or the email is blank,
     * this method performs no action.</p>
     */
    private void createInitialAdmin() {
        final var adminEmails = appConfig.instance().admins().trim().toLowerCase(Locale.getDefault()).split(",");
        for (final String adminEmail : adminEmails) {
            if (adminEmail.isBlank()) {
                LOGGER.debug("No administrator email configured, skipping admin user creation.");
                continue;
            }
            final var existingUser = userService.getUserByEmail(adminEmail);
            if (existingUser.isPresent()) {
                final var user = existingUser.orElseThrow();
                if (user.role().equals(UserRole.ADMIN)) {
                    LOGGER.info("Admin user with email '{}' already exists, skipping creation.", adminEmail);
                    continue;
                }
                final var adminUser = new UserDto(user.id(), user.created(), user.updated(),
                        user.name(), adminEmail, UserRole.ADMIN);
                userService.storeUser(adminUser);
                LOGGER.warn("Existing user with email '{}' nominated as admin.", adminEmail);
                continue;
            }
            final var adminUser = new UserDto(null, null, null,
                    "Instance Admin", adminEmail, UserRole.ADMIN);
            userService.storeUser(adminUser);
            LOGGER.info("Admin user with email '{}' created as requested.", adminEmail);
        }
    }

}
