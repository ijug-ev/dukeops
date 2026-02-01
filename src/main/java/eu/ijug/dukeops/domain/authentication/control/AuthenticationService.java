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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import eu.ijug.dukeops.SecurityConfig;
import eu.ijug.dukeops.domain.authentication.entity.AuthenticationSignal;
import eu.ijug.dukeops.domain.authentication.entity.UserPrincipal;
import eu.ijug.dukeops.domain.user.control.UserService;
import eu.ijug.dukeops.domain.user.entity.UserDto;
import eu.ijug.dukeops.domain.user.entity.UserRole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

@Service
public final class AuthenticationService {

    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(AuthenticationService.class);

    private final @NotNull UserService userService;
    private final @NotNull AuthenticationSignal authenticationSignal;

    public AuthenticationService(final @NotNull UserService userService,
                                 final @NotNull AuthenticationSignal authenticationSignal) {
        super();
        this.userService = userService;
        this.authenticationSignal = authenticationSignal;
    }

    public boolean login(final @NotNull String email) {
        final var optUser = userService.getUserByEmail(email);
        if (optUser.isEmpty()) {
            LOGGER.warn("User with email '{}' not found.", email);
            return false;
        }

        final var user = optUser.orElseThrow();
        final var roles = new ArrayList<GrantedAuthority>();
        roles.add(new SimpleGrantedAuthority(UserRole.USER.getRole()));
        if (user.role().equals(UserRole.ADMIN)) {
            roles.add(new SimpleGrantedAuthority(UserRole.ADMIN.getRole()));
        }

        final var authorities = Collections.unmodifiableList(roles);
        final var principal = new UserPrincipal(user, authorities);


        // Authentication-Token without password (passwordless)
        final var authentication = new PreAuthenticatedAuthenticationToken(principal, null, authorities);

        // create and set SecurityContext
        final var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // persist in HTTP session
        final var request = VaadinService.getCurrentRequest();
        final var response = VaadinService.getCurrentResponse();
        if (request instanceof VaadinServletRequest vaadinServletRequest
                && response instanceof VaadinServletResponse vaadinServletResponse) {
            final var httpServletRequest = vaadinServletRequest.getHttpServletRequest();
            final var httpServletResponse = vaadinServletResponse.getHttpServletResponse();
            new HttpSessionSecurityContextRepository().saveContext(context, httpServletRequest, httpServletResponse);
        } else {
            // fallback: should never happen in Vaadin UI context
            LOGGER.warn("No Vaadin servlet request/response available; SecurityContext not saved to session.");
        }

        LOGGER.info("User with email '{}' successfully logged in.", email);
        authenticationSignal.setAuthenticated(true, UserRole.ADMIN.equals(user.role()));

        return true;
    }

    /**
     * <p>Returns the current {@link Authentication} object if the user is authenticated.</p>
     *
     * @return an {@link Authentication} object or {@code null} if the user is not authenticated
     */
    private static @Nullable Authentication getAuthentication() {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() ? authentication : null;
    }

    /**
     * <p>Returns the current {@link UserPrincipal} if the user is authenticated.</p>
     *
     * <p>Anonymous users and non-{@code UserPrincipal} principals yield an empty Optional.</p>
     *
     * @return an {@link Optional} containing the current {@link UserPrincipal} if available
     */
    private static @NotNull Optional<UserPrincipal> getUserPrincipal() {
        final var authentication = getAuthentication();
        if (authentication != null) {
            final var principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal) {
                return Optional.of((UserPrincipal) principal);
            }
        }
        return Optional.empty();
    }

    public @NotNull Optional<UserDto> getLoggedInUser() {
        return getUserPrincipal().map(UserPrincipal::getUser);
    }

    public boolean isUserLoggedIn() {
        return getUserPrincipal().isPresent();
    }

    public void logout(final @NotNull UI ui) {
        logout(ui, SecurityConfig.LOGOUT_SUCCESS_URL);
    }

    public void logout(final @NotNull UI ui,
                       final @NotNull String location) {
        final var user = getLoggedInUser().orElse(null);
        if (user == null) {
            LOGGER.warn("No authenticated user found; logout skipped.");
        } else {
            final var email = user.email();
            authenticationSignal.setAuthenticated(false, false);
            SecurityContextHolder.clearContext();
            final var logoutHandler = new SecurityContextLogoutHandler();
            logoutHandler.logout(VaadinServletRequest.getCurrent().getHttpServletRequest(), null, null);
            LOGGER.info("User with email '{}' successfully logged out.", email);
        }
        ui.getPage().setLocation(location);
    }

}
