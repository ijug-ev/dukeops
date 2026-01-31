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

import org.jetbrains.annotations.NotNull;

public final class LinkUtil {

    private static @NotNull String baseUrl = "";

    public static void setBaseUrl(final @NotNull String baseUrl) {
        LinkUtil.baseUrl = baseUrl;
    }

    public static @NotNull String getBaseUrl() {
        if (baseUrl.isBlank()) {
            throw new IllegalStateException("Base URL has not been set");
        }
        return baseUrl;
    }

    private LinkUtil() {
        throw new IllegalStateException("Utility class");
    }

}
