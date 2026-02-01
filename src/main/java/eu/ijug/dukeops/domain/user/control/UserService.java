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

import eu.ijug.dukeops.domain.user.entity.UserDto;
import eu.ijug.dukeops.infra.persistence.jooq.StorageService;
import eu.ijug.dukeops.infra.persistence.jooq.UniqueIdGenerator;
import eu.ijug.dukeops.infra.persistence.jooq.generated.tables.records.UserRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static eu.ijug.dukeops.infra.persistence.jooq.generated.Tables.USER;

@Service
public final class UserService extends StorageService {

    private final @NotNull DSLContext dsl;

    public UserService(final @NotNull DSLContext dsl,
                       final @NotNull UniqueIdGenerator idGenerator) {
        super(idGenerator);
        this.dsl = dsl;
    }

    public @NotNull UserDto storeUser(final @NotNull UserDto user) {
        final UserRecord userRecord = dsl.fetchOptional(USER, USER.ID.eq(user.id()))
                .orElse(dsl.newRecord(USER));
        createOrUpdate(USER, user, userRecord);
        return userRecord.into(UserDto.class);
    }

    public @NotNull List<@NotNull UserDto> getAllUsers() {
        return dsl.selectFrom(USER)
                .fetchInto(UserDto.class);
    }

    public @NotNull Optional<UserDto> getUserByEmail(final @NotNull String email) {
        return dsl.selectFrom(USER)
                .where(USER.EMAIL.eq(email))
                .fetchOptionalInto(UserDto.class);
    }

    public boolean deleteUser(final @NotNull UserDto user) {
        return dsl.delete(USER)
                .where(USER.EMAIL.eq(user.email()))
                .execute() > 0;
    }

}
