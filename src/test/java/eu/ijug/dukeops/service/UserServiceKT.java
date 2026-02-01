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
package eu.ijug.dukeops.service;

import eu.ijug.dukeops.entity.UserDto;
import eu.ijug.dukeops.entity.UserRole;
import eu.ijug.dukeops.test.KaribuTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceKT extends KaribuTest {

    @Autowired
    private @NotNull UserService userService;

    @Test
    @SuppressWarnings("java:S5961")
    void crud() {
        final var originalUserCount = userService.getAllUsers().size();

        var testUser = new UserDto(null, null, null,
                "Test User", "test@example.com", UserRole.USER);
        userService.storeUser(testUser);

        testUser = userService.getUserByEmail("test@example.com").orElseThrow();
        assertThat(testUser).isNotNull().satisfies(testee -> {
            assertThat(testee.name()).isEqualTo("Test User");
            assertThat(testee.email()).isEqualTo("test@example.com");
            assertThat(testee.role()).isEqualTo(UserRole.USER);
        });

        assertThat(userService.getAllUsers().size()).isEqualTo(originalUserCount + 1);

        testUser = new UserDto(testUser.id(), testUser.created(), testUser.updated(),
                "Updated Test User", testUser.email(), testUser.role());
        userService.storeUser(testUser);

        testUser = userService.getUserByEmail("test@example.com").orElseThrow();
        assertThat(testUser).isNotNull().satisfies(testee -> {
            assertThat(testee.name()).isEqualTo("Updated Test User");
            assertThat(testee.email()).isEqualTo("test@example.com");
            assertThat(testee.role()).isEqualTo(UserRole.USER);
        });

        assertThat(userService.getAllUsers().size()).isEqualTo(originalUserCount + 1);

        assertThat(userService.deleteUser(testUser)).isTrue();
        assertThat(userService.getAllUsers().size()).isEqualTo(originalUserCount);
        assertThat(userService.deleteUser(testUser)).isFalse();
        assertThat(userService.getAllUsers().size()).isEqualTo(originalUserCount);
    }

}
