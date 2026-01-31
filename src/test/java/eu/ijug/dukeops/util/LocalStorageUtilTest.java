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
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.function.SerializableConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * <p>Unit tests for {@link LocalStorageUtil} covering set/get of strings and booleans,
 * default value handling, and error fallback behavior.</p>
 *
 * <p>The tests mock Vaadin's {@link UI}, {@link Page}, and {@link PendingJavaScriptResult}
 * to avoid client-side dependencies.</p>
 */
@ExtendWith(MockitoExtension.class)
class LocalStorageUtilTest {

    @Mock
    private Page page;

    @Mock
    private PendingJavaScriptResult pending;

    /**
     * <p>Test-specific UI that returns the mocked {@link Page}.</p>
     */
    private static final class TestUI extends UI {
        private final Page page;
        private TestUI(Page page) { this.page = page; }
        @Override public Page getPage() { return page; }
    }

    @Test
    void setString_writesToLocalStorage() {
        // given
        final var ui = new TestUI(page);

        // when
        LocalStorageUtil.setString(ui, "k", "v");

        // then
        verify(page).executeJs("localStorage.setItem($0, $1);", "k", "v");
        verifyNoMoreInteractions(page);
    }

    @Test
    void getString_returnsStoredValue_whenPresent() {
        // given
        final var ui = new TestUI(page);
        when(page.executeJs(eq("return localStorage.getItem($0);"), any()))
                .thenReturn(pending);
        doAnswer(inv -> {
            SerializableConsumer<String> consumer = inv.getArgument(1);
            consumer.accept("stored");
            return null;
        }).when(pending).then(eq(String.class), any());

        final AtomicReference<String> result = new AtomicReference<>();

        // when
        LocalStorageUtil.getString(ui, "k", "default", result::set);

        // then
        assertThat(result.get()).isEqualTo("stored");
    }

    @Test
    void getString_returnsDefault_whenMissing() {
        // given
        final var ui = new TestUI(page);
        when(page.executeJs(eq("return localStorage.getItem($0);"), any()))
                .thenReturn(pending);
        doAnswer(inv -> {
            SerializableConsumer<String> consumer = inv.getArgument(1);
            consumer.accept(null); // simulate localStorage miss
            return null;
        }).when(pending).then(eq(String.class), any());

        final AtomicReference<String> result = new AtomicReference<>();

        // when
        LocalStorageUtil.getString(ui, "missing", "fallback", result::set);

        // then
        assertThat(result.get()).isEqualTo("fallback");
    }

    @Test
    void getString_returnsDefault_onException() {
        // given
        final var ui = new TestUI(page);
        when(page.executeJs(anyString(), any()))
                .thenThrow(new RuntimeException("boom"));

        final AtomicReference<String> result = new AtomicReference<>();

        // when
        LocalStorageUtil.getString(ui, "k", "safe", result::set);

        // then
        assertThat(result.get()).isEqualTo("safe");
    }

    @Test
    void setBoolean_delegatesToSetString() {
        // given
        final var ui = new TestUI(page);

        // when
        LocalStorageUtil.setBoolean(ui, "flag", true);

        // then
        verify(page).executeJs("localStorage.setItem($0, $1);", "flag", "true");
        verifyNoMoreInteractions(page);
    }

    @Test
    void getBoolean_returnsParsedValue_whenPresent() {
        // given
        final var ui = new TestUI(page);
        when(page.executeJs(eq("return localStorage.getItem($0);"), any()))
                .thenReturn(pending);
        doAnswer(inv -> {
            SerializableConsumer<String> consumer = inv.getArgument(1);
            consumer.accept("true");
            return null;
        }).when(pending).then(eq(String.class), any());

        final AtomicReference<Boolean> result = new AtomicReference<>();

        // when
        LocalStorageUtil.getBoolean(ui, "flag", false, result::set);

        // then
        assertThat(result.get()).isTrue();
    }

    @Test
    void getBoolean_returnsDefault_whenMissing() {
        // given
        final var ui = new TestUI(page);
        when(page.executeJs(eq("return localStorage.getItem($0);"), any()))
                .thenReturn(pending);
        doAnswer(inv -> {
            SerializableConsumer<String> consumer = inv.getArgument(1);
            consumer.accept(null);
            return null;
        }).when(pending).then(eq(String.class), any());

        final AtomicReference<Boolean> result = new AtomicReference<>();

        // when
        LocalStorageUtil.getBoolean(ui, "flag", false, result::set);

        // then
        assertThat(result.get()).isFalse();
    }

    @Test
    void getBoolean_parsesNonBooleanAsFalse() {
        // given
        final var ui = new TestUI(page);
        when(page.executeJs(eq("return localStorage.getItem($0);"), any()))
                .thenReturn(pending);
        doAnswer(inv -> {
            SerializableConsumer<String> consumer = inv.getArgument(1);
            consumer.accept("not-a-bool");
            return null;
        }).when(pending).then(eq(String.class), any());

        final AtomicReference<Boolean> result = new AtomicReference<>();

        // when
        LocalStorageUtil.getBoolean(ui, "flag", true, result::set);

        // then
        assertThat(result.get()).isFalse();
    }
}
