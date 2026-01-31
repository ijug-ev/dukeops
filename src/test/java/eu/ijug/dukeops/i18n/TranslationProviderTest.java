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
package eu.ijug.dukeops.i18n;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Locale.ENGLISH;
import static java.util.Locale.GERMAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class TranslationProviderTest {

    private TranslationProvider translationProvider;

    @BeforeEach
    void setUp() {
        this.translationProvider = new TranslationProvider();
    }

    @Test
    void testProviderLocales() {
        assertThat(translationProvider.getProvidedLocales())
                .containsExactlyInAnyOrder(ENGLISH, GERMAN);
    }

    @Test
    void testGetMessageInEnglish() {
        assertThat(translationProvider.getTranslation("test.simpleMessage", ENGLISH))
                .isEqualTo("Test message in English");
    }

    @Test
    void testTranslationInGerman() {
        assertThat(translationProvider.getTranslation("test.simpleMessage", GERMAN))
                .isEqualTo("Testmeldung auf Deutsch");
    }

    @Test
    void testSwissGermanFallbackToGerman() {
        assertThat(translationProvider.getTranslation("test.simpleMessage", Locale.forLanguageTag("de-CH")))
                .isEqualTo("Testmeldung auf Deutsch");
    }

    @Test
    void testFallbackToEnglish() {
        assertThat(translationProvider.getTranslation("test.simpleMessage", Locale.ITALIAN))
                .isEqualTo("Test message in English");
    }

    @Test
    void testFallbackWhenLocaleIsNull() {
        assertThat(translationProvider.getTranslation("test.simpleMessage", null))
                .isEqualTo("Test message in English");
    }

    @Test
    void testWithPlaceholder() {
        assertThat(translationProvider.getTranslation("test.placeholder", ENGLISH, "foobar"))
                .isEqualTo("This is a placeholder: foobar");
    }

    @Test
    void testWithTwoPlaceholders() {
        assertThat(translationProvider.getTranslation("test.twoPlaceholders", ENGLISH, "foo", "bar"))
                .isEqualTo("These are two placeholders: foo and bar");
    }

    @ParameterizedTest
    @MethodSource("testWithNamedPlaceholderArguments")
    void testWithNamedPlaceholder(final int count, final Locale locale, final String expectedText) {
        final var params = Map.of("count", count);
        assertThat(translationProvider.getTranslation("test.namedPlaceholder", locale, params))
                .isEqualTo(expectedText);
    }

    private static Stream<Arguments> testWithNamedPlaceholderArguments() {
        return Stream.of(
                arguments(0, ENGLISH, "no messages"),
                arguments(1, ENGLISH, "one message"),
                arguments(2, ENGLISH, "2 messages"),
                arguments(0, GERMAN, "keine Nachrichten"),
                arguments(1, GERMAN, "eine Nachricht"),
                arguments(2, GERMAN, "2 Nachrichten")
        );
    }

    @Test
    void testWithMissingPlaceholder() {
        assertThat(translationProvider.getTranslation("test.placeholder", ENGLISH))
                .isEqualTo("This is a placeholder: {0}");
    }

    @Test
    void testMissingPlaceholder() {
        assertThat(translationProvider.getTranslation("test.simpleMessage", ENGLISH, "foobar"))
                .isEqualTo("Test message in English");
    }

    @Test
    void testMissingTranslation() {
        assertThat(translationProvider.getTranslation("test.missingTranslation", ENGLISH))
                .isEqualTo("!en: test.missingTranslation");
        assertThat(translationProvider.getTranslation("test.missingTranslation", GERMAN))
                .isEqualTo("!de: test.missingTranslation");
    }

}
