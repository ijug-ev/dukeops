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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEffect;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;
import eu.ijug.dukeops.domain.authentication.boundary.LoginView;
import eu.ijug.dukeops.domain.authentication.boundary.LogoutView;
import eu.ijug.dukeops.domain.authentication.entity.AuthenticationSignal;
import eu.ijug.dukeops.domain.dashboard.boundary.DashboardView;
import eu.ijug.dukeops.infra.ui.vaadin.control.ThemeUtil;
import org.jetbrains.annotations.NotNull;

public final class NavigationBar extends HorizontalLayout {

    public NavigationBar(final @NotNull AuthenticationSignal authenticationSignal) {
        super();
        addClassName("navigation-bar");

        final var menuContainer = new Div();
        menuContainer.addClassName("menu-container");
        menuContainer.add(createNavigationBar(authenticationSignal));
        addToStart(menuContainer);

        addToEnd(createThemeToggle());
    }

    private Component createNavigationBar(final @NotNull AuthenticationSignal authenticationSignal) {
        final var dashboardLink = new RouterLink(getTranslation("web.view.DashboardView.title"), DashboardView.class);
        final var loginLink = new RouterLink(getTranslation("web.view.LoginView.title"), LoginView.class);
        final var logoutLink = new RouterLink(getTranslation("web.view.LogoutView.title"), LogoutView.class);

        final var imprintLink = new Anchor("https://www.ijug.eu/impressum",
                getTranslation("web.layout.NavigationBar.imprintLink.text"),
                AnchorTarget.BLANK);

        // update menu items based on authentication state
        ComponentEffect.effect(this, () -> {
            final var isLoggedIn = authenticationSignal.isAuthenticated();
            dashboardLink.setVisible(isLoggedIn);
            loginLink.setVisible(!isLoggedIn);
            logoutLink.setVisible(isLoggedIn);
        });

        final var menuBar = new Nav();
        menuBar.addClassName("menu-bar");
        menuBar.add(dashboardLink, loginLink, logoutLink, imprintLink);
        return menuBar;
    }

    private Component createThemeToggle() {
        final var themeToggleButton = new Button();
        themeToggleButton.addClassName("theme-toggle-button");
        themeToggleButton.setIcon(new Icon(VaadinIcon.ADJUST));
        themeToggleButton.addClickListener(clickEvent ->
                ThemeUtil.toggleDarkMode(clickEvent.getSource().getUI().orElseThrow()));
        themeToggleButton.setTooltipText(getTranslation("web.layout.NavigationBar.themeToggle.tooltip"));
        return themeToggleButton;
    }
}
