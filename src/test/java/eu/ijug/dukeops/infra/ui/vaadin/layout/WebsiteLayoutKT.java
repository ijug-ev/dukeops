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
package eu.ijug.dukeops.infra.ui.vaadin.layout;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.RouterLink;
import eu.ijug.dukeops.domain.dashboard.boundary.DashboardView;
import eu.ijug.dukeops.test.KaribuTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static eu.ijug.dukeops.test.TestUtil.assertContainsExactlyOneAnchorLinkOf;
import static eu.ijug.dukeops.test.TestUtil.assertContainsExactlyOneInstanceOf;
import static eu.ijug.dukeops.test.TestUtil.assertContainsExactlyOneRouterLinkOf;
import static eu.ijug.dukeops.test.TestUtil.findComponent;
import static eu.ijug.dukeops.test.TestUtil.findComponents;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebsiteLayoutKT extends KaribuTest {

    private @NotNull WebsiteLayout websiteLayout;

    @BeforeEach
    void setUp() {
        UI.getCurrent().navigate(DashboardView.class);
        final var uiParent = UI.getCurrent()
                .getCurrentView()
                .getParent().orElseThrow()
                .getParent().orElseThrow();
        websiteLayout = (WebsiteLayout) uiParent;
    }

    @Test
    void testLayoutContent() {
        final var components = websiteLayout.getChildren().toList();
        assertContainsExactlyOneInstanceOf(components,
                PageHeader.class, NavigationBar.class, Main.class, PageFooter.class);
    }

    @Test
    void testPageHeader() {
        final var header = findComponent(websiteLayout, Header.class);
        assertThat(header).isNotNull();

        final var h1 = findComponent(header, H1.class);
        assertThat(h1).isNotNull();
        assertThat(h1.getText()).isEqualTo("DukeOps");

        final var h2 = findComponent(header, H2.class);
        assertThat(h2).isNotNull();
        assertThat(h2.getText()).isEqualTo("iJUG Self-Service Portal");
    }

    @Test
    void testPageFooter() {
        final var footer = findComponent(websiteLayout, Footer.class);
        assertThat(footer).isNotNull();

        final var anchor = findComponent(footer, Anchor.class);
        assertThat(anchor).isNotNull();
        assertThat(anchor.getText()).contains("DukeOps · Version");
    }

    @Test
    void testNavigationBar() {
        final var navigationBar = findComponent(websiteLayout, NavigationBar.class);
        assertThat(navigationBar).isNotNull();

        final var routerLinks = findComponents(navigationBar, RouterLink.class);
        assertContainsExactlyOneRouterLinkOf(routerLinks,
                new Anchor("", "Dashboard"),
                new Anchor("login", "Login")
        );
        final var anchorLinks = findComponents(navigationBar, Anchor.class);
        assertContainsExactlyOneAnchorLinkOf(anchorLinks,
                new Anchor("/logout", "Logout"),
                new Anchor("https://www.ijug.eu/impressum", "Imprint")
        );
    }

    @Test
    void testRouterLayoutContent() {
        final var main = findComponent(websiteLayout, Main.class);
        assertThat(main).isNotNull();
        assertThat(main.getElement().getChildCount()).isEqualTo(1);

        websiteLayout.showRouterLayoutContent(new Paragraph("foo"));
        assertThat(main.getElement().getChildCount()).isEqualTo(1);
        assertThat(main.getElement().getChild(0).getText()).isEqualTo("foo");

        websiteLayout.showRouterLayoutContent(new Paragraph("bar"));
        assertThat(main.getElement().getChildCount()).isEqualTo(1);
        assertThat(main.getElement().getChild(0).getText()).isEqualTo("bar");

        websiteLayout.removeRouterLayoutContent(null);
        assertThat(main.getElement().getChildCount()).isZero();
    }

    @Test
    void testRouterLayoutContentException() {
        final var content = mock(HasElement.class);
        final var element = mock(Element.class);
        when(content.getElement()).thenReturn(element);
        when(element.getComponent()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> websiteLayout.showRouterLayoutContent(content))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("WebsiteLayout content must be a Component");
    }

}
