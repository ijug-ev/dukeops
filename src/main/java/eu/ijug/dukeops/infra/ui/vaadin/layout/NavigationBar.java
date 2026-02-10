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
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.NonNull;

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

        ComponentEffect.effect(this, () ->
                updateAuthenticationVisibility(authenticationSignal, dashboardLink, loginLink, logoutLink));

        final var menuBar = new Nav();
        menuBar.addClassName("menu-bar");
        menuBar.add(dashboardLink, loginLink, logoutLink, imprintLink);
        return menuBar;
    }

    /**
     * <p>Updates the visibility of navigation elements based on the current authentication state.</p>
     *
     * <p>The method evaluates the authentication state via the given {@link AuthenticationSignal}
     * and adjusts the visibility of the navigation links accordingly.</p>
     *
     * <p>If the authentication state cannot be determined because the underlying Vaadin session
     * is no longer active (for example during logout teardown), the method safely falls back to
     * treating the user as unauthenticated.</p>
     *
     * <p>This method is extracted to allow deterministic unit testing of the visibility logic
     * without requiring an active Vaadin session or UI context.</p>
     *
     * @param authenticationSignal the signal providing the current authentication state
     * @param dashboardLink the navigation link to the dashboard view
     * @param loginLink the navigation link to the login view
     * @param logoutLink the navigation link to the logout view
     */
    @VisibleForTesting
    static void updateAuthenticationVisibility(final @NonNull AuthenticationSignal authenticationSignal,
                                               final @NotNull RouterLink dashboardLink,
                                               final @NotNull RouterLink loginLink,
                                               final @NotNull RouterLink logoutLink) {
        boolean isLoggedIn;
        try {
            isLoggedIn = authenticationSignal.isAuthenticated();
        } catch (final Exception e) {
            isLoggedIn = false;
        }
        dashboardLink.setVisible(isLoggedIn);
        loginLink.setVisible(!isLoggedIn);
        logoutLink.setVisible(isLoggedIn);
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
