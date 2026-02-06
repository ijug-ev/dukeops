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
import eu.ijug.dukeops.domain.clubdesk.entity.ImportRecord;
import eu.ijug.dukeops.domain.user.control.FullNameBuilder;
import eu.ijug.dukeops.domain.user.control.UserService;
import eu.ijug.dukeops.domain.user.entity.UserDto;
import eu.ijug.dukeops.domain.user.entity.UserRole;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

/**
 * <p>Spring-managed service that orchestrates the import of ClubDesk CSV data into the application.</p>
 *
 * <p>The service is responsible for reading ClubDesk export files, converting them into
 * {@link ImportRecord} instances, resolving or creating corresponding users, and persisting
 * the resulting {@link ClubDeskDto} records via the repository layer.</p>
 */
@Service
public class ClubDeskService {

    private static final boolean DEFAULT_NEWSLETTER_SETTING = true; // opt-out
    private static final @NotNull Charset CLUBDESK_CHARSET = StandardCharsets.ISO_8859_1;
    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(ClubDeskService.class);

    private final @NotNull ClubDeskRepository clubDeskRepository;
    private final @NotNull ClubDeskImporter clubDeskImporter;
    private final @NotNull UserService userService;

    /**
     * <p>Creates a new ClubDesk service using the required collaborators.</p>
     *
     * @param clubDeskRepository the repository used to persist ClubDesk records
     * @param clubDeskImporter the importer used to parse ClubDesk CSV files
     * @param userService the user service used to resolve or create users
     */
    public ClubDeskService(final @NotNull ClubDeskRepository clubDeskRepository,
                           final @NotNull ClubDeskImporter clubDeskImporter,
                           final @NotNull UserService userService) {
        super();
        this.clubDeskRepository = clubDeskRepository;
        this.clubDeskImporter = clubDeskImporter;
        this.userService = userService;
    }


    /**
     * <p>Reads and parses a ClubDesk CSV export file into a list of {@link ImportRecord} instances.</p>
     *
     * <p>The file is read using the character set required by ClubDesk exports. The actual CSV parsing
     * logic is delegated to the configured {@link ClubDeskImporter}.</p>
     *
     * @param file the ClubDesk CSV file to import
     * @return an immutable list of parsed import records
     * @throws IllegalArgumentException if the given file is not a regular file
     * @throws IllegalStateException if the file cannot be read
     */
    public @NotNull List<ImportRecord> importClubDeskFile(final @NotNull File file) {
        if (!file.isFile()) {
            throw new IllegalArgumentException("Not a file: " + file.getAbsolutePath());
        }

        try (Reader reader = Files.newBufferedReader(file.toPath(), CLUBDESK_CHARSET)) {
            return clubDeskImporter.parse(reader);
        } catch (final IOException e) {
            throw new IllegalStateException("Unable to read CSV file: " + file.getAbsolutePath(), e);
        }
    }


    /**
     * <p>Persists the given list of {@link ImportRecord} instances and returns the number of stored records.</p>
     *
     * <p>For each import record, an existing user is resolved by email or a new user is created if none exists.
     * The users name is updated when necessary, and the corresponding ClubDesk data is stored with a default
     * newsletter setting.</p>
     *
     * @param importRecords the import records to persist
     * @return the number of successfully stored ClubDesk records
     */
    public int saveImportRecords(final @NotNull List<ImportRecord> importRecords) {
        if (importRecords.isEmpty()) {
            return 0;
        }

        int recordCounter = 0;

        for (final var importRecord : importRecords) {
            final var userId = getOrCreateUserIdAndUpdateNameIfNeeded(importRecord);
            final var clubDesk = clubDeskRepository.upsert(toClubDeskDto(userId, importRecord));
            recordCounter++;
            LOGGER.info("Successfully saved ClubDesk record #{} for user {} {} with email {}.",
                    recordCounter, clubDesk.firstname(), clubDesk.lastname(), clubDesk.email());
        }

        return recordCounter;
    }

    private @NotNull UUID getOrCreateUserIdAndUpdateNameIfNeeded(final @NotNull ImportRecord importRecord) {
        final var fullName = FullNameBuilder.buildFullName(
                importRecord.firstname(), importRecord.lastname(), "");

        final var user = userService.getUserByEmail(importRecord.email())
                .orElseGet(() -> createUser(fullName, importRecord.email()));

        if (!user.name().equals(fullName)) {
            final var updatedUser = updateUserName(user, fullName);
            //noinspection DataFlowIssue // id is never null here
            return updatedUser.id();
        }

        //noinspection DataFlowIssue // id is never null here
        return user.id();
    }

    private @NotNull UserDto createUser(final @NotNull String fullName,
                                        final @NotNull String email) {
        return userService.storeUser(
                new UserDto(null, null, null, fullName, email, UserRole.USER)
        );
    }

    private @NotNull UserDto updateUserName(final @NotNull UserDto user,
                                            final @NotNull String fullName) {
        return userService.storeUser(
                new UserDto(user.id(), user.created(), user.updated(), fullName, user.email(), user.role())
        );
    }

    private @NotNull ClubDeskDto toClubDeskDto(final @NotNull UUID userId,
                                               final @NotNull ImportRecord importRecord) {
        return new ClubDeskDto(
                userId,

                null,
                null,

                importRecord.firstname(),
                importRecord.lastname(),
                importRecord.address(),
                importRecord.addressAddition(),
                importRecord.zipCode(),
                importRecord.city(),
                importRecord.country(),

                importRecord.email(),
                importRecord.emailAlternative(),
                importRecord.matrix(),
                importRecord.mastodon(),
                importRecord.linkedin(),

                importRecord.sepaEnabled(),
                importRecord.sepaAccountHolder(),
                importRecord.sepaIban(),
                importRecord.sepaBic(),

                importRecord.jug(),
                DEFAULT_NEWSLETTER_SETTING
        );
    }

}
