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

@Component
public final class ServiceInitListener implements VaadinServiceInitListener {


    private final @NotNull List<@NotNull UIInitializer> initializers;

    public ServiceInitListener(final @NotNull List<@NotNull UIInitializer> initializers) {
        this.initializers = List.copyOf(initializers);
    }

    @Override
    public void serviceInit(final @NotNull ServiceInitEvent event) {
        event.getSource().addUIInitListener(initEvent -> {
            final var ui = initEvent.getUI();
            initializers.forEach(initializer -> initializer.initialize(ui));
        });
    }

}
