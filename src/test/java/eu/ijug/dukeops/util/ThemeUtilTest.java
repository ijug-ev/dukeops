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
package eu.ijug.dukeops.util;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.theme.lumo.Lumo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ThemeUtilTest {

    private UI uiMock;
    private ThemeList themeListMock;

    @BeforeEach
    void setup() {
        uiMock = Mockito.mock(UI.class);
        themeListMock = Mockito.mock(ThemeList.class);

        final var elementMock = Mockito.mock(Element.class);
        when(uiMock.getElement()).thenReturn(elementMock);
        when(elementMock.getThemeList()).thenReturn(themeListMock);

        UI.setCurrent(uiMock);
    }

    @Test
    void testIsDarkModeActive() {
        when(themeListMock.contains(Lumo.DARK)).thenReturn(true);
        assertThat(ThemeUtil.isDarkModeActive(uiMock)).isTrue();

        when(themeListMock.contains(Lumo.DARK)).thenReturn(false);
        assertThat(ThemeUtil.isDarkModeActive(uiMock)).isFalse();
    }

    @Test
    void testToggleDarkModeActivatesDarkMode() {
        try (MockedStatic<LocalStorageUtil> mockedLocalStorage = mockStatic(LocalStorageUtil.class)) {
            when(themeListMock.contains(Lumo.DARK)).thenReturn(false);

            ThemeUtil.toggleDarkMode(uiMock);

            verify(themeListMock).add(Lumo.DARK);
            mockedLocalStorage.verify(() -> LocalStorageUtil.setBoolean(uiMock, "dark-mode", true));
        }
    }

    @Test
    void testToggleDarkModeDeactivatesDarkMode() {
        try (MockedStatic<LocalStorageUtil> mockedLocalStorage = mockStatic(LocalStorageUtil.class)) {
            when(themeListMock.contains(Lumo.DARK)).thenReturn(true);

            ThemeUtil.toggleDarkMode(uiMock);

            verify(themeListMock).remove(Lumo.DARK);
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

            when(themeListMock.contains(Lumo.DARK)).thenReturn(false);

            ThemeUtil.initializeDarkMode(uiMock);

            verify(themeListMock).add(Lumo.DARK);
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

            when(themeListMock.contains(Lumo.DARK)).thenReturn(false);

            ThemeUtil.initializeDarkMode(uiMock);

            verify(themeListMock, never()).add(Lumo.DARK);
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

            when(themeListMock.contains(Lumo.DARK)).thenReturn(true);

            ThemeUtil.initializeDarkMode(uiMock);

            verify(themeListMock, never()).add(anyString());
        }
    }

}
