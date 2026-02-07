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

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class CountryTest {

    @Test
    void testCountries() {
        assertThat(Country.ofIso2("DE").displayName(Locale.ENGLISH)).isEqualTo("Germany");
        assertThat(Country.ofIso2("AT").displayName(Locale.ENGLISH)).isEqualTo("Austria");
        assertThat(Country.ofIso2("CH").displayName(Locale.ENGLISH)).isEqualTo("Switzerland");

        assertThat(Country.ofIso2("DE").displayName(Locale.GERMAN)).isEqualTo("Deutschland");
        assertThat(Country.ofIso2("AT").displayName(Locale.GERMAN)).isEqualTo("Österreich");
        assertThat(Country.ofIso2("CH").displayName(Locale.GERMAN)).isEqualTo("Schweiz");
    }

    @Test
    @SuppressWarnings({"EqualsWithItself", "EqualsBetweenInconvertibleTypes", "ConstantValue"})
    void testEquality() {
        assertThat(Country.ofIso2("DE").equals(null)).isFalse();
        assertThat(Country.ofIso2("DE").equals("DE")).isFalse();
        assertThat(Country.ofIso2("DE").equals(Country.ofIso2("DE"))).isTrue();
        assertThat(Country.ofIso2("DE").equals(Country.ofIso2("AT"))).isFalse();
    }

}
