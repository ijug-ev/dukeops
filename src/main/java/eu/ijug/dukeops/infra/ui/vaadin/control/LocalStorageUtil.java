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
import com.vaadin.flow.function.SerializableConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LocalStorageUtil {

    /**
     * <p>Stores a key-value pair in the browser's LocalStorage.</p>
     *
     * <p>This method writes the given value under the specified key
     * into the browser's LocalStorage. The data will persist even after
     * the page is reloaded or the browser is closed and reopened.</p>
     *
     * @param ui    the current UI instance; must not be {@code null}
     * @param key   the storage key; must not be {@code null}
     * @param value the storage value; must not be {@code null}
     */
    public static void setString(final @NotNull UI ui,
                                 final @NotNull String key,
                                 final @NotNull String value) {
        ui.getPage().executeJs("localStorage.setItem($0, $1);", key, value);
    }

    /**
     * <p>Retrieves a value from the browser's LocalStorage asynchronously.</p>
     *
     * <p>If no value is stored under the given key, the specified {@code defaultValue}
     * will be passed to the callback instead. The retrieval is asynchronous because
     * it requires communication with the client-side browser.</p>
     *
     * @param ui           the current UI instance; must not be {@code null}
     * @param key          the storage key; must not be {@code null}
     * @param defaultValue the value to pass to the callback if the specified key is not found in LocalStorage;
     *                     may be {@code null}
     * @param callback     a callback that receives the stored value or {@code defaultValue}
     *                     if not found; must not be {@code null}
     */
    public static void getString(final @NotNull UI ui,
                                 final @NotNull String key,
                                 final @Nullable String defaultValue,
                                 final @NotNull SerializableConsumer<@Nullable String> callback) {
        try {
            ui.getPage()
                    .executeJs("return localStorage.getItem($0);", key)
                    .then(String.class, value -> callback.accept(value != null ? value : defaultValue));
        } catch (Exception e) {
            callback.accept(defaultValue);
        }
    }

    /**
     * <p>Stores a boolean value in the browser's LocalStorage.</p>
     *
     * <p>This is a convenience method that converts the boolean value to
     * a String and delegates to
     * {@link #getString(UI, String, String, SerializableConsumer)}.</p>
     *
     * @param ui    the current UI instance; must not be {@code null}
     * @param key   the storage key; must not be {@code null}
     * @param value the boolean value to store
     */
    public static void setBoolean(final @NotNull UI ui,
                                  final @NotNull String key,
                                  final boolean value) {
        setString(ui, key, String.valueOf(value));
    }

    /**
     * <p>Retrieves a boolean value from the browser's LocalStorage asynchronously.</p>
     *
     * <p>This is a convenience method that delegates to
     * {@link #getString(UI, String, String, SerializableConsumer)} and converts
     * the stored String value to a boolean. If no value is found, the specified
     * {@code defaultValue} is returned.</p>
     *
     * @param ui           the current UI instance; must not be {@code null}
     * @param key          the storage key; must not be {@code null}
     * @param defaultValue the value to pass to the callback if the specified key is not found in LocalStorage
     * @param callback     a callback that receives the stored boolean value or {@code defaultValue}
     *                     if not found; must not be {@code null}
     */
    public static void getBoolean(final @NotNull UI ui,
                                  final @NotNull String key,
                                  final boolean defaultValue,
                                  final @NotNull SerializableConsumer<@NotNull Boolean> callback) {
        getString(ui, key, Boolean.toString(defaultValue), value -> callback.accept(Boolean.parseBoolean(value)));
    }

    /**
     * <p>Private constructor to prevent instantiation of this utility class.</p>
     */
    private LocalStorageUtil() {
        throw new IllegalStateException("Utility class");
    }

}
