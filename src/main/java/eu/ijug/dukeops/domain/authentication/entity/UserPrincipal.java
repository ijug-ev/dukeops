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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public final class UserPrincipal implements UserDetails {

    private final @NotNull UserDto user;
    private final @NotNull Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(final @NotNull UserDto user,
                         final @NotNull List<@NotNull GrantedAuthority> authorities) {
        this.user = user;
        this.authorities = authorities;
    }

    public @NotNull UserDto getUser() {
        return user;
    }

    @Override
    public @NotNull Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public @Nullable String getPassword() {
        return null;
    }

    @Override
    public @NotNull String getUsername() {
        return user.email();
    }

}
