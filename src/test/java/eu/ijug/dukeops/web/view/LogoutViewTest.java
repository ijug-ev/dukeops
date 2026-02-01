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
package eu.ijug.dukeops.web.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import eu.ijug.dukeops.domain.authentication.control.AuthenticationService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogoutViewTest {

    @Test
    void enteringLogoutViewShouldCallLogoutMethod() {
        final var authenticationService = mock(AuthenticationService.class);
        final var logoutView = new LogoutView(authenticationService);

        final var ui = mock(UI.class);
        final var beforeEnterEvent = mock(BeforeEnterEvent.class);
        when(beforeEnterEvent.getUI()).thenReturn(ui);

        logoutView.beforeEnter(beforeEnterEvent);
        verify(authenticationService).logout(ui);
    }

    @Test
    void getViewTitleShouldReturnCorrectTranslationKey() {
        final var authenticationService = mock(AuthenticationService.class);
        final var logoutView = new LogoutView(authenticationService);

        final var title = logoutView.getViewTitle();
        assert title.equals("!{web.view.LogoutView.title}!");
    }

}
