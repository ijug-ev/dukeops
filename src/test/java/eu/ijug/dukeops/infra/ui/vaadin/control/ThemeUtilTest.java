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
import com.vaadin.flow.component.page.Page;
import eu.ijug.dukeops.infra.ui.vaadin.control.LocalStorageUtil;
import eu.ijug.dukeops.infra.ui.vaadin.control.ThemeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ThemeUtilTest {

    private UI uiMock;
    private Page page;

    @BeforeEach
    void setup() {
        uiMock = mock(UI.class);
        page = mock(Page.class);
        when(uiMock.getPage()).thenReturn(page);
        UI.setCurrent(uiMock);
    }

    @Test
    void testIsDarkModeActive() {
        when(page.getColorScheme()).thenReturn(ColorScheme.Value.DARK);
        assertThat(ThemeUtil.isDarkModeActive(uiMock)).isTrue();

        when(page.getColorScheme()).thenReturn(ColorScheme.Value.LIGHT);
        assertThat(ThemeUtil.isDarkModeActive(uiMock)).isFalse();
    }

    @Test
    void testToggleDarkModeActivatesDarkMode() {
        try (MockedStatic<LocalStorageUtil> mockedLocalStorage = mockStatic(LocalStorageUtil.class)) {
            when(page.getColorScheme()).thenReturn(ColorScheme.Value.LIGHT);

            ThemeUtil.toggleDarkMode(uiMock);

            verify(page).setColorScheme(ColorScheme.Value.DARK);
            mockedLocalStorage.verify(() -> LocalStorageUtil.setBoolean(uiMock, "dark-mode", true));
        }
    }

    @Test
    void testToggleDarkModeDeactivatesDarkMode() {
        try (MockedStatic<LocalStorageUtil> mockedLocalStorage = mockStatic(LocalStorageUtil.class)) {
            when(page.getColorScheme()).thenReturn(ColorScheme.Value.DARK);

            ThemeUtil.toggleDarkMode(uiMock);

            verify(page).setColorScheme(ColorScheme.Value.LIGHT);
            mockedLocalStorage.verify(() -> LocalStorageUtil.setBoolean(uiMock, "dark-mode", false));
        }
    }

    @Test
    void testInitializeDarkModeActivates() {
        try (MockedStatic<LocalStorageUtil> mockedLocalStorage = mockStatic(LocalStorageUtil.class)) {
            mockedLocalStorage.when(() -> LocalStorageUtil.getBoolean(any(), eq("dark-mode"), anyBoolean(), any())
            ).thenAnswer(invocation -> {
                final var callback = invocation.<Consumer<Boolean>>getArgument(3);
                callback.accept(true);
                return null;
            });

            when(page.getColorScheme()).thenReturn(ColorScheme.Value.LIGHT);

            ThemeUtil.initializeDarkMode(uiMock);

            verify(page).setColorScheme(ColorScheme.Value.DARK);
        }
    }

    @Test
    void testInitializeDarkModeDoesNothingIfNotSet() {
        try (MockedStatic<LocalStorageUtil> mockedLocalStorage = mockStatic(LocalStorageUtil.class)) {
            mockedLocalStorage.when(() -> LocalStorageUtil.getBoolean(any(), eq("dark-mode"), anyBoolean(), any())
            ).thenAnswer(invocation -> {
                final var callback = invocation.<Consumer<Boolean>>getArgument(3);
                callback.accept(false);
                return null;
            });

            when(page.getColorScheme()).thenReturn(ColorScheme.Value.LIGHT);

            ThemeUtil.initializeDarkMode(uiMock);

            verify(page, never()).setColorScheme(any());
        }
    }

    @Test
    void testInitializeDarkModeDoesNothingIfAlreadyActive() {
        try (MockedStatic<LocalStorageUtil> mockedLocalStorage = mockStatic(LocalStorageUtil.class)) {
            mockedLocalStorage.when(() ->
                    LocalStorageUtil.getBoolean(any(), eq("dark-mode"), anyBoolean(), any())
            ).thenAnswer(invocation -> {
                var callback = invocation.<Consumer<Boolean>>getArgument(3);
                callback.accept(true);
                return null;
            });

            when(page.getColorScheme()).thenReturn(ColorScheme.Value.DARK);

            ThemeUtil.initializeDarkMode(uiMock);

            verify(page, never()).setColorScheme(any());
        }
    }

}
