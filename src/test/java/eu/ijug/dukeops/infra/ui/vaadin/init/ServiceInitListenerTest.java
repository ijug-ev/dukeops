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
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceInitListenerTest {

    @Test
    void shouldInvokeAllInitializersOnUiInit() {
        final var initializerOne = mock(UIInitializer.class);
        final var initializerTwo = mock(UIInitializer.class);

        final var listener = new ServiceInitListener(List.of(initializerOne, initializerTwo));

        final var vaadinService = mock(VaadinService.class);
        final var serviceInitEvent = mock(ServiceInitEvent.class);
        when(serviceInitEvent.getSource()).thenReturn(vaadinService);

        final var captor = ArgumentCaptor.forClass(UIInitListener.class);

        listener.serviceInit(serviceInitEvent);
        verify(vaadinService).addUIInitListener(captor.capture());

        UI ui = new UI();
        captor.getValue().uiInit(new UIInitEvent(ui, vaadinService));

        verify(initializerOne).initialize(ui);
        verify(initializerTwo).initialize(ui);
    }

}
