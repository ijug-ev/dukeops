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

import eu.ijug.dukeops.domain.clubdesk.entity.Country;
import eu.ijug.dukeops.domain.clubdesk.entity.ImportRecord;
import eu.ijug.dukeops.domain.user.control.UserService;
import eu.ijug.dukeops.test.IntegrationTest;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static eu.ijug.dukeops.infra.persistence.jooq.generated.Tables.CLUBDESK;
import static eu.ijug.dukeops.infra.persistence.jooq.generated.Tables.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

final class ClubDeskServiceIT extends IntegrationTest {

    @Autowired
    private ClubDeskService clubDeskService;

    @Autowired
    private UserService userService;

    @Autowired
    private DSLContext dsl;

    @MockitoBean
    private Clock clock;

    @Test
    void saveImportRecords_shouldCreateOrUpdateUser_andPersistClubDeskRecord() {
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(clock.instant()).thenReturn(Instant.parse("2026-02-02T10:00:00Z"));

        final var record = new ImportRecord(
                "John",
                "Doe",
                "Street 1",
                "",
                "6000",
                "Lucerne",
                Country.ofIso2("CH"),
                "user@example.com",
                "",
                "",
                "",
                "",
                false,
                "",
                "",
                "",
                "",
                "JUG CH"
        );

        final int saved = clubDeskService.saveImportRecords(List.of(record));

        assertThat(saved).isEqualTo(1);

        // user exists
        final var user = userService.getUserByEmail("user@example.com").orElseThrow();
        assertThat(user.id()).isNotNull();
        assertThat(user.name()).isEqualTo("John Doe");

        // clubdesk record exists
        final var clubDesk = dsl.fetchOne(CLUBDESK, CLUBDESK.ID.eq(user.id()));
        assertThat(clubDesk).isNotNull();
        assertThat(clubDesk.getEmail()).isEqualTo("user@example.com");
        assertThat(clubDesk.getJug()).isEqualTo("JUG CH");
        assertThat(clubDesk.getCreated()).isNotNull();
        assertThat(clubDesk.getUpdated()).isNotNull();

        // sanity: user row in DB exists too
        final var userRow = dsl.fetchOne(USER, USER.ID.eq(user.id()));
        assertThat(userRow).isNotNull();
    }

    @Test
    void saveImportRecords_shouldUpdateUserName_andUpdateClubDeskTimestampOnSecondImport() {
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        // First import at 10:00
        when(clock.instant()).thenReturn(Instant.parse("2026-02-02T10:00:00Z"));

        final var first = new ImportRecord(
                "John",
                "Doe",
                "Street 1",
                "",
                "6000",
                "Lucerne",
                Country.ofIso2("CH"),
                "user@example.com",
                "",
                "",
                "",
                "",
                false,
                "",
                "",
                "",
                "",
                "JUG CH"
        );

        final int savedFirst = clubDeskService.saveImportRecords(List.of(first));
        assertThat(savedFirst).isEqualTo(1);

        final var userAfterFirst = userService.getUserByEmail("user@example.com").orElseThrow();
        assertThat(userAfterFirst.id()).isNotNull();
        assertThat(userAfterFirst.name()).isEqualTo("John Doe");

        final var clubDeskAfterFirst = dsl.fetchOne(CLUBDESK, CLUBDESK.ID.eq(userAfterFirst.id()));
        assertThat(clubDeskAfterFirst).isNotNull();
        final var createdAt = clubDeskAfterFirst.getCreated();
        final var updatedAtFirst = clubDeskAfterFirst.getUpdated();

        // Second import at 11:30 with changed first name
        when(clock.instant()).thenReturn(Instant.parse("2026-02-02T11:30:00Z"));

        final var second = new ImportRecord(
                "Jane",
                "Doe",
                "Street 1",
                "",
                "6000",
                "Lucerne",
                Country.ofIso2("CH"),
                "user@example.com",
                "",
                "",
                "",
                "",
                false,
                "",
                "",
                "",
                "",
                "JUG CH"
        );

        final int savedSecond = clubDeskService.saveImportRecords(List.of(second));
        assertThat(savedSecond).isEqualTo(1);

        // Users name updated
        final var userAfterSecond = userService.getUserByEmail("user@example.com").orElseThrow();
        assertThat(userAfterSecond.id()).isEqualTo(userAfterFirst.id());
        assertThat(userAfterSecond.name()).isEqualTo("Jane Doe");

        // ClubDesk record updated timestamp changed, created stays the same
        final var clubDeskAfterSecond = dsl.fetchOne(CLUBDESK, CLUBDESK.ID.eq(userAfterFirst.id()));
        assertThat(clubDeskAfterSecond).isNotNull();
        assertThat(clubDeskAfterSecond.getCreated()).isEqualTo(createdAt);
        assertThat(clubDeskAfterSecond.getUpdated()).isNotEqualTo(updatedAtFirst);
    }

}
