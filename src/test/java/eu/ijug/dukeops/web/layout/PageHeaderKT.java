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
package eu.ijug.dukeops.web.layout;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Image;
import eu.ijug.dukeops.test.KaribuTest;
import org.junit.jupiter.api.Test;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

public class PageHeaderKT extends KaribuTest {

    @Test
    void logoClickNavigatesToDashboard() {
        login(TEST_USER);
        final var pageHeader = _get(PageHeader.class);
        final var logo = _get(pageHeader, Image.class);

        // TODO: navigate to a different view first to make sure navigation happens

        _click(logo);
        MockVaadin.clientRoundtrip();

        final var ui = UI.getCurrent();
        assertThat(ui.getInternals().getActiveViewLocation().getPath()).isEqualTo("dashboard");
    }

}
