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
package eu.ijug.dukeops.infra.ui.vaadin.i18n;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.ULocale;
import com.vaadin.flow.i18n.I18NProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * <p>Spring-managed {@link I18NProvider} implementation that provides translations
 * for Vaadin UI components based on Java {@link ResourceBundle}s.</p>
 *
 * <p>Translations are loaded from property files located under
 * {@code src/main/resources/vaadin-i18n/} and support both positional and named parameters
 * using ICU {@link MessageFormat}.</p>
 */
@Component
public class TranslationProvider implements I18NProvider {

    /**
     * <p>Base name of the resource bundles containing the translations.</p>
     */
    private static final @NotNull String BUNDLE_BASENAME =
            com.vaadin.flow.i18n.DefaultI18NProvider.BUNDLE_FOLDER + "."
                    + com.vaadin.flow.i18n.DefaultI18NProvider.BUNDLE_FILENAME;

    /**
     * <p>List of locales that are supported by this translation provider.</p>
     */
    private static final @NotNull List<Locale> PROVIDED_LOCALES = List.of(
            Locale.ENGLISH, Locale.GERMAN);

    /**
     * <p>Creates a new {@code TranslationProvider} and sets the JVM default locale to English.</p>
     *
     * <p>This ensures a deterministic fallback behaviour in case no locale is explicitly provided.</p>
     */
    public TranslationProvider() {
        Locale.setDefault(Locale.ENGLISH);
    }

    /**
     * <p>Returns the list of locales supported by this {@link I18NProvider}.</p>
     *
     * @return an immutable list of provided locales
     */
    @Override
    public @NotNull List<Locale> getProvidedLocales() {
        return PROVIDED_LOCALES;
    }

    /**
     * <p>Resolves a translation for the given key and locale and applies optional parameters.</p>
     *
     * <p>If no locale is provided, English is used as the default. When a translation key is missing, a placeholder
     * string is returned instead of throwing an exception.</p>
     *
     * <p>Parameter substitution supports positional arguments ({@code {0}, {1}, ...}) and named arguments when a
     * single {@link Map} is provided.</p>
     *
     * @param key the translation key to resolve
     * @param locale the locale to use, or {@code null} to fall back to English
     * @param params optional parameters for message formatting
     * @return the resolved and formatted translation, or a placeholder if the key is missing
     */
    @Override
    public @NotNull String getTranslation(final @NotNull String key,
                                          final @Nullable Locale locale,
                                          final @NotNull Object... params) {
        final var effectiveLocale = locale != null ? locale : Locale.ENGLISH;

        final String pattern;
        try {
            final var bundle = ResourceBundle.getBundle(BUNDLE_BASENAME, effectiveLocale);
            pattern = bundle.getString(key);
        } catch (final MissingResourceException ex) {
            // Missing translation → return placeholder
            return "!" + LocaleUtil.getLanguageCode(effectiveLocale).toLowerCase(Locale.ENGLISH) + ": " + key;
        }

        // No placeholder → return directly
        if (params.length == 0) {
            return pattern;
        }

        final var uLocale = ULocale.forLocale(effectiveLocale);
        final var icuFormat = new MessageFormat(pattern, uLocale);

        // Optional: Support named arguments when a map is the first argument
        if (params.length == 1 && params[0] instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            final var namedArgs = (Map<String, Object>) map;
            return icuFormat.format(namedArgs);
        }

        // Standard: Position arguments {0}, {1}, ...
        return icuFormat.format(params);
    }

}
