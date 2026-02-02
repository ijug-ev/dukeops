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

/**
 * <p>Spring-managed service responsible for managing {@link UserDto} persistence and retrieval.</p>
 *
 * <p>The service provides CRUD-style operations for users and delegates identifier generation and
 * common persistence behaviour to the underlying {@link StorageService}.</p>
 */
@Service
public class UserService extends StorageService {

    private final @NotNull DSLContext dsl;

    /**
     * <p>Creates a new user service using the provided jOOQ context and unique ID generator.</p>
     *
     * @param dsl the jOOQ DSL context used to execute database operations
     * @param idGenerator the generator used to create unique identifiers for new users
     */
    public UserService(final @NotNull DSLContext dsl,
                       final @NotNull UniqueIdGenerator idGenerator) {
        super(idGenerator);
        this.dsl = dsl;
    }


    /**
     * <p>Creates a new user or updates an existing user record based on the identifier contained
     * in the given {@link UserDto}.</p>
     *
     * <p>If the user does not yet exist, a new database record is created. Otherwise, the existing
     * record is updated with the provided data.</p>
     *
     * @param user the user data to store
     * @return the persisted user data as a DTO
     */
    public @NotNull UserDto storeUser(final @NotNull UserDto user) {
        final UserRecord userRecord = dsl.fetchOptional(USER, USER.ID.eq(user.id()))
                .orElse(dsl.newRecord(USER));
        createOrUpdate(USER, user, userRecord);
        return userRecord.into(UserDto.class);
    }

    /**
     * <p>Returns all users currently stored in the database.</p>
     *
     * @return a list of all users as DTOs
     */
    public @NotNull List<@NotNull UserDto> getAllUsers() {
        return dsl.selectFrom(USER)
                .fetchInto(UserDto.class);
    }

    /**
     * <p>Retrieves a user by the given email address.</p>
     *
     * <p>If no user with the specified email exists, an empty {@link Optional} is returned.</p>
     *
     * @param email the email address of the user to look up
     * @return an optional containing the user if found, or empty otherwise
     */
    public @NotNull Optional<UserDto> getUserByEmail(final @NotNull String email) {
        return dsl.selectFrom(USER)
                .where(USER.EMAIL.eq(email))
                .fetchOptionalInto(UserDto.class);
    }

    /**
     * <p>Deletes the given user based on the email address contained in the provided DTO.</p>
     *
     * @param user the user to delete
     * @return {@code true} if a user record was deleted, {@code false} otherwise
     */
    public boolean deleteUser(final @NotNull UserDto user) {
        return dsl.delete(USER)
                .where(USER.EMAIL.eq(user.email()))
                .execute() > 0;
    }

}
