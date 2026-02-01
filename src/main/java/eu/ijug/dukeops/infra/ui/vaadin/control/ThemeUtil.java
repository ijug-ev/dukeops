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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ColorScheme;
import org.jetbrains.annotations.NotNull;

public final class ThemeUtil {

    private static final String DARK_MODE = "dark-mode";

    @SuppressWarnings("java:S5411") // Boolean value is never null
    public static void initializeDarkMode(final @NotNull UI ui) {
        LocalStorageUtil.getBoolean(ui, DARK_MODE, false, isDarkModeEnabled -> {
            final var isDarkModeActive = isDarkModeActive(ui);
            if (isDarkModeEnabled && !isDarkModeActive) {
                toggleDarkMode(ui);
            }
        });
    }

    public static boolean isDarkModeActive(final @NotNull UI ui) {
        return ui.getPage().getColorScheme().equals(ColorScheme.Value.DARK);
    }

    public static void toggleDarkMode(final @NotNull UI ui) {
        if (isDarkModeActive(ui)) {
            ui.getPage().setColorScheme(ColorScheme.Value.LIGHT);
            LocalStorageUtil.setBoolean(ui, DARK_MODE, false);
        } else {
            ui.getPage().setColorScheme(ColorScheme.Value.DARK);
            LocalStorageUtil.setBoolean(ui, DARK_MODE, true);
        }
    }


    /**
     * <p>Private constructor to prevent instantiation of this utility class.</p>
     */
    private ThemeUtil() {
        throw new IllegalStateException("Utility class");
    }

}
