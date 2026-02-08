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

import eu.ijug.dukeops.domain.authentication.control.AuthenticationService;
import eu.ijug.dukeops.domain.clubdesk.entity.ClubDeskDto;
import eu.ijug.dukeops.domain.clubdesk.entity.ImportRecord;
import eu.ijug.dukeops.domain.user.control.UserService;
import eu.ijug.dukeops.domain.user.entity.UserDto;
import eu.ijug.dukeops.domain.user.entity.UserRole;
import eu.ijug.dukeops.infra.communication.mail.MailService;
import eu.ijug.dukeops.infra.ui.vaadin.i18n.TranslationProvider;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

final class ClubDeskServiceTest {

    private UserService userService;
    private ClubDeskRepository clubDeskRepository;
    private ClubDeskService service;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        clubDeskRepository = mock(ClubDeskRepository.class);
        final var clubDeskImporter = mock(ClubDeskImporter.class);
        final var authenticationService = mock(AuthenticationService.class);
        final var mailService = mock(MailService.class);
        final var translationProvider = mock(TranslationProvider.class);
        final var dsl = mock(DSLContext.class);
        service = new ClubDeskService(clubDeskRepository, clubDeskImporter, userService,
                authenticationService, mailService, translationProvider, dsl);
    }

    @Test
    void saveImportRecords_shouldReturnZeroForEmptyList() {
        final int saved = service.saveImportRecords(List.of());
        assertThat(saved).isZero();
        verifyNoInteractions(userService, clubDeskRepository);
    }

    @Test
    void saveImportRecords_shouldCreateUserWhenMissing_andUpsertClubDesk() {
        final var testId = UUID.randomUUID();

        final ImportRecord record = new ImportRecord(
                "John",
                "Doe",
                "", "", "", "", null,
                "john.doe@example.com",
                "", "", "", "",
                false, "", "", "",
                "JUG X"
        );

        when(userService.getUserByEmail("john.doe@example.com")).thenReturn(Optional.empty());

        final UserDto createdUser = new UserDto(
                testId,
                LocalDateTime.parse("2026-02-02T10:00:00"),
                LocalDateTime.parse("2026-02-02T10:00:00"),
                "John Doe",
                "john.doe@example.com",
                UserRole.USER
        );
        when(userService.storeUser(any(UserDto.class))).thenReturn(createdUser);

        when(clubDeskRepository.upsert(any(ClubDeskDto.class)))
                .thenAnswer(inv -> inv.getArgument(0, ClubDeskDto.class));

        final int saved = service.saveImportRecords(List.of(record));

        assertThat(saved).isEqualTo(1);

        // Verify user creation called once
        verify(userService).getUserByEmail("john.doe@example.com");
        verify(userService, times(1)).storeUser(any(UserDto.class));

        // Verify repository upsert received expected values
        final ArgumentCaptor<ClubDeskDto> captor = ArgumentCaptor.forClass(ClubDeskDto.class);
        verify(clubDeskRepository).upsert(captor.capture());

        final ClubDeskDto dto = captor.getValue();
        assertThat(dto.id()).isEqualTo(testId);
        assertThat(dto.firstname()).isEqualTo("John");
        assertThat(dto.lastname()).isEqualTo("Doe");
        assertThat(dto.email()).isEqualTo("john.doe@example.com");

        // Newsletter default must be true (opt-out)
        assertThat(dto.newsletter()).isTrue();
    }

    @Test
    void saveImportRecords_shouldUpdateUserNameWhenDifferent() {
        final var testId = UUID.randomUUID();

        final ImportRecord record = new ImportRecord(
                "John",
                "Doe",
                "", "", "", "", null,
                "john.doe@example.com",
                "", "", "", "",
                false, "", "", "",
                ""
        );

        final UserDto existingUser = new UserDto(
                testId,
                LocalDateTime.parse("2026-01-01T10:00:00"),
                LocalDateTime.parse("2026-01-10T10:00:00"),
                "Old Name",
                "john.doe@example.com",
                UserRole.USER
        );

        when(userService.getUserByEmail("john.doe@example.com")).thenReturn(Optional.of(existingUser));

        // On update, service calls storeUser with same id/created/updated and new name
        when(userService.storeUser(any(UserDto.class))).thenAnswer(inv -> inv.getArgument(0, UserDto.class));

        when(clubDeskRepository.upsert(any(ClubDeskDto.class)))
                .thenAnswer(inv -> inv.getArgument(0, ClubDeskDto.class));

        final int saved = service.saveImportRecords(List.of(record));

        assertThat(saved).isEqualTo(1);

        // Expect: 1x getUserByEmail, 1x storeUser for name update (not create)
        verify(userService, times(1)).getUserByEmail("john.doe@example.com");
        verify(userService, times(1)).storeUser(argThat(u ->
                testId.equals(u.id())
                        && u.name().equals("John Doe")
                        && u.email().equals("john.doe@example.com")
                        && u.role() == UserRole.USER
        ));
    }

    @Test
    void saveImportRecords_shouldNotUpdateUserNameWhenSame() {
        final var testId = UUID.randomUUID();

        final ImportRecord record = new ImportRecord(
                "John",
                "Doe",
                "", "", "", "", null,
                "john.doe@example.com",
                "", "", "", "",
                false, "", "", "",
                ""
        );

        final UserDto existingUser = new UserDto(
                testId,
                LocalDateTime.parse("2026-01-01T10:00:00"),
                LocalDateTime.parse("2026-01-10T10:00:00"),
                "John Doe",
                "john.doe@example.com",
                UserRole.USER
        );

        when(userService.getUserByEmail("john.doe@example.com")).thenReturn(Optional.of(existingUser));
        when(clubDeskRepository.upsert(any(ClubDeskDto.class)))
                .thenAnswer(inv -> inv.getArgument(0, ClubDeskDto.class));

        final int saved = service.saveImportRecords(List.of(record));

        assertThat(saved).isEqualTo(1);

        verify(userService, times(1)).getUserByEmail("john.doe@example.com");
        verify(userService, never()).storeUser(any());
        verify(clubDeskRepository, times(1)).upsert(any());
    }

    @Test
    void importClubDeskFile_shouldPropagateImporterException() throws Exception {
        final var importer = mock(ClubDeskImporter.class);
        final var repository = mock(ClubDeskRepository.class);
        final var userService = mock(UserService.class);
        final var authenticationService = mock(AuthenticationService.class);
        final var mailService = mock(MailService.class);
        final var translationProvider = mock(TranslationProvider.class);
        final var dsl = mock(DSLContext.class);

        final var service = new ClubDeskService(repository, importer, userService,
                authenticationService, mailService, translationProvider, dsl);

        final var file = File.createTempFile("clubdesk", ".csv");
        file.deleteOnExit();

        when(importer.parse(any())).thenThrow(new IOException("Simulated parse error"));

        assertThatThrownBy(() -> service.importClubDeskFile(file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Unable to read CSV file:");

        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    @Test
    void getClubDeskForCurrentUser_whenNotLoggedIn_shouldReturnEmpty() {
        final var importer = mock(ClubDeskImporter.class);
        final var repository = mock(ClubDeskRepository.class);
        final var userService = mock(UserService.class);
        final var authenticationService = mock(AuthenticationService.class);
        final var mailService = mock(MailService.class);
        final var translationProvider = mock(TranslationProvider.class);
        final var dsl = mock(DSLContext.class);

        when(authenticationService.getLoggedInUser()).thenReturn(Optional.empty());

        final var service = new ClubDeskService(repository, importer, userService,
                authenticationService, mailService, translationProvider, dsl);

        final var result = service.getClubDeskForCurrentUser();
        assertThat(result).isEmpty();
    }

    @Test
    void getClubDeskForCurrentUser_whenLoggedInUserHasNoId_shouldReturnEmpty() {
        final var importer = mock(ClubDeskImporter.class);
        final var repository = mock(ClubDeskRepository.class);
        final var userService = mock(UserService.class);
        final var authenticationService = mock(AuthenticationService.class);
        final var mailService = mock(MailService.class);
        final var translationProvider = mock(TranslationProvider.class);
        final var dsl = mock(DSLContext.class);

        final var userWithNoId = new UserDto(
                null, LocalDateTime.now(), LocalDateTime.now(),
                "John Doe", "john.doe@example.com", UserRole.USER);
        when(authenticationService.getLoggedInUser()).thenReturn(Optional.of(userWithNoId));

        final var service = new ClubDeskService(repository, importer, userService,
                authenticationService, mailService, translationProvider, dsl);

        final var result = service.getClubDeskForCurrentUser();
        assertThat(result).isEmpty();
    }

    @Test
    @SuppressWarnings("DuplicateExpressions")
    void notifyOffice_skipWhenIdentical() {
        final var importer = mock(ClubDeskImporter.class);
        final var repository = mock(ClubDeskRepository.class);
        final var userService = mock(UserService.class);
        final var authenticationService = mock(AuthenticationService.class);
        final var mailService = mock(MailService.class);
        final var translationProvider = mock(TranslationProvider.class);
        final var dsl = mock(DSLContext.class);

        final var service = new ClubDeskService(repository, importer, userService,
                authenticationService, mailService, translationProvider, dsl);

        final var id = UUID.randomUUID();
        final var clubDeskOriginal = new ClubDeskDto(id, null, null,
                "John", "Doe", "", "", "", "", null,
                "john.doe@example.com", "", "", "", "",
                false, "", "", "",
                "", true);
        final var clubDeskUpdated = new ClubDeskDto(id, null, null,
                "John", "Doe", "", "", "", "", null,
                "john.doe@example.com", "", "", "", "",
                false, "", "", "",
                "", true);

        service.notifyOffice(clubDeskOriginal, clubDeskUpdated, Locale.ENGLISH);

        verifyNoInteractions(mailService);
    }

    @Test
    void notifyOffice_sendMailWithDiff() {
        final var importer = mock(ClubDeskImporter.class);
        final var repository = mock(ClubDeskRepository.class);
        final var userService = mock(UserService.class);
        final var authenticationService = mock(AuthenticationService.class);
        final var mailService = mock(MailService.class);
        final var translationProvider = new TranslationProvider();
        final var dsl = mock(DSLContext.class);

        final var service = new ClubDeskService(repository, importer, userService,
                authenticationService, mailService, translationProvider, dsl);

        final var id = UUID.randomUUID();
        final var clubDeskOriginal = new ClubDeskDto(id, null, null,
                "John", "Doe", "", "", "", "", null,
                "john.doe@example.com", "", "", "", "",
                false, "", "", "",
                "", true);
        final var clubDeskUpdated = new ClubDeskDto(id, null, null,
                "Jane", "Doe", "", "", "", "", null,
                "jane.doe@example.com", "", "", "", "",
                false, "", "", "",
                "", false);

        service.notifyOffice(clubDeskOriginal, clubDeskUpdated, Locale.ENGLISH);

        verify(mailService).sendMail(
                eq("office@ijug.eu"),
                eq("[DukeOps] Neue Stammdaten für Jane Doe"),
                eq("""
                        Vorname: John → Jane
                        E-Mail: john.doe@example.com → jane.doe@example.com
                        Vereinsinformationen: ja → nein"""));

        verify(mailService).sendMail(
                eq("john.doe@example.com"),
                eq("[DukeOps] Your master data has been updated"),
                contains("""
                        First name: John → Jane
                        Email: john.doe@example.com → jane.doe@example.com
                        Club information: yes → no"""));

        verify(mailService).sendMail(
                eq("jane.doe@example.com"),
                eq("[DukeOps] Your master data has been updated"),
                contains("""
                        First name: John → Jane
                        Email: john.doe@example.com → jane.doe@example.com
                        Club information: yes → no"""));
    }

}
