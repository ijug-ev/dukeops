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
package eu.ijug.dukeops.infra.ui.vaadin.init;

import com.vaadin.flow.component.UI;
import eu.ijug.dukeops.infra.config.AppConfig;
import eu.ijug.dukeops.infra.ui.vaadin.control.LinkUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

/**
 * <p>Initializes {@link LinkUtil} with the configured base URL when a Vaadin {@link UI} is created.</p>
 *
 * <p>If {@link AppConfig#baseUrl()} is blank, no base URL is set and {@link LinkUtil} keeps its default behavior.</p>
 */
@Component
public class LinkUtilInitializer implements UIInitializer {

    private final @NotNull AppConfig appConfig;

    /**
     * <p>Creates a new initializer using the provided application configuration.</p>
     *
     * @param appConfig the application configuration containing the base URL
     */
    public LinkUtilInitializer(final @NotNull AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * <p>Configures {@link LinkUtil} with the base URL from {@link AppConfig} for the current application instance.</p>
     *
     * @param ui the Vaadin UI instance being initialized
     */
    @Override
    public void initialize(final @NotNull UI ui) {
        final var baseUrl = appConfig.baseUrl();
        if (!baseUrl.isBlank()) {
            LinkUtil.setBaseUrl(baseUrl);
        }
    }
}
