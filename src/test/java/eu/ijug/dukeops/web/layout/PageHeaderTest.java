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
package eu.ijug.dukeops.web.layout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import eu.ijug.dukeops.service.AuthenticationService;
import eu.ijug.dukeops.web.infra.Navigator;
import eu.ijug.dukeops.web.view.DashboardView;
import eu.ijug.dukeops.web.view.LoginView;
import org.junit.jupiter.api.Test;

import static eu.ijug.dukeops.test.TestUtil.findComponent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PageHeaderTest {

    @Test
    void onlyTitle() {
        final var authenticationService = mock(AuthenticationService.class);
        final var navigator = mock(Navigator.class);
        final var header = new PageHeader("DukeOps", null, authenticationService, navigator);
        assertThat(header.getClassNames()).contains("page-header");

        final var h1 = findComponent(header, H1.class);
        assertThat(h1).isNotNull();
        assertThat(h1.getText()).isEqualTo("DukeOps");

        assertThat(findComponent(header, H2.class)).isNull();
    }

    @Test
    void titleWithSubtitle() {
        final var authenticationService = mock(AuthenticationService.class);
        final var navigator = mock(Navigator.class);
        final var header = new PageHeader("DukeOps", "iJUG Self-Service Portal", authenticationService, navigator);
        assertThat(header.getClassNames()).contains("page-header");

        final var h1 = findComponent(header, H1.class);
        assertThat(h1).isNotNull();
        assertThat(h1.getText()).isEqualTo("DukeOps");

        final var h2 = findComponent(header, H2.class);
        assertThat(h2).isNotNull();
        assertThat(h2.getText()).isEqualTo("iJUG Self-Service Portal");
    }

    @Test
    void logoClick_navigatesToLogin_whenNotLoggedIn() {
        final var authenticationService = mock(AuthenticationService.class);
        when(authenticationService.isUserLoggedIn()).thenReturn(false);
        final var navigator = mock(Navigator.class);
        final var ui = mock(UI.class);

        final var header = new PageHeader("x", null, authenticationService, navigator);
        header.handleLogoClick(ui, authenticationService, navigator);

        verify(navigator).navigate(ui, LoginView.class);
        verify(navigator, never()).navigate(eq(ui), eq(DashboardView.class));
    }

    @Test
    void logoClick_navigatesToDashboard_whenLoggedIn() {
        final var authenticationService = mock(AuthenticationService.class);
        when(authenticationService.isUserLoggedIn()).thenReturn(true);
        final var navigator = mock(Navigator.class);
        final var ui = mock(UI.class);

        final var header = new PageHeader("x", null, authenticationService, navigator);
        header.handleLogoClick(ui, authenticationService, navigator);

        verify(navigator).navigate(ui, DashboardView.class);
        verify(navigator, never()).navigate(eq(ui), eq(LoginView.class));
    }

}
