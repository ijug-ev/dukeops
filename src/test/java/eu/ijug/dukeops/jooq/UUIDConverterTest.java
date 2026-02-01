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
package eu.ijug.dukeops.jooq;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UUIDConverterTest {

    private final UUIDConverter converter = new UUIDConverter();

    @Test
    void fromReturnsNullForNullInput() {
        assertThat(converter.from(null)).isNull();
    }

    @Test
    void fromParsesValidUuidString() {
        final UUID uuid = UUID.randomUUID();
        assertThat(converter.from(uuid.toString())).isEqualTo(uuid);
    }

    @Test
    void fromThrowsExceptionForInvalidUuidString() {
        assertThatThrownBy(() -> converter.from("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @SuppressWarnings("ConstantValue")
    void toReturnsNullForNullInput() {
        assertThat(converter.to(null)).isNull();
    }

    @Test
    void toReturnsStringRepresentationForUuid() {
        final UUID uuid = UUID.randomUUID();
        assertThat(converter.to(uuid)).isEqualTo(uuid.toString());
    }

    @Test
    void fromTypeReturnsStringClass() {
        assertThat(converter.fromType()).isSameAs(String.class);
    }

    @Test
    void toTypeReturnsUuidClass() {
        assertThat(converter.toType()).isSameAs(UUID.class);
    }
}
