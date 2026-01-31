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

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLayout;
import eu.ijug.dukeops.service.AuthenticationService;
import eu.ijug.dukeops.web.infra.Navigator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import eu.ijug.dukeops.config.AppConfig;
import eu.ijug.dukeops.entity.AuthenticationSignal;
import eu.ijug.dukeops.util.ThemeUtil;

public final class WebsiteLayout extends Div implements RouterLayout, BeforeEnterObserver {

    private final @NotNull Main main;

    public WebsiteLayout(final @NotNull AppConfig appConfig,
                         final @NotNull AuthenticationService authenticationService,
                         final @NotNull Navigator navigator,
                         final @NotNull AuthenticationSignal authenticationSignal) {
        super();

        add(new PageHeader("DukeOps", "iJUG Self-Service Portal", authenticationService, navigator));
        add(new NavigationBar(authenticationSignal));

        main = new Main();
        add(main);

        final var dukeOpsVersion = appConfig.version();
        add(new PageFooter(dukeOpsVersion));
    }

    @Override
    public void showRouterLayoutContent(final @NotNull HasElement content) {
        main.removeAll();
        main.add(content.getElement().getComponent()
                .orElseThrow(() -> new IllegalArgumentException(
                        "WebsiteLayout content must be a Component")));
    }

    @Override
    public void removeRouterLayoutContent(final @Nullable HasElement oldContent) {
        main.removeAll();
    }

    @Override
    public void beforeEnter(final @NotNull BeforeEnterEvent event) {
        ThemeUtil.initializeDarkMode(event.getUI());
    }

}
