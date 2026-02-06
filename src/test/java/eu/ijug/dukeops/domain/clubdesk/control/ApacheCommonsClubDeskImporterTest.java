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

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class ApacheCommonsClubDeskImporterTest {

    private final ApacheCommonsClubDeskImporter importer = new ApacheCommonsClubDeskImporter();

    @Test
    void parse_shouldReadMinimalValidCsv() throws IOException {
        final String csv = """
                "E-Mail";"Vorname";"Nachname";"Adresse";"Adress-Zusatz";"PLZ";"Ort";"Land";"E-Mail Alternativ";"Matrix";"Mastodon";"LinkedIn";"SEPA-Lastschrift erlauben";"Mandatsreferenz";"Mandat Unterschriftsdatum";"Lastschriftart";"Letzter Lastschrifteinzug";"Kontoinhaber";"IBAN";"BIC";"Java User Group"
                "john.doe@example.com";"John";"Doe";"";"";"";"";"";"";"";"";"";"Nein";"";"";"";"";"";"";"";""
                """;

        final var records = importer.parse(Reader.of(csv));

        assertThat(records).hasSize(1);
        assertThat(records.getFirst().email()).isEqualTo("john.doe@example.com");
        assertThat(records.getFirst().firstname()).isEqualTo("John");
        assertThat(records.getFirst().lastname()).isEqualTo("Doe");
        assertThat(records.getFirst().sepaEnabled()).isFalse();
    }

    @Test
    void parse_shouldFailWhenRequiredEmailBlank() {
        final String csv = """
                "E-Mail";"Vorname";"Nachname";"Adresse";"Adress-Zusatz";"PLZ";"Ort";"Land";"E-Mail Alternativ";"Matrix";"Mastodon";"LinkedIn";"SEPA-Lastschrift erlauben";"Mandatsreferenz";"Mandat Unterschriftsdatum";"Lastschriftart";"Letzter Lastschrifteinzug";"Kontoinhaber";"IBAN";"BIC";"Java User Group"
                "";"";"";"";"";"";"";"";"";"";"";"";"Nein";"";"";"";"";"";"";"";""
                """;

        assertThatThrownBy(() -> importer.parse(Reader.of(csv)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required value for CSV column 'E-Mail'");
    }

    @Test
    void parse_shouldFailWhenExpectedHeaderIsMissing() {
        final String csv = """
            "E-Mail";"Nachname";"Adresse";"Adress-Zusatz";"PLZ";"Ort";"Land";"E-Mail Alternativ";"Matrix";"Mastodon";"LinkedIn";"SEPA-Lastschrift erlauben";"Mandatsreferenz";"Mandat Unterschriftsdatum";"Lastschriftart";"Letzter Lastschrifteinzug";"Kontoinhaber";"IBAN";"BIC";"Java User Group"
            "john.doe@example.com";"Doe";"";"";"";"";"";"";"";"";"";"Nein";"";"";"";"";"";"";"";""
            """;

        assertThatThrownBy(() -> importer.parse(Reader.of(csv)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CSV header missing expected column: 'Vorname'");
    }

    @Test
    void parse_shouldFailWhenRowHasFewerColumnsThanHeader() {
        final String csv = """
            "E-Mail";"Vorname";"Nachname";"Adresse";"Adress-Zusatz";"PLZ";"Ort";"Land";"E-Mail Alternativ";"Matrix";"Mastodon";"LinkedIn";"SEPA-Lastschrift erlauben";"Mandatsreferenz";"Mandat Unterschriftsdatum";"Lastschriftart";"Letzter Lastschrifteinzug";"Kontoinhaber";"IBAN";"BIC";"Java User Group"
            "john.doe@example.com";"John";"Doe"
            """;

        assertThatThrownBy(() -> importer.parse(Reader.of(csv)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Index for header 'Adresse'");
    }

    @Test
    void parse_shouldParseSepaEnabledJaAsTrue() throws IOException {
        final String csv = """
            "E-Mail";"Vorname";"Nachname";"Adresse";"Adress-Zusatz";"PLZ";"Ort";"Land";"E-Mail Alternativ";"Matrix";"Mastodon";"LinkedIn";"SEPA-Lastschrift erlauben";"Mandatsreferenz";"Mandat Unterschriftsdatum";"Lastschriftart";"Letzter Lastschrifteinzug";"Kontoinhaber";"IBAN";"BIC";"Java User Group"
            "john.doe@example.com";"John";"Doe";"";"";"";"";"";"";"";"";"";"Ja";"";"";"";"";"";"";"";""
            """;

        final var records = importer.parse(Reader.of(csv));

        assertThat(records).hasSize(1);
        assertThat(records.getFirst().sepaEnabled()).isTrue();
    }

    @Test
    void parse_shouldParseSepaEnabledTruthyVariantsAsTrue() throws IOException {
        final String header = """
            "E-Mail";"Vorname";"Nachname";"Adresse";"Adress-Zusatz";"PLZ";"Ort";"Land";"E-Mail Alternativ";"Matrix";"Mastodon";"LinkedIn";"SEPA-Lastschrift erlauben";"Mandatsreferenz";"Mandat Unterschriftsdatum";"Lastschriftart";"Letzter Lastschrifteinzug";"Kontoinhaber";"IBAN";"BIC";"Java User Group"
            """;

        for (final var truthy : List.of("true", "1", "yes")) {
            final String csv = header + """
                "john.doe@example.com";"John";"Doe";"";"";"";"";"";"";"";"";"";"%s";"";"";"";"";"";"";"";""
                """.formatted(truthy);

            final var record = importer.parse(Reader.of(csv)).getFirst();
            assertThat(record.sepaEnabled()).isTrue();
        }
    }

    @Test
    void parse_shouldForwardIOExceptionOfReader() {
        @SuppressWarnings("resource")
        final Reader throwingReader = new Reader() {
            @Override
            public int read(final char @NonNull [] cbuf, final int off, final int len) throws IOException {
                throw new IOException("boom");
            }

            @Override
            public void close() {
                // nothing
            }
        };

        assertThatThrownBy(() -> importer.parse(throwingReader))
                .isInstanceOf(IOException.class)
                .hasMessage("boom");
    }

}
