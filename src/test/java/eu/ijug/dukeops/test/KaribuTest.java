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
package eu.ijug.dukeops.test;

import com.github.mvysny.fakeservlet.FakeRequest;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import com.github.mvysny.kaributesting.v10.spring.MockSpringServlet;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.VaadinServletRequest;
import eu.ijug.dukeops.SecurityConfig;
import eu.ijug.dukeops.domain.authentication.boundary.LoginView;
import eu.ijug.dukeops.domain.authentication.control.AuthenticationService;
import eu.ijug.dukeops.domain.authentication.entity.UserPrincipal;
import eu.ijug.dukeops.domain.user.entity.UserDto;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.List;
import java.util.Locale;

/**
 * An abstract class which sets up Spring, Karibu-Testing and our app.
 * The easiest way to use this class in our tests is having our test class to extend
 * this class.
 */
public abstract class KaribuTest extends IntegrationTest {

    private static Routes routes;

    @Autowired
    private @NotNull ApplicationContext applicationContext;

    @Autowired
    private @NotNull AuthenticationService authenticationService;

    @BeforeAll
    public static void discoverRoutes() {
        routes = new Routes().autoDiscoverViews("eu.ijug.dukeops");
    }

    @BeforeEach
    public void setupMockVaadin() {
        final Function0<UI> uiFactory = UI::new;
        final var servlet = new MockSpringServlet(routes, applicationContext, uiFactory);
        MockVaadin.setup(uiFactory, servlet);
        UI.getCurrent().setLocale(Locale.ENGLISH);
    }

    @AfterEach
    public void tearDownMockVaadin() {
        MockVaadin.tearDown();
    }

    /**
     * <p>Returns the class of the currently active Vaadin view in the UI
     * (excludes RouterLayouts like WebsiteLayout).</p>
     *
     * <p>This helper is primarily intended for use in Karibu-Testing based unit tests
     * to assert that a navigation or redirect has led to the expected view. It inspects
     * the Vaadin {@link com.vaadin.flow.component.UI#getCurrent() current UI}'s router
     * internals and retrieves the last entry of the active router target chain, which
     * represents the active view instance.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * assertThat(currentViewClass()).isEqualTo(EventsView.class);
     * }</pre>
     *
     * @param <T> the expected view type (used for generic casting in tests)
     * @return the class of the currently active view (excludes RouterLayouts)
     * @throws IllegalStateException if there is no active UI or no active view
     */
    @SuppressWarnings("unchecked")
    public static <T extends Component> Class<? extends T> currentViewClass() {
        final var ui = UI.getCurrent();
        if (ui == null) {
            throw new IllegalStateException("No current UI available");
        }

        final var chain = ui.getInternals().getActiveRouterTargetsChain();
        if (chain.isEmpty()) {
            throw new IllegalStateException("No active view found in router target chain");
        }

        for (int index = chain.size() - 1; index >= 0; index--) {
            final var element = chain.get(index);
            if (!(element instanceof RouterLayout)) {
                return (Class<? extends T>) element.getClass();
            }
        }
        throw new IllegalStateException("No concrete view found (only RouterLayouts present)");
    }

    /**
     * Logs in a user for integration tests by setting the Spring Security context and
     * updating the Vaadin mock request accordingly.
     *
     * @param user the user to log in
     */
    protected void login(final @NotNull UserDto user) {
        final var roleNames = List.of(user.role().getRole());
        final var authorities = roleNames.stream()
                .map(roleName -> (GrantedAuthority) new SimpleGrantedAuthority(roleName))
                .toList();

        // create a Spring Security user (UserDetails)
        final var userPrincipal = new UserPrincipal(user, authorities);

        // create the authentication token
        final var authentication = new PreAuthenticatedAuthenticationToken(userPrincipal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // make ViewAccessChecker work
        final var request = (FakeRequest) VaadinServletRequest.getCurrent().getRequest();
        request.setUserPrincipalInt(authentication);
        request.setUserInRole((_, role) -> roleNames.contains("ROLE_" + role));
    }

    /**
     * <p>Logs out a previously logged-in user in Karibu-based tests.</p>
     *
     * <p>The helper clears the Spring Security context and updates the current Vaadin mock request
     * so that {@code VaadinServletRequest}, {@code ViewAccessChecker} and other security-related
     * checks behave as if the user had logged out.</p>
     *
     * <p>This does not execute the Spring Security {@code /logout} endpoint. If a test needs to
     * verify navigation after logout, it must trigger navigation explicitly.</p>
     *
     * @param ui the current Vaadin UI instance
     */
    protected void logout(final @NotNull UI ui) {
        SecurityContextHolder.clearContext();

        final var vaadinRequest = VaadinServletRequest.getCurrent();
        if (vaadinRequest != null) {
            final var request = (FakeRequest) vaadinRequest.getRequest();
            request.setUserPrincipalInt(null);
            request.setUserInRole((_, _) -> false);
        }

        ui.navigate(SecurityConfig.LOGOUT_SUCCESS_URL);
    }

}
