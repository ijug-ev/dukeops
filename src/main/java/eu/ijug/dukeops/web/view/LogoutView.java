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

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.jetbrains.annotations.NotNull;
import eu.ijug.dukeops.domain.authentication.control.AuthenticationService;
import eu.ijug.dukeops.web.layout.WebsiteLayout;

@PermitAll
@Route(value = "logout", layout = WebsiteLayout.class)
public final class LogoutView extends AbstractView implements BeforeEnterObserver {

    private final @NotNull AuthenticationService authenticationService;

    public LogoutView(final @NotNull AuthenticationService authenticationService) {
        super();
        this.authenticationService = authenticationService;
    }

    @Override
    protected @NotNull String getViewTitle() {
        return getTranslation("web.view.LogoutView.title");
    }

    @Override
    public void beforeEnter(@NotNull final BeforeEnterEvent beforeEnterEvent) {
        authenticationService.logout(beforeEnterEvent.getUI());
    }
}
