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
package eu.ijug.dukeops.domain.clubdesk.control;

import eu.ijug.dukeops.domain.clubdesk.entity.ClubDeskDto;
import eu.ijug.dukeops.domain.user.control.UserService;
import eu.ijug.dukeops.domain.user.entity.UserDto;
import eu.ijug.dukeops.domain.user.entity.UserRole;
import eu.ijug.dukeops.test.IntegrationTest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static eu.ijug.dukeops.infra.persistence.jooq.generated.Tables.CLUBDESK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

final class JooqClubDeskRepositoryIT extends IntegrationTest {

    @Autowired
    private ClubDeskRepository clubDeskRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private DSLContext dsl;

    @MockitoBean
    private Clock clock;

    @Test
    void upsert_shouldSetCreatedAndUpdatedOnInsert_andUpdateUpdatedOnSecondUpsert() {
        final var userId = UUID.randomUUID();
        userService.storeUser(new UserDto(userId, null, null,
                "John Doe", "john.doe@example.com", UserRole.USER));

        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        // Insert at 10:00
        when(clock.instant()).thenReturn(Instant.parse("2026-02-02T10:00:00Z"));
        final var insertDto = minimalDto(userId, null, null, "Jon", "Doe", "john.doe@example.com");

        final var inserted = clubDeskRepository.upsert(insertDto);

        assertThat(inserted.id()).isEqualTo(userId);
        assertThat(inserted.created()).isNotNull();
        assertThat(inserted.updated()).isNotNull();
        assertThat(inserted.created()).isEqualTo(LocalDateTime.of(2026, 2, 2, 10, 0, 0));
        assertThat(inserted.updated()).isEqualTo(LocalDateTime.of(2026, 2, 2, 10, 0, 0));
        assertThat(inserted.firstname()).isEqualTo("Jon");

        // Update at 11:30
        when(clock.instant()).thenReturn(Instant.parse("2026-02-02T11:30:00Z"));
        final var updateDto = minimalDto(userId, inserted.created(), inserted.updated(),
                "John", "Doe", "john.doe@example.com");

        final var updated = clubDeskRepository.upsert(updateDto);

        assertThat(updated.id()).isEqualTo(userId);
        assertThat(updated.created()).isEqualTo(LocalDateTime.of(2026, 2, 2, 10, 0, 0));
        assertThat(updated.updated()).isEqualTo(LocalDateTime.of(2026, 2, 2, 11, 30, 0));
        assertThat(updated.firstname()).isEqualTo("John");

        // verify DB state directly
        final var record = dsl.fetchOne(CLUBDESK, CLUBDESK.ID.eq(userId));
        assertThat(record).isNotNull();
        assertThat(record.getCreated()).isEqualTo(LocalDateTime.of(2026, 2, 2, 10, 0, 0));
        assertThat(record.getUpdated()).isEqualTo(LocalDateTime.of(2026, 2, 2, 11, 30, 0));
        assertThat(record.getFirstname()).isEqualTo("John");
    }

    private static @NotNull ClubDeskDto minimalDto(final @NotNull UUID userId,
                                                   final @Nullable LocalDateTime created,
                                                   final @Nullable LocalDateTime updated,
                                                   final @NotNull String firstname,
                                                   @SuppressWarnings("SameParameterValue") final @NotNull String lastname,
                                                   @SuppressWarnings("SameParameterValue") final @NotNull String email) {
        return new ClubDeskDto(
                userId,

                created,
                updated,

                firstname,
                lastname,
                "",   // address
                "",   // addressAddition
                "",   // zip
                "",   // city
                null, // country

                email,
                "", // emailAlternative
                "", // matrix
                "", // mastodon
                "", // linkedin

                false, // sepaEnabled
                "",    // accountHolder
                "",    // iban
                "",    // bic

                "",  // jug
                true // newsletter default
        );
    }

}
