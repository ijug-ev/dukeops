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
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import eu.ijug.dukeops.domain.authentication.entity.AuthenticationSignal;
import eu.ijug.dukeops.domain.authentication.entity.UserPrincipal;
import eu.ijug.dukeops.domain.user.control.UserService;
import eu.ijug.dukeops.domain.user.entity.UserDto;
import eu.ijug.dukeops.domain.user.entity.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest {

    private UserService userService;
    private UserDto userDto;

    private VaadinServletRequest vaadinServletRequest;
    private VaadinServletResponse vaadinServletResponse;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        userDto = mock(UserDto.class);

        vaadinServletRequest = mock(VaadinServletRequest.class);
        when(vaadinServletRequest.getHttpServletRequest()).thenReturn(mock(HttpServletRequest.class));
        vaadinServletResponse = mock(VaadinServletResponse.class);
        when(vaadinServletResponse.getHttpServletResponse()).thenReturn(mock(HttpServletResponse.class));

        final var authenticationSignal = mock(AuthenticationSignal.class);
        authenticationService = new AuthenticationService(userService, authenticationSignal);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginShouldFailWithNonExistingUser() {
        when(userService.getUserByEmail(anyString())).thenReturn(Optional.empty());

        try (var logCaptor = LogCaptor.forClass(AuthenticationService.class)) {
            final var result = authenticationService.login("non-existing-user@example.com");
            assertThat(result).isFalse();
            assertThat(logCaptor.getWarnLogs()).containsExactly(
                    "User with email 'non-existing-user@example.com' not found.");
        }

        final var securityContext = SecurityContextHolder.getContext();
        final var authentication = securityContext.getAuthentication();
        assertThat(authentication).isNull();

        final var loggedInUser = authenticationService.getLoggedInUser();
        assertThat(loggedInUser).isEmpty();
    }

    @Test
    void loginShouldSucceedWithExistingUserWithoutRequest() {
        when(userDto.name()).thenReturn("Test User");
        when(userDto.email()).thenReturn("user@example.com");
        when(userDto.role()).thenReturn(UserRole.USER);
        when(userService.getUserByEmail("user@example.com")).thenReturn(Optional.of(userDto));

        try (var logCaptor = LogCaptor.forClass(AuthenticationService.class)) {
            final var result = authenticationService.login("user@example.com");
            assertThat(result).isTrue();
            assertThat(logCaptor.getWarnLogs()).containsExactly(
                    "No Vaadin servlet request/response available; SecurityContext not saved to session.");
            assertThat(logCaptor.getInfoLogs()).containsExactly(
                    "User with email 'user@example.com' successfully logged in.");
        }

        final var securityContext = SecurityContextHolder.getContext();
        final var authentication = securityContext.getAuthentication();
        final var authorities = authentication.getAuthorities();
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER");

        final var loggedInUser = authenticationService.getLoggedInUser().orElseThrow();
        assertThat(loggedInUser.name()).isEqualTo("Test User");
        assertThat(loggedInUser.email()).isEqualTo("user@example.com");
        assertThat(loggedInUser.role()).isEqualTo(UserRole.USER);
    }

    @Test
    void loginShouldSucceedWithExistingUserWithRequestAndResponse() {
        when(userDto.name()).thenReturn("Test User");
        when(userDto.email()).thenReturn("user@example.com");
        when(userDto.role()).thenReturn(UserRole.USER);
        when(userService.getUserByEmail("user@example.com")).thenReturn(Optional.of(userDto));

        try (var vaadinService = mockStatic(VaadinService.class);
             var logCaptor = LogCaptor.forClass(AuthenticationService.class)) {

            vaadinService.when(VaadinService::getCurrentRequest).thenReturn(vaadinServletRequest);
            vaadinService.when(VaadinService::getCurrentResponse).thenReturn(vaadinServletResponse);

            final var result = authenticationService.login("user@example.com");
            assertThat(result).isTrue();
            assertThat(logCaptor.getWarnLogs()).isEmpty();
            assertThat(logCaptor.getInfoLogs()).containsExactly(
                    "User with email 'user@example.com' successfully logged in.");
        }

        final var securityContext = SecurityContextHolder.getContext();
        final var authentication = securityContext.getAuthentication();
        final var authorities = authentication.getAuthorities();
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER");

        final var loggedInUser = authenticationService.getLoggedInUser().orElseThrow();
        assertThat(loggedInUser.name()).isEqualTo("Test User");
        assertThat(loggedInUser.email()).isEqualTo("user@example.com");
        assertThat(loggedInUser.role()).isEqualTo(UserRole.USER);
    }

    @Test
    void loginShouldSucceedWithExistingUserWithRequestWithoutResponse() {
        when(userDto.name()).thenReturn("Test User");
        when(userDto.email()).thenReturn("user@example.com");
        when(userDto.role()).thenReturn(UserRole.USER);
        when(userService.getUserByEmail("user@example.com")).thenReturn(Optional.of(userDto));

        try (var vaadinService = mockStatic(VaadinService.class);
             var logCaptor = LogCaptor.forClass(AuthenticationService.class)) {

            vaadinService.when(VaadinService::getCurrentRequest).thenReturn(vaadinServletRequest);
            vaadinService.when(VaadinService::getCurrentResponse).thenReturn(null);

            final var result = authenticationService.login("user@example.com");
            assertThat(result).isTrue();
            assertThat(logCaptor.getWarnLogs()).containsExactly(
                    "No Vaadin servlet request/response available; SecurityContext not saved to session.");
            assertThat(logCaptor.getInfoLogs()).containsExactly(
                    "User with email 'user@example.com' successfully logged in.");
        }

        final var securityContext = SecurityContextHolder.getContext();
        final var authentication = securityContext.getAuthentication();
        final var authorities = authentication.getAuthorities();
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER");

        final var loggedInUser = authenticationService.getLoggedInUser().orElseThrow();
        assertThat(loggedInUser.name()).isEqualTo("Test User");
        assertThat(loggedInUser.email()).isEqualTo("user@example.com");
        assertThat(loggedInUser.role()).isEqualTo(UserRole.USER);
    }

    @Test
    void loginShouldSucceedWithExistingUserWithoutRequestWithResponse() {
        when(userDto.name()).thenReturn("Test User");
        when(userDto.email()).thenReturn("user@example.com");
        when(userDto.role()).thenReturn(UserRole.USER);
        when(userService.getUserByEmail("user@example.com")).thenReturn(Optional.of(userDto));

        try (var vaadinService = mockStatic(VaadinService.class);
             var logCaptor = LogCaptor.forClass(AuthenticationService.class)) {

            vaadinService.when(VaadinService::getCurrentRequest).thenReturn(null);
            vaadinService.when(VaadinService::getCurrentResponse).thenReturn(vaadinServletResponse);

            final var result = authenticationService.login("user@example.com");
            assertThat(result).isTrue();
            assertThat(logCaptor.getWarnLogs()).containsExactly(
                    "No Vaadin servlet request/response available; SecurityContext not saved to session.");
            assertThat(logCaptor.getInfoLogs()).containsExactly(
                    "User with email 'user@example.com' successfully logged in.");
        }

        final var securityContext = SecurityContextHolder.getContext();
        final var authentication = securityContext.getAuthentication();
        final var authorities = authentication.getAuthorities();
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER");

        final var loggedInUser = authenticationService.getLoggedInUser().orElseThrow();
        assertThat(loggedInUser.name()).isEqualTo("Test User");
        assertThat(loggedInUser.email()).isEqualTo("user@example.com");
        assertThat(loggedInUser.role()).isEqualTo(UserRole.USER);
    }

    @Test
    void loginShouldSucceedWithExistingAdminWithoutRequest() {
        when(userDto.name()).thenReturn("Test Admin");
        when(userDto.email()).thenReturn("admin@example.com");
        when(userDto.role()).thenReturn(UserRole.ADMIN);
        when(userService.getUserByEmail("admin@example.com")).thenReturn(Optional.of(userDto));

        try (var logCaptor = LogCaptor.forClass(AuthenticationService.class)) {
            final var result = authenticationService.login("admin@example.com");
            assertThat(result).isTrue();
            assertThat(logCaptor.getWarnLogs()).containsExactly(
                    "No Vaadin servlet request/response available; SecurityContext not saved to session.");
            assertThat(logCaptor.getInfoLogs()).containsExactly(
                    "User with email 'admin@example.com' successfully logged in.");
        }

        final var securityContext = SecurityContextHolder.getContext();
        final var authentication = securityContext.getAuthentication();
        final var authorities = authentication.getAuthorities();
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");

        final var loggedInUser = authenticationService.getLoggedInUser().orElseThrow();
        assertThat(loggedInUser.name()).isEqualTo("Test Admin");
        assertThat(loggedInUser.email()).isEqualTo("admin@example.com");
        assertThat(loggedInUser.role()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void loginShouldSucceedWithExistingAdminWithRequest() {
        when(userDto.name()).thenReturn("Test Admin");
        when(userDto.email()).thenReturn("admin@example.com");
        when(userDto.role()).thenReturn(UserRole.ADMIN);
        when(userService.getUserByEmail("admin@example.com")).thenReturn(Optional.of(userDto));

        try (var vaadinService = mockStatic(VaadinService.class);
             var logCaptor = LogCaptor.forClass(AuthenticationService.class)) {

            vaadinService.when(VaadinService::getCurrentRequest).thenReturn(vaadinServletRequest);
            vaadinService.when(VaadinService::getCurrentResponse).thenReturn(vaadinServletResponse);

            final var result = authenticationService.login("admin@example.com");
            assertThat(result).isTrue();
            assertThat(logCaptor.getWarnLogs()).isEmpty();
            assertThat(logCaptor.getInfoLogs()).containsExactly(
                    "User with email 'admin@example.com' successfully logged in.");
        }

        final var securityContext = SecurityContextHolder.getContext();
        final var authentication = securityContext.getAuthentication();
        final var authorities = authentication.getAuthorities();
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");

        final var loggedInUser = authenticationService.getLoggedInUser().orElseThrow();
        assertThat(loggedInUser.name()).isEqualTo("Test Admin");
        assertThat(loggedInUser.email()).isEqualTo("admin@example.com");
        assertThat(loggedInUser.role()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void logoutWithoutLoggedInUserShouldRedirectOnly() {
        final var page = mock(Page.class);
        final var ui = mock(UI.class);
        when(ui.getPage()).thenReturn(page);

        try (var logCaptor = LogCaptor.forClass(AuthenticationService.class)) {
            authenticationService.logout(ui, "/redirect-url");
            assertThat(logCaptor.getWarnLogs()).containsExactly(
                    "No authenticated user found; logout skipped.");
        }

        verify(page).setLocation("/redirect-url");
        verifyNoMoreInteractions(page);
    }

    @Test
    void logoutWithLoggedInUserShouldLogoutAndRedirect() {
        // reuse login test to log in a user
        loginShouldSucceedWithExistingUserWithRequestAndResponse();

        final var page = mock(Page.class);
        final var ui = mock(UI.class);
        when(ui.getPage()).thenReturn(page);

        try (var vaadinServletRequestStatic = mockStatic(VaadinServletRequest.class);
             var logCaptor = LogCaptor.forClass(AuthenticationService.class)) {

            final var vaadinServletRequest = mock(VaadinServletRequest.class);
            when(vaadinServletRequest.getHttpServletRequest()).thenReturn(mock(HttpServletRequest.class));
            vaadinServletRequestStatic.when(VaadinServletRequest::getCurrent).thenReturn(vaadinServletRequest);

            authenticationService.logout(ui, "/redirect-url");
            assertThat(logCaptor.getWarnLogs()).isEmpty();
            assertThat(logCaptor.getInfoLogs()).containsExactly(
                    "User with email 'user@example.com' successfully logged out.");
        }

        verify(page).setLocation("/redirect-url");
        verifyNoMoreInteractions(page);
    }

    @Test
    void logoutWithUiOnlyShouldRedirectToDefaultLocation() {
        // reuse login test to log in a user
        loginShouldSucceedWithExistingUserWithRequestAndResponse();

        final var page = mock(Page.class);
        final var ui = mock(UI.class);
        when(ui.getPage()).thenReturn(page);

        try (var vaadinServletRequestStatic = mockStatic(VaadinServletRequest.class);
             var logCaptor = LogCaptor.forClass(AuthenticationService.class)) {

            final var vaadinServletRequest = mock(VaadinServletRequest.class);
            when(vaadinServletRequest.getHttpServletRequest()).thenReturn(mock(HttpServletRequest.class));
            vaadinServletRequestStatic.when(VaadinServletRequest::getCurrent).thenReturn(vaadinServletRequest);

            authenticationService.logout(ui);
            assertThat(logCaptor.getWarnLogs()).isEmpty();
            assertThat(logCaptor.getInfoLogs()).containsExactly(
                    "User with email 'user@example.com' successfully logged out.");
        }

        verify(page).setLocation("/login");
        verifyNoMoreInteractions(page);
    }

    @Test
    void getLoggedInUserShouldHandleIncorrectPrincipalTypeGracefully() {
        // reuse login test to log in a user
        loginShouldSucceedWithExistingUserWithRequestAndResponse();

        // set incorrect principal type
        final var principal = new User("Name", "Password", emptyList());
        final var authentication = new PreAuthenticatedAuthenticationToken(principal, null, emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // verify that getLoggedInUser() handles this gracefully
        final var loggedInUser = authenticationService.getLoggedInUser();
        assertThat(loggedInUser).isEmpty();
    }

    @Test
    void getLoggedInUserShouldHandleIncorrectAuthenticationGracefully() {
        // reuse login test to log in a user
        loginShouldSucceedWithExistingUserWithRequestAndResponse();

        // set incorrect authentication
        final var authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // verify that getLoggedInUser() handles this gracefully
        final var loggedInUser = authenticationService.getLoggedInUser();
        assertThat(loggedInUser).isEmpty();
    }

    @Test
    void isUserLoggedIn_returnsFalse() {
        SecurityContextHolder.clearContext();
        final boolean result = authenticationService.isUserLoggedIn();
        assertThat(result).isFalse();
    }

    @Test
    void isUserLoggedIn_returnsTrue() {
        final var principal = mock(UserPrincipal.class);
        final var authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(principal);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            final boolean result = authenticationService.isUserLoggedIn();
            assertThat(result).isTrue();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
