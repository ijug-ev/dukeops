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

import eu.ijug.dukeops.domain.clubdesk.entity.ImportRecord;
import eu.ijug.dukeops.domain.user.control.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

final class ClubDeskServiceImportTest {

    @TempDir
    Path tempDir;

    @Test
    void importClubDeskFile_shouldDelegateToImporter_andReturnItsResult() throws IOException {
        final var clubDeskRepository = mock(ClubDeskRepository.class);
        final var clubDeskImporter = mock(ClubDeskImporter.class);
        final var userService = mock(UserService.class);

        final var service = new ClubDeskService(clubDeskRepository, clubDeskImporter, userService);

        final var csv = tempDir.resolve("clubdesk.csv");
        Files.writeString(csv, "dummy", StandardCharsets.ISO_8859_1);

        final var expected = new ImportRecord(
                "John", "Doe",
                "", "", "", "", "",
                "john.doe@example.com",
                "", "", "", "",
                false, null, null, null,
                ""
        );
        when(clubDeskImporter.parse(any(Reader.class))).thenReturn(List.of(expected));

        final var result = service.importClubDeskFile(csv.toFile());

        assertThat(result).containsExactly(expected);
        verify(clubDeskImporter, times(1)).parse(any(Reader.class));
        verifyNoMoreInteractions(clubDeskImporter);
    }

    @Test
    void importClubDeskFile_shouldFailWhenNotAFile() {
        final var clubDeskRepository = mock(ClubDeskRepository.class);
        final var clubDeskImporter = mock(ClubDeskImporter.class);
        final var userService = mock(UserService.class);

        final var service = new ClubDeskService(clubDeskRepository, clubDeskImporter, userService);

        final var directoryAsFile = tempDir.toFile(); // is a directory, not a file

        assertThatThrownBy(() -> service.importClubDeskFile(directoryAsFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not a file:");
    }

}
