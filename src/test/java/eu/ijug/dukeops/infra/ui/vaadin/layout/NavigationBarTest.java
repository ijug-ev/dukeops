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
package eu.ijug.dukeops.infra.ui.vaadin.layout;

import com.vaadin.flow.router.RouterLink;
import eu.ijug.dukeops.domain.authentication.entity.AuthenticationSignal;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NavigationBarTest {

    @Test
    void updateAuthVisibility_setsLoggedOutState_whenSignalThrows() {
        final var signal = mock(AuthenticationSignal.class);
        when(signal.isAuthenticated()).thenThrow(new IllegalStateException("session closed"));

        final var dashboard = mock(RouterLink.class);
        final var login = mock(RouterLink.class);
        final var logout = mock(RouterLink.class);

        NavigationBar.updateAuthenticationVisibility(signal, dashboard, login, logout);

        verify(dashboard).setVisible(false);
        verify(login).setVisible(true);
        verify(logout).setVisible(false);
    }

    @Test
    void updateAuthVisibility_setsLoggedOutState_whenSignalReturnsFalse() {
        final var signal = mock(AuthenticationSignal.class);
        when(signal.isAuthenticated()).thenReturn(false);

        final var dashboard = mock(RouterLink.class);
        final var login = mock(RouterLink.class);
        final var logout = mock(RouterLink.class);

        NavigationBar.updateAuthenticationVisibility(signal, dashboard, login, logout);

        verify(dashboard).setVisible(false);
        verify(login).setVisible(true);
        verify(logout).setVisible(false);
    }

    @Test
    void updateAuthVisibility_setsLoggedInState_whenSignalReturnsTrue() {
        final var signal = mock(AuthenticationSignal.class);
        when(signal.isAuthenticated()).thenReturn(true);

        final var dashboard = mock(RouterLink.class);
        final var login = mock(RouterLink.class);
        final var logout = mock(RouterLink.class);

        NavigationBar.updateAuthenticationVisibility(signal, dashboard, login, logout);

        verify(dashboard).setVisible(true);
        verify(login).setVisible(false);
        verify(logout).setVisible(true);
    }

}
