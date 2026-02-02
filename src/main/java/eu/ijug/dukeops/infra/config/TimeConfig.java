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
package eu.ijug.dukeops.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * <p>Spring configuration that provides a {@link Clock} bean for the application.</p>
 *
 * <p>The clock is used as a central time source and can be replaced or mocked in tests to ensure
 * deterministic and reproducible time-based behavior.</p>
 */
@Configuration
public class TimeConfig {

    /**
     * <p>Creates the default {@link Clock} instance for the application context.</p>
     *
     * <p>The returned clock uses the system default time zone and should be injected wherever
     * the current time is required.</p>
     *
     * @return the system default {@link Clock}
     */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

}
