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
package eu.ijug.dukeops.domain.clubdesk.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;

public record ImportRecord(
        @Nullable String firstname,
        @Nullable String lastname,
        @Nullable String address,
        @Nullable String addressAddition,
        @Nullable String zipCode,
        @Nullable String city,
        @Nullable String country,

        @NotNull String email,
        @Nullable String emailAlternative,
        @Nullable String matrix,
        @Nullable String mastodon,
        @Nullable String linkedin,

        boolean sepaEnabled,
        @Nullable String sepaMandateReference,
        @Nullable LocalDate sepaMandateDate,
        @Nullable String sepaType,
        @Nullable LocalDate sepaLastDebitDate,
        @Nullable String sepaAccountHolder,
        @Nullable String sepaIban,
        @Nullable String sepaBic,

        @Nullable String jug
) { }
