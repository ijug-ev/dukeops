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
package eu.ijug.dukeops.infra.persistence.jooq;

import eu.ijug.dukeops.domain.clubdesk.entity.Country;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.Converter;

public final class CountryConverter implements Converter<String, Country> {

    @Override
    public @Nullable Country from(final @Nullable String databaseObject) {
        if (databaseObject == null || databaseObject.isBlank()) {
            return null;
        }
        return Country.ofIso2(databaseObject);
    }

    @Override
    public @NotNull String to(final @Nullable Country userObject) {
        if (userObject == null) {
            return "";
        }
        return userObject.iso2();
    }

    @Override
    public @NotNull Class<String> fromType() {
        return String.class;
    }

    @Override
    public @NotNull Class<Country> toType() {
        return Country.class;
    }
}
