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
package eu.ijug.dukeops.domain.authentication.entity;

import eu.ijug.dukeops.domain.user.entity.UserDto;
import eu.ijug.dukeops.domain.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    @Test
    void testUserWithAllValuesSet() {
        final var id = UUID.randomUUID();
        final var created = LocalDateTime.now().minusDays(2);
        final var updated = LocalDateTime.now().minusDays(1);
        final var name = "Test User";
        final var email = "test@example.com";
        final var role = UserRole.USER;

        final var user = new UserDto(id, created, updated, name, email, role);
        final var authorities = List.of((GrantedAuthority) new SimpleGrantedAuthority("ROLE_USER"));

        final var principal = new UserPrincipal(user, authorities);

        assertThat(principal.getUser().equals(user));
        assertThat(principal.getAuthorities()).isEqualTo(authorities);
        assertThat(principal.getPassword()).isNull();
        assertThat(principal.getUsername().equals(email));
        assertThat(principal.isAccountNonExpired()).isTrue();
        assertThat(principal.isAccountNonLocked()).isTrue();
        assertThat(principal.isCredentialsNonExpired()).isTrue();
        assertThat(principal.isEnabled()).isTrue();
    }
}
