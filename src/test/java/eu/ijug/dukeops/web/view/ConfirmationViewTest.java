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
package eu.ijug.dukeops.web.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.server.Command;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import eu.ijug.dukeops.service.ConfirmationService;
import eu.ijug.dukeops.web.infra.Navigator;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmationViewTest {

    protected static final String TEST_CONFIRMATION_ID = UUID.randomUUID().toString();

    @Test
    void negativeRedirectTimeoutShouldThrowIllegalArgumentException() {
        try (final var scheduler = mock(ScheduledExecutorService.class)) {
            final var confirmationService = mock(ConfirmationService.class);
            final var navigator = mock(Navigator.class);

            assertThatThrownBy(() -> new ConfirmationView(
                    confirmationService,
                    scheduler,
                    navigator,
                    Duration.ofSeconds(-1),
                    Duration.ofMillis(10)
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("'dukeops.confirm.redirect.timeout' must be >= 0");
        }
    }

    @Test
    void negativeRedirectTickShouldThrowIllegalArgumentException() {
        try (final var scheduler = mock(ScheduledExecutorService.class)) {
            final var confirmationService = mock(ConfirmationService.class);
            final var navigator = mock(Navigator.class);

            assertThatThrownBy(() -> new ConfirmationView(
                    confirmationService,
                    scheduler,
                    navigator,
                    Duration.ofSeconds(2),
                    Duration.ofMillis(-1)
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("'dukeops.confirm.redirect.tick' must be > 0");
        }
    }

    @Test
    void zeroRedirectTickShouldThrowIllegalArgumentException() {
        try (final var scheduler = mock(ScheduledExecutorService.class)) {
            final var confirmationService = mock(ConfirmationService.class);
            final var navigator = mock(Navigator.class);

            assertThatThrownBy(() -> new ConfirmationView(
                    confirmationService,
                    scheduler,
                    navigator,
                    Duration.ofSeconds(2),
                    Duration.ZERO
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("'dukeops.confirm.redirect.tick' must be > 0");
        }
    }

    @Test
    void successWithZeroTimeoutShouldNavigateImmediatelyAndNotSchedule() {
        final var confirmationService = mock(ConfirmationService.class);
        when(confirmationService.confirmAndLogin(TEST_CONFIRMATION_ID)).thenReturn(true);

        final var scheduler = mock(ScheduledExecutorService.class);
        final var navigator = mock(Navigator.class);

        final var view = new ConfirmationView(
                confirmationService,
                scheduler,
                navigator,
                Duration.ZERO,
                Duration.ofMillis(10)
        );

        final var ui = mock(UI.class);

        final var event = mockBeforeEnterEvent(ui);

        view.beforeEnter(event);

        verify(navigator).navigate(ui, DashboardView.class);
        verifyNoInteractions(scheduler);
    }

    @Test
    void successShouldScheduleCountdownAndNavigateAfterTimeout() {
        final var confirmationService = mock(ConfirmationService.class);
        when(confirmationService.confirmAndLogin(TEST_CONFIRMATION_ID)).thenReturn(true);

        final var scheduler = mock(ScheduledExecutorService.class);
        final var navigator = mock(Navigator.class);

        final ScheduledFuture<?> future = mock(ScheduledFuture.class);

        final var taskCaptor = ArgumentCaptor.forClass(Runnable.class);

        doReturn(future).when(scheduler)
                .scheduleAtFixedRate(taskCaptor.capture(), anyLong(), anyLong(), any(TimeUnit.class));

        final var view = new ConfirmationView(
                confirmationService,
                scheduler,
                navigator,
                Duration.ofSeconds(2),      // => 2 ticks to navigation
                Duration.ofMillis(10)
        );

        final var ui = mock(UI.class);
        doAnswer(inv -> {
            ((Command) inv.getArgument(0)).execute();
            return null;
        }).when(ui).access(any(Command.class));

        final var event = mockBeforeEnterEvent(ui);

        view.beforeEnter(event);

        // Scheduler was registered correctly
        verify(scheduler).scheduleAtFixedRate(any(Runnable.class), eq(10L), eq(10L), eq(TimeUnit.MILLISECONDS));

        // Tick 1: 2 -> 1 (no navigation)
        taskCaptor.getValue().run();
        verifyNoInteractions(navigator);

        // Tick 2: 1 -> 0 (navigation + cancel)
        taskCaptor.getValue().run();
        verify(navigator).navigate(ui, DashboardView.class);

        // cancelCountdown() should call future.cancel(false)
        verify(future).cancel(false);
    }

    @Test
    void failureShouldShowErrorAndNotSchedule() {
        final var confirmationService = mock(ConfirmationService.class);
        when(confirmationService.confirmAndLogin(anyString())).thenReturn(false);

        final var scheduler = mock(ScheduledExecutorService.class);
        final var navigator = mock(Navigator.class);

        final var view = new ConfirmationView(
                confirmationService,
                scheduler,
                navigator,
                Duration.ofSeconds(2),
                Duration.ofMillis(10)
        );

        final var ui = mock(UI.class);
        final var event = mockBeforeEnterEvent(ui);

        view.beforeEnter(event);

        verifyNoInteractions(scheduler);
        verifyNoInteractions(navigator);

        // TODO: check error message visible
        System.out.println(view.getChildren());
        assertThat(view.getChildren().count()).isGreaterThan(0);
    }

    @Test
    void beforeLeaveShouldCancelScheduledCountdown() {
        final var confirmationService = mock(ConfirmationService.class);
        when(confirmationService.confirmAndLogin(TEST_CONFIRMATION_ID)).thenReturn(true);

        final var scheduler = mock(ScheduledExecutorService.class);
        final var navigator = mock(Navigator.class);

        final ScheduledFuture<?> future = mock(ScheduledFuture.class);

        doReturn(future).when(scheduler)
                .scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));

        final var view = new ConfirmationView(
                confirmationService,
                scheduler,
                navigator,
                Duration.ofSeconds(2),
                Duration.ofMillis(10)
        );

        final var ui = mock(UI.class);

        // first simulate entering the view
        final var enterEvent = mockBeforeEnterEvent(ui);
        view.beforeEnter(enterEvent);

        // now simulate leaving the view
        final var leaveEvent = mock(BeforeLeaveEvent.class);
        view.beforeLeave(leaveEvent);
        verify(future).cancel(false);

        // calling beforeLeave again should not cause any issues
        view.beforeLeave(leaveEvent);
        verify(future).cancel(false);
    }

    private static BeforeEnterEvent mockBeforeEnterEvent(UI ui) {
        final var event = mock(BeforeEnterEvent.class);
        lenient().when(event.getUI()).thenReturn(ui);

        final var location = mock(Location.class);
        final var queryParams = mock(QueryParameters.class);

        when(event.getLocation()).thenReturn(location);
        when(location.getQueryParameters()).thenReturn(queryParams);
        when(queryParams.getSingleParameter("id")).thenReturn(Optional.ofNullable(TEST_CONFIRMATION_ID));

        return event;
    }
}
