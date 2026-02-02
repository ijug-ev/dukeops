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

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <p>Registers all {@link UIInitializer} implementations to be executed when a new Vaadin
 * {@link com.vaadin.flow.component.UI} is created.</p>
 *
 * <p>This listener hooks into the Vaadin service lifecycle and ensures that UI-related initialization logic is applied
 * consistently for every UI instance.</p>
 */
@Component
public class ServiceInitListener implements VaadinServiceInitListener {


    private final @NotNull List<@NotNull UIInitializer> initializers;

    /**
     * <p>Creates a new service init listener with the given list of UI initializers.</p>
     *
     * <p>The provided list is defensively copied to prevent external modification.</p>
     *
     * @param initializers the UI initializers to be executed for each new UI
     */
    public ServiceInitListener(final @NotNull List<@NotNull UIInitializer> initializers) {
        this.initializers = List.copyOf(initializers);
    }

    /**
     * <p>Registers a UI initialization listener that applies all configured {@link UIInitializer} instances.</p>
     *
     * @param event the Vaadin service initialization event
     */
    @Override
    public void serviceInit(final @NotNull ServiceInitEvent event) {
        event.getSource().addUIInitListener(initEvent -> {
            final var ui = initEvent.getUI();
            initializers.forEach(initializer -> initializer.initialize(ui));
        });
    }

}
