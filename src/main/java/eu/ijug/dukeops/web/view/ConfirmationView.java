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

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import eu.ijug.dukeops.service.ConfirmationService;
import eu.ijug.dukeops.web.infra.Navigator;
import eu.ijug.dukeops.web.layout.WebsiteLayout;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@AnonymousAllowed
@Route(value = "confirm", layout = WebsiteLayout.class)
public final class ConfirmationView extends AbstractView implements BeforeEnterObserver, BeforeLeaveObserver {

    private volatile ScheduledFuture<?> redirectCountdownFuture;

    private final @NotNull ConfirmationService confirmationService;
    private final @NotNull ScheduledExecutorService redirectScheduler;
    private final @NotNull Navigator redirectNavigator;
    private final @NotNull Duration redirectTimeout;
    private final @NotNull Duration redirectTick;

    public ConfirmationView(final @NotNull ConfirmationService confirmationService,
                            final @NotNull ScheduledExecutorService redirectScheduler,
                            final @NotNull Navigator redirectNavigator,
                            @Value("${dukeops.confirm.redirect.timeout}") final @NotNull Duration redirectTimeout,
                            @Value("${dukeops.confirm.redirect.tick}") final @NotNull Duration redirectTick) {
        super();

        if (redirectTimeout.isNegative()) {
            throw new IllegalArgumentException("'dukeops.confirm.redirect.timeout' must be >= 0");
        }

        if (redirectTick.isZero() || redirectTick.isNegative()) {
            throw new IllegalArgumentException("'dukeops.confirm.redirect.tick' must be > 0");
        }

        this.confirmationService = confirmationService;
        this.redirectScheduler = redirectScheduler;
        this.redirectNavigator = redirectNavigator;
        this.redirectTimeout = redirectTimeout;
        this.redirectTick = redirectTick;
        setId("confirmation-view");
        addDetachListener(_ -> cancelCountdown());
    }

    @Override
    protected @NotNull String getViewTitle() {
        return getTranslation("web.view.ConfirmationView.title");
    }

    @Override
    public void beforeEnter(final @NotNull BeforeEnterEvent beforeEnterEvent) {
        removeAll();

        final @NotNull var confirmationId = beforeEnterEvent
                .getLocation()
                .getQueryParameters()
                .getSingleParameter("id")
                .orElse("");

        if (confirmationService.confirmAndLogin(confirmationId)) {
            final var ui = beforeEnterEvent.getUI();

            if (redirectTimeout.isZero()) {
                redirectNavigator.navigate(ui, DashboardView.class);
                return;
            }

            add(new H3(getTranslation("web.view.ConfirmationView.success.title")));

            final var countdownMessage = new Markdown();
            add(countdownMessage);

            final var remainingSeconds = new AtomicLong(redirectTimeout.toSeconds());
            updateMessage(countdownMessage, remainingSeconds.get());

            redirectCountdownFuture = redirectScheduler.scheduleAtFixedRate(() -> ui.access(() -> {
                final var secondsLeft = remainingSeconds.decrementAndGet();
                updateMessage(countdownMessage, secondsLeft);
                if (secondsLeft <= 0) {
                    cancelCountdown();
                    redirectNavigator.navigate(ui, DashboardView.class);
                }
            }), redirectTick.toMillis(), redirectTick.toMillis(), TimeUnit.MILLISECONDS);
        } else {
            add(new H3(getTranslation("web.view.ConfirmationView.error.title")));
            add(new Markdown(getTranslation("web.view.ConfirmationView.error.message")));
        }
    }

    @Override
    public void beforeLeave(final @NotNull BeforeLeaveEvent event) {
        cancelCountdown();
    }

    private void updateMessage(final @NotNull Markdown message, final long secondsLeft) {
        final var params = Map.of("timeout", secondsLeft);
        message.setContent(getTranslation("web.view.ConfirmationView.success.message", params));
    }

    private void cancelCountdown() {
        if (redirectCountdownFuture != null) {
            redirectCountdownFuture.cancel(false);
            redirectCountdownFuture = null;
        }
    }
}
