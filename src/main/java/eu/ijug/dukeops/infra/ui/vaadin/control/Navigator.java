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
package eu.ijug.dukeops.infra.ui.vaadin.control;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import org.jetbrains.annotations.NotNull;

/**
 * Navigator is a simple utility component to handle navigation within the Vaadin UI.
 */
@org.springframework.stereotype.Component
public final class Navigator {

    /**
     * Navigates to the specified target component within the given UI.
     *
     * @param ui     the UI instance to perform the navigation on
     * @param target the target component class to navigate to
     */
    public void navigate(final @NotNull UI ui,
                         final @NotNull Class<? extends Component> target) {
        ui.navigate(target);
    }

}
