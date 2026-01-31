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
package eu.ijug.dukeops.web.infra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Configuration for schedulers used in the web layer.
 */
@Configuration
public class SchedulerConfiguration {

    /**
     * Creates a scheduler for handling confirmation redirect countdowns.
     *
     * @return a ScheduledExecutorService for redirect countdowns
     */
    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService redirectScheduler() {
        return Executors.newScheduledThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors()),
            runnable -> {
                final var thread = new Thread(runnable, "confirmation-redirect-countdown");
                thread.setDaemon(true);
                return thread;
            }
        );
    }
}
