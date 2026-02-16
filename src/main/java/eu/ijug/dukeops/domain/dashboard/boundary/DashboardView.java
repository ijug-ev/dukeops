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
package eu.ijug.dukeops.domain.dashboard.boundary;

import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.router.Route;
import eu.ijug.dukeops.domain.authentication.control.AuthenticationService;
import eu.ijug.dukeops.domain.clubdesk.boundary.ClubDeskEditView;
import eu.ijug.dukeops.domain.clubdesk.boundary.ClubDeskImportView;
import eu.ijug.dukeops.infra.ui.vaadin.control.Navigator;
import eu.ijug.dukeops.infra.ui.vaadin.layout.AbstractView;
import eu.ijug.dukeops.infra.ui.vaadin.layout.WebsiteLayout;
import jakarta.annotation.security.PermitAll;
import org.jetbrains.annotations.NotNull;

import static eu.ijug.dukeops.domain.user.entity.UserRole.ADMIN;

@PermitAll
@Route(value = "", layout = WebsiteLayout.class)
public final class DashboardView extends AbstractView {

    @NotNull
    private final Navigator navigator;

    public DashboardView(final @NotNull AuthenticationService authenticationService,
                         final @NotNull Navigator navigator) {
        super();
        this.navigator = navigator;
        addClassName("dashboard-view");
        add(new H3("Dashboard"));

        final var cardContainer = new Div();
        cardContainer.addClassName("card-container");
        add(cardContainer);

        final var loggedInUser = authenticationService.getLoggedInUser().orElseThrow();
        if (loggedInUser.role() == ADMIN) {
            cardContainer.add(createCard(
                    getTranslation("domain.clubdesk.boundary.ClubDeskImportView.title"),
                    getTranslation("domain.clubdesk.boundary.ClubDeskImportView.description"),
                    "images/clubdesk-import.webp",
                    ClubDeskImportView.class));
        }

        cardContainer.add(createCard(
                getTranslation("domain.clubdesk.boundary.ClubDeskEditView.title"),
                getTranslation("domain.clubdesk.boundary.ClubDeskEditView.description"),
                "images/clubdesk-edit.webp",
                ClubDeskEditView.class));
    }

    private @NotNull Card createCard(final @NotNull String title,
                                     final @NotNull String description,
                                     final @NotNull String imageUrl,
                                     final @NotNull Class<? extends AbstractView> viewClass) {
        final var card = new Card();
        card.setTitle(title);
        card.add(description);
        card.setMedia(new Image(imageUrl, title));

        card.addClassName("clickable");
        card.getElement()
                .addEventListener("click", (_ -> navigator.navigate(getUI().orElseThrow(), viewClass)))
                .setFilter("event.button === 0");

        return card;
    }

    @Override
    protected @NotNull String getViewTitle() {
        return getTranslation("web.view.DashboardView.title");
    }

}
