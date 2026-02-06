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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * <p>Spring-managed {@link ClubDeskImporter} implementation that parses ClubDesk CSV exports using
 * Apache Commons CSV.</p>
 *
 * <p>The importer converts each CSV row into an {@link ImportRecord}, applies strict validation for
 * required fields, and performs locale-specific parsing for dates and boolean values.</p>
 */
@Component
public class ApacheCommonsClubDeskImporter implements ClubDeskImporter {

    private static final @NotNull DateTimeFormatter DATE_DD_MM_YYYY =
            DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.GERMAN);

    /**
     * <p>Parses the provided CSV input and converts all valid rows into immutable {@link ImportRecord} instances.</p>
     *
     * <p>The method expects a semicolon-separated CSV with a header row matching the ClubDesk export format.
     * Missing required values or malformed date fields result in an {@link IllegalArgumentException}.</p>
     *
     * @param reader the reader providing the CSV content
     * @return an immutable list of parsed import records
     * @throws IllegalStateException if the CSV content cannot be read
     * @throws IllegalArgumentException if required columns or values are missing or invalid
     * @throws IOException if an I/O error occurs during reading
     */
    @Override
    public @NotNull List<ImportRecord> parse(final @NotNull Reader reader) throws IOException {
        try (var parser = CSVParser.parse(reader, clubDeskFormat())) {
            final List<ImportRecord> result = new ArrayList<>();

            for (final var record : parser) {
                result.add(toImportRecord(record));
            }

            return List.copyOf(result);
        }
    }

    private static @NotNull CSVFormat clubDeskFormat() {
        return CSVFormat.Builder.create()
                .setDelimiter(';')
                .setQuote('"')
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreSurroundingSpaces(true)
                .setIgnoreEmptyLines(true)
                .get();
    }

    private static @NotNull ImportRecord toImportRecord(final @NotNull CSVRecord row) {
        // Required fields
        final var email = required(row, "E-Mail");

        // Optional fields
        final var firstname = optional(row, "Vorname");
        final var lastname = optional(row, "Nachname");
        final var address = optional(row, "Adresse");
        final var addressAddition = optional(row, "Adress-Zusatz");
        final var zip = optional(row, "PLZ");
        final var city = optional(row, "Ort");
        final var country = optional(row, "Land");

        final var emailAlt = optional(row, "E-Mail Alternativ");
        final var matrix = optional(row, "Matrix");
        final var mastodon = optional(row, "Mastodon");
        final var linkedin = optional(row, "LinkedIn");

        final var sepaEnabled = parseBoolean(optional(row, "SEPA-Lastschrift erlauben"));
        final var sepaAccountHolder = optional(row, "Kontoinhaber");
        final var sepaIban = optional(row, "IBAN");
        final var sepaBic = optional(row, "BIC");

        final var jug = optional(row, "Java User Group");

        return new ImportRecord(
                firstname,
                lastname,
                address,
                addressAddition,
                zip,
                city,
                country,
                email,
                emailAlt,
                matrix,
                mastodon,
                linkedin,
                sepaEnabled,
                sepaAccountHolder,
                sepaIban,
                sepaBic,
                jug
        );
    }

    private static @NotNull String required(final @NotNull CSVRecord row,
                                            @SuppressWarnings("SameParameterValue") final @NotNull String header) {
        final var value = optional(row, header);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Missing required value for CSV column '" + header
                    + "' in row " + row.getRecordNumber());
        }
        return value;
    }

    private static @NotNull String optional(final @NotNull CSVRecord row,
                                            final @NotNull String header) {
        if (!row.isMapped(header)) {
            throw new IllegalArgumentException("CSV header missing expected column: '" + header + "'");
        }
        return row.get(header).trim();
    }

    private static boolean parseBoolean(final @NotNull String value) {
        final var v = value.trim().toLowerCase(Locale.ROOT);
        return v.equals("ja") || v.equals("true") || v.equals("1") || v.equals("yes");
    }

}
