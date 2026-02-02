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
package eu.ijug.dukeops.domain.user.control;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class FullNameBuilderTest {

    @Test
    void buildFullName_shouldJoinFirstnameAndLastname() {
        assertThat(FullNameBuilder.buildFullName("John", "Doe", "fallback"))
                .isEqualTo("John Doe");
    }

    @Test
    void buildFullName_shouldHandleMissingLastname() {
        assertThat(FullNameBuilder.buildFullName("John", "", "fallback"))
                .isEqualTo("John");
    }

    @Test
    void buildFullName_shouldHandleMissingFirstname() {
        assertThat(FullNameBuilder.buildFullName("", "Doe", "fallback"))
                .isEqualTo("Doe");
    }

    @Test
    void buildFullName_shouldTrimAndCollapseEmpty() {
        assertThat(FullNameBuilder.buildFullName("  John  ", "  Doe  ", "fallback"))
                .isEqualTo("John Doe");

        assertThat(FullNameBuilder.buildFullName(" ", "  ", "fallback"))
                .isEqualTo("fallback");
    }

    @Test
    void buildFullName_shouldFallbackWhenBothNull() {
        assertThat(FullNameBuilder.buildFullName(null, null, "fallback"))
                .isEqualTo("fallback");
    }

}
