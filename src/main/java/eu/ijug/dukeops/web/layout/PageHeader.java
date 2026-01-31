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
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.ijug.dukeops.service.AuthenticationService;
import eu.ijug.dukeops.web.infra.Navigator;
import eu.ijug.dukeops.web.view.DashboardView;
import eu.ijug.dukeops.web.view.LoginView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

public final class PageHeader extends Header {

    public PageHeader(final @NotNull String title, final @Nullable String slogan,
                      final @NotNull AuthenticationService authenticationService,
                      final @NotNull Navigator navigator) {
        super();
        addClassName("page-header");

        // Logo
        final var logo = new Image("/icons/icon.png", "DukeOps Logo");
        logo.addClassName("logo");
        logo.addClickListener(_ -> handleLogoClick(getUI().orElseThrow(), authenticationService, navigator));

        // Title
        final var titleLayout = new VerticalLayout();
        titleLayout.setPadding(false);
        titleLayout.setSpacing(false);
        titleLayout.add(new H1(title));
        if (slogan != null) {
            titleLayout.add(new H2(slogan));
        }

        // Container
        final var content = new HorizontalLayout(logo, titleLayout);
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.setPadding(false);
        content.setSpacing(true);

        add(content);
    }

    @VisibleForTesting
    void handleLogoClick(final @NotNull UI ui,
                         final @NotNull AuthenticationService authenticationService,
                         final @NotNull Navigator navigator) {
        if (authenticationService.isUserLoggedIn()) {
            navigator.navigate(ui, DashboardView.class);
        } else {
            navigator.navigate(ui, LoginView.class);
        }
    }

}
