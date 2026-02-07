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
package eu.ijug.dukeops.domain.clubdesk.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class Country {

    private static final @NotNull Set<String> ISO2 = Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2);

    private static final @NotNull Map<Locale, Map<String, String>> NAME_TO_ISO_CACHE = new ConcurrentHashMap<>();

    private final String iso2;

    private Country(final @NotNull String iso2) {
        this.iso2 = iso2;
    }

    public static @NotNull Country ofIso2(final @NotNull String iso2) {
        return new Country(iso2);
    }

    public @NotNull String iso2() {
        return iso2;
    }

    public @NotNull String displayName(final @NotNull Locale locale) {
        return new Locale.Builder().setRegion(iso2).build().getDisplayCountry(locale);
    }

    public static @Nullable Country fromName(final @NotNull String name, final @NotNull Locale locale) {
        if (name.isBlank()) {
            return null;
        }
        final var key = normalizeName(name, locale);
        final var code = nameToIso(locale).get(key);
        return ofIso2(code);
    }

    public static @NotNull List<@NotNull Country> all(final @NotNull Locale sortLocale) {
        return ISO2.stream()
                .map(Country::ofIso2)
                .sorted(Comparator
                        .comparing((Country country) -> country.displayName(sortLocale))
                        .thenComparing(Country::iso2))
                .toList();
    }

    private static @NotNull Map<@NotNull String, @NotNull String> nameToIso(final @NotNull Locale locale) {
        return NAME_TO_ISO_CACHE.computeIfAbsent(locale, loc ->
            ISO2.stream().collect(Collectors.toUnmodifiableMap(
                code -> normalizeName(new Locale.Builder().setRegion(code).build().getDisplayCountry(loc), loc),
                code -> code)));
    }

    private static @NotNull String normalizeName(final @NotNull String name, final @NotNull Locale locale) {
        var normalized = name.trim().toLowerCase(locale);
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFKD).replaceAll("\\p{M}+", "");
        normalized = normalized.replace("ß", "ss");
        normalized = normalized.replaceAll("\\s+", " ");
        return normalized;
    }

    @Override
    public @NotNull String toString() {
        return iso2;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        return (o instanceof Country other) && Objects.equals(this.iso2, other.iso2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(iso2);
    }

}
