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
import eu.ijug.dukeops.domain.clubdesk.entity.Country;
import eu.ijug.dukeops.domain.clubdesk.entity.ImportRecord;
import eu.ijug.dukeops.domain.user.control.FullNameBuilder;
import eu.ijug.dukeops.domain.user.control.UserService;
import eu.ijug.dukeops.domain.user.entity.UserDto;
import eu.ijug.dukeops.domain.user.entity.UserRole;
import eu.ijug.dukeops.infra.communication.mail.MailService;
import eu.ijug.dukeops.infra.ui.vaadin.i18n.TranslationProvider;
import org.apache.commons.codec.binary.Base32;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static eu.ijug.dukeops.infra.persistence.jooq.generated.Tables.CLUBDESK;

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
    private final @NotNull AuthenticationService authenticationService;
    private final @NotNull MailService mailService;
    private final @NotNull TranslationProvider translationProvider;
    private final @NotNull DSLContext dsl;

    /**
     * <p>Creates a new ClubDesk service using the required collaborators.</p>
     *
     * @param clubDeskRepository the repository used to persist ClubDesk records
     * @param clubDeskImporter the importer used to parse ClubDesk CSV files
     * @param userService the user service used to resolve or create users
     * @param authenticationService the authentication service used to access the current user context
     * @param mailService the mail service used to send notification emails after relevant data changes
     * @param translationProvider the translation provider used to localize email content
     * @param dsl the jOOQ DSL context used to execute database operations
     */
    public ClubDeskService(final @NotNull ClubDeskRepository clubDeskRepository,
                           final @NotNull ClubDeskImporter clubDeskImporter,
                           final @NotNull UserService userService,
                           final @NotNull AuthenticationService authenticationService,
                           final @NotNull MailService mailService,
                           final @NotNull TranslationProvider translationProvider,
                           final @NotNull DSLContext dsl) {
        super();
        this.clubDeskRepository = clubDeskRepository;
        this.clubDeskImporter = clubDeskImporter;
        this.userService = userService;
        this.authenticationService = authenticationService;
        this.mailService = mailService;
        this.translationProvider = translationProvider;
        this.dsl = dsl;
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
                importRecord.sepaMandateReference(),
                importRecord.sepaIban(),
                importRecord.sepaBic(),

                importRecord.jug(),
                DEFAULT_NEWSLETTER_SETTING
        );
    }

    /**
     * <p>Returns the {@link ClubDeskDto} associated with the currently authenticated user, if available.</p>
     *
     * <p>The method first resolves the currently logged-in user via the authentication subsystem and then
     * attempts to load the corresponding ClubDesk record using the user's unique identifier. If no user
     * is logged in, or if the logged-in user does not have a valid identifier, an empty {@link Optional}
     * is returned.</p>
     *
     * <p>An error is logged if an authenticated user exists but has no ID, as this represents an invalid
     * application state. A warning is logged if no user is currently authenticated.</p>
     *
     * @return an {@link Optional} containing the {@link ClubDeskDto} for the current user, or an empty
     *         {@link Optional} if no matching data can be resolved
     */
    public Optional<ClubDeskDto> getClubDeskForCurrentUser() {
        final var userData = authenticationService.getLoggedInUser();
        if (userData.isPresent()) {
            final var id = userData.orElse(null).id();
            if (id == null) {
                LOGGER.error("Logged in user has no ID, cannot fetch ClubDesk data. This is an invalid state!");
                return Optional.empty();
            }
            return getById(id);
        }
        LOGGER.warn("No user is currently logged in, cannot fetch ClubDesk data.");
        return Optional.empty();
    }

    /**
     * <p>Fetches the {@link ClubDeskDto} for the given user ID from the database.</p>
     *
     * <p>If no record exists for the provided ID, an empty {@link Optional} is returned.</p>
     *
     * @param id the unique identifier of the user whose ClubDesk data should be retrieved
     * @return an {@link Optional} containing the {@link ClubDeskDto} if found, otherwise an empty {@link Optional}
     */
    private @NotNull Optional<ClubDeskDto> getById(final @NotNull UUID id) {
        final var clubDeskDto = dsl.selectFrom(CLUBDESK)
                .where(CLUBDESK.ID.eq(id))
                .fetchOptionalInto(ClubDeskDto.class);
        LOGGER.debug("Fetched ClubDesk data for user ID {}: {}", id, clubDeskDto);
        return clubDeskDto;
    }

    /**
     * <p>Retrieves a list of all distinct Java User Group names stored in ClubDesk.</p>
     *
     * <p>Only non-null and non-empty group names are included, and the result is sorted alphabetically.</p>
     *
     * @return a list of distinct Java User Group names
     */
    public @NotNull List<String> getAllJavaUserGroups() {
        return dsl.selectDistinct(CLUBDESK.JUG)
                .from(CLUBDESK)
                .where(CLUBDESK.JUG.isNotNull()
                        .and(CLUBDESK.JUG.notEqual("")))
                .orderBy(CLUBDESK.JUG.asc())
                .fetchInto(String.class);
    }

    /**
     * <p>Saves the given ClubDesk data by creating a new record or updating an existing one.</p>
     *
     * <p>The method delegates to the underlying repository, which performs an upsert operation.
     * This means that an existing record is updated if it already exists, otherwise a new record
     * is created.</p>
     *
     * @param clubDesk the ClubDesk data to be persisted; must not be {@code null}
     * @return the persisted ClubDesk data as stored in the database; never {@code null}
     */
    public @NotNull ClubDeskDto save(final @NotNull ClubDeskDto clubDesk) {
        final var savedClubDesk = clubDeskRepository.upsert(clubDesk);
        final var user = userService.getUserById(savedClubDesk.id()).orElseThrow();
        if (!user.email().equals(savedClubDesk.email())) {
            final var updatedUser = userService.storeUser(new UserDto(user.id(), user.created(), user.updated(),
                    user.name(), savedClubDesk.email(), user.role()));
            userService.storeUser(updatedUser);
        }
        return savedClubDesk;
    }

    /**
     * <p>Sends a notification email to the iJUG office containing a summary of all changes made to the
     * ClubDesk data of a member.</p>
     *
     * <p>The email includes only those fields whose values differ between the original and the updated
     * data set. For each changed field, both the previous and the new value are listed in a
     * human-readable diff format.</p>
     *
     * <p>If no differences are detected, no email is sent.</p>
     *
     * @param clubDeskOriginal the original ClubDesk data before the update; must not be {@code null}
     * @param clubDeskUpdated the updated ClubDesk data after the change; must not be {@code null}
     */
    public void notifyOffice(final @NotNull ClubDeskDto clubDeskOriginal,
                             final @NotNull ClubDeskDto clubDeskUpdated,
                             final @NotNull Locale memberLocale) {
        final var officeLocale = Locale.GERMAN;
        final var diffForOffice = createDiff(clubDeskOriginal, clubDeskUpdated, officeLocale);
        if (diffForOffice.isEmpty()) {
            return;
        }

        final var mailToOfficeSubject = translationProvider.getTranslation(
                "domain.clubdesk.control.ClubDeskService.email.office.subject", officeLocale,
                clubDeskUpdated.firstname(), clubDeskUpdated.lastname());
        mailService.sendMail("office@ijug.eu", mailToOfficeSubject, diffForOffice);

        final var diffForMember = createDiff(clubDeskOriginal, clubDeskUpdated, memberLocale);
        final var mailToMemberSubject = translationProvider.getTranslation(
                "domain.clubdesk.control.ClubDeskService.email.member.subject", memberLocale);
        final var mailToMemberBody = translationProvider.getTranslation(
                "domain.clubdesk.control.ClubDeskService.email.member.body", memberLocale,
                clubDeskUpdated.firstname(), clubDeskUpdated.lastname(), diffForMember);
        mailService.sendMail(clubDeskOriginal.email(), mailToMemberSubject, mailToMemberBody);

        if (!clubDeskOriginal.email().equals(clubDeskUpdated.email())) {
            mailService.sendMail(clubDeskUpdated.email(), mailToMemberSubject, mailToMemberBody);
        }
    }

    private @NonNull String createDiff(final @NonNull ClubDeskDto clubDeskOriginal,
                                              final @NonNull ClubDeskDto clubDeskUpdated,
                                              final @NotNull Locale locale) {
        final var lines = new StringBuilder();

        addDiff(lines, locale, "firstname",
                clubDeskOriginal.firstname(), clubDeskUpdated.firstname());

        addDiff(lines, locale, "lastname",
                clubDeskOriginal.lastname(), clubDeskUpdated.lastname());

        addDiff(lines, locale, "address",
                clubDeskOriginal.address(), clubDeskUpdated.address());

        addDiff(lines, locale, "addressAddition",
                clubDeskOriginal.addressAddition(), clubDeskUpdated.addressAddition());

        addDiff(lines, locale, "zipCode",
                clubDeskOriginal.zipCode(), clubDeskUpdated.zipCode());

        addDiff(lines, locale, "city",
                clubDeskOriginal.city(), clubDeskUpdated.city());

        addDiff(lines, locale, "country",
                displayCountry(clubDeskOriginal.country(), locale),
                displayCountry(clubDeskUpdated.country(), locale));

        addDiff(lines, locale, "email",
                clubDeskOriginal.email(), clubDeskUpdated.email());

        addDiff(lines, locale, "emailAlternative",
                clubDeskOriginal.emailAlternative(), clubDeskUpdated.emailAlternative());

        addDiff(lines, locale, "matrix",
                clubDeskOriginal.matrix(), clubDeskUpdated.matrix());

        addDiff(lines, locale, "mastodon",
                clubDeskOriginal.mastodon(), clubDeskUpdated.mastodon());

        addDiff(lines, locale, "linkedin",
                clubDeskOriginal.linkedin(), clubDeskUpdated.linkedin());

        addDiff(lines, locale, "sepaEnabled",
                clubDeskOriginal.sepaEnabled(), clubDeskUpdated.sepaEnabled());

        addDiff(lines, locale, "sepaAccountHolder",
                clubDeskOriginal.sepaAccountHolder(), clubDeskUpdated.sepaAccountHolder());

        addDiff(lines, locale, "sepaMandateReference",
                clubDeskOriginal.sepaMandateReference(), clubDeskUpdated.sepaMandateReference());

        addDiff(lines, locale, "sepaIban",
                clubDeskOriginal.sepaIban(), clubDeskUpdated.sepaIban());

        addDiff(lines, locale, "sepaBic",
                clubDeskOriginal.sepaBic(), clubDeskUpdated.sepaBic());

        addDiff(lines, locale, "javaUserGroup",
                clubDeskOriginal.jug(), clubDeskUpdated.jug());

        addDiff(lines, locale, "newsletter",
                clubDeskOriginal.newsletter(), clubDeskUpdated.newsletter());

        return lines.toString().trim();
    }

    /**
     * <p>Determines whether two values differ from each other.</p>
     *
     * <p>The comparison is {@code null}-safe and relies on {@link Objects#equals(Object, Object)}
     * to check for value equality.</p>
     *
     * @param oldValue the original value; may be {@code null}
     * @param newValue the updated value; may be {@code null}
     * @return {@code true} if the values are different, {@code false} if they are equal
     */
    private static boolean changed(final @Nullable Object oldValue,
                                   final @Nullable Object newValue) {
        return !Objects.equals(oldValue, newValue);
    }

    /**
     * <p>Adds a formatted diff line to the given output buffer if the provided values differ.</p>
     *
     * <p>The method resolves a human-readable field label via the translation provider and appends a
     * single line describing the change in the form {@code &lt;label&gt;: &lt;old&gt; → &lt;new&gt;}.</p>
     *
     * <p>If the values are equal, the buffer remains unchanged.</p>
     *
     * @param lines the buffer to which the diff line is appended; must not be {@code null}
     * @param locale the locale used to resolve the translated field label; must not be {@code null}
     * @param fieldKey the translation key suffix identifying the field; must not be {@code null}
     * @param oldValue the original value; may be {@code null}
     * @param newValue the updated value; may be {@code null}
     */
    private void addDiff(final @NotNull StringBuilder lines,
                         final @NotNull Locale locale,
                         final @NotNull String fieldKey,
                         final @Nullable Object oldValue,
                         final @Nullable Object newValue) {

        if (!changed(oldValue, newValue)) {
            return;
        }

        final var label = translationProvider.getTranslation(
                "domain.clubdesk.boundary.ClubDeskEditView.label." + fieldKey, locale);

        lines.append(label)
                .append(": ")
                .append(formatForDiff(oldValue, locale))
                .append(" → ")
                .append(formatForDiff(newValue, locale))
                .append('\n');
    }

    /**
     * <p>Formats a value for inclusion in a human-readable diff output.</p>
     *
     * <p>{@code null} values are rendered as "[empty]" to clearly indicate missing data.
     * Boolean values are converted into readable textual representations. All other values
     * are formatted using their {@link Object#toString()} representation.</p>
     *
     * @param value the value to format; may be {@code null}
     * @return a non-null, human-readable string representation of the value
     */
    private @NotNull String formatForDiff(final @Nullable Object value,
                                          final @NotNull Locale locale) {
        final var empty = translationProvider.getTranslation("domain.clubdesk.control.ClubDeskService.diff.empty", locale);
        final var yes = translationProvider.getTranslation("domain.clubdesk.control.ClubDeskService.diff.yes", locale);
        final var no = translationProvider.getTranslation("domain.clubdesk.control.ClubDeskService.diff.no", locale);

        if (value == null) {
            return "[" + empty + "]";
        }
        if (value instanceof Boolean b) {
            return b ? yes : no;
        }
        final var text = value.toString();
        return text.isBlank() ? "[" + empty + "]" : text;
    }

    /**
     * <p>Returns a localized display name for the given country.</p>
     *
     * <p>If the country is {@code null}, the method returns {@code null}. Otherwise, the country name
     * is resolved using the provided locale.</p>
     *
     * @param country the country to display; may be {@code null}
     * @param locale the locale used to resolve the display name; must not be {@code null}
     * @return the localized country name, or {@code null} if the country is {@code null}
     */
    private static @Nullable String displayCountry(final @Nullable Country country,
                                                   final @NotNull Locale locale) {
        return country == null ? null : country.displayName(locale);
    }

    /**
     * <p>Generates a deterministic SEPA mandate reference for the given ClubDesk record.</p>
     *
     * <p>The mandate reference is derived from the record's UUID by applying an SHA-256 hash,
     * encoding the result using Base32, and truncating it to a fixed length. This ensures
     * a short, human-readable, and stable identifier without exposing internal UUIDs.</p>
     *
     * <p>The generated reference uses the fixed prefix {@code IJUG-} and must be generated
     * exactly once when SEPA is first enabled for the record.</p>
     *
     * @param clubDeskDto the ClubDesk record for which the mandate reference is generated
     * @return the generated SEPA mandate reference
     */
    public static @NotNull String generateSepaMandateReference(final @NotNull ClubDeskDto clubDeskDto) {
        try {
            final var digest = MessageDigest.getInstance("SHA-256");
            final var hash = digest.digest(clubDeskDto.id().toString().getBytes(StandardCharsets.UTF_8));

            final var base32 = new Base32()
                    .encodeToString(hash)
                    .replace("=", "")
                    .substring(0, 12);

            return "IJUG-" + base32;
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", e);
        }
    }
}
