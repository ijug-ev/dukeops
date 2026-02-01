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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AbstractViewTest {

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    @Test
    void pageTitle() {
        final var view = new TestView();
        assertThat(view.getPageTitle()).isEqualTo("Test View – DukeOps");
    }

    @Test
    void updatePageTitle_withUI() {
        final var page = mock(Page.class);
        final var ui = new TestUI(page);
        UI.setCurrent(ui);

        final var view = new TestView();
        ui.add(view);
        view.updatePageTitle();
        verify(page).setTitle("Test View – DukeOps");
    }

    @Test
    void updatePageTitle_withoutUI() {
        UI.setCurrent(null); // no UI available
        final var view = new TestView();
        assertThatCode(view::updatePageTitle).doesNotThrowAnyException();
    }

    private static class TestView extends AbstractView {
        protected TestView() {
            super();
        }

        @Override
        protected @NotNull String getViewTitle() {
            return "Test View";
        }
    }

    private static final class TestUI extends UI {

        private final Page page;

        TestUI(final @NotNull Page page) {
            this.page = page;
        }

        @Override
        public @NotNull Page getPage() {
            return page;
        }
    }

}
