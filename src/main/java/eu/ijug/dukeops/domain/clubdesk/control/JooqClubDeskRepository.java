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
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.LocalDateTime;

import static eu.ijug.dukeops.infra.persistence.jooq.generated.Tables.CLUBDESK;

/**
 * <p>jOOQ-based {@link ClubDeskRepository} implementation for persisting {@link ClubDeskDto} entities.</p>
 *
 * <p>The repository performs an upsert operation based on the ClubDesk identifier and manages the
 * {@code created} and {@code updated} timestamps using an injected {@link Clock}.</p>
 */
@Repository
public class JooqClubDeskRepository implements ClubDeskRepository {

    private final @NotNull DSLContext dsl;
    private final @NotNull Clock clock;

    /**
     * <p>Creates a new repository using the provided jOOQ DSL context and clock.</p>
     *
     * @param dsl the jOOQ DSL context used to execute database operations
     * @param clock the clock used to determine creation and update timestamps
     */
    public JooqClubDeskRepository(final @NotNull DSLContext dsl,
                                  final @NotNull Clock clock) {
        super();
        this.dsl = dsl;
        this.clock = clock;
    }

    /**
     * <p>Creates or updates a ClubDesk record based on the identifier contained in the given DTO.</p>
     *
     * <p>If no existing record is found, a new record is created and both {@code created} and
     * {@code updated} timestamps are set. If a record already exists, only the {@code updated}
     * timestamp is modified.</p>
     *
     * @param clubDesk the ClubDesk data to persist
     * @return the persisted ClubDesk data as a DTO
     */
    @Override
    public @NotNull ClubDeskDto upsert(final @NotNull ClubDeskDto clubDesk) {
        final var clubDeskRecord = dsl.fetchOptional(CLUBDESK, CLUBDESK.ID.eq(clubDesk.id()))
                .orElseGet(() -> dsl.newRecord(CLUBDESK));

        final var existingCreated = clubDeskRecord.getCreated();
        clubDeskRecord.from(clubDesk);

        final var now = LocalDateTime.now(clock);
        if (existingCreated == null) {
            clubDeskRecord.setCreated(now);
            clubDeskRecord.setUpdated(now);
        } else {
            clubDeskRecord.setCreated(existingCreated);
            clubDeskRecord.setUpdated(now);
        }

        clubDeskRecord.store();
        return clubDeskRecord.into(ClubDeskDto.class);
    }

}
