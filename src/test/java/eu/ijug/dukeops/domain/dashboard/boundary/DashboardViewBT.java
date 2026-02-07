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
package eu.ijug.dukeops.domain.dashboard.boundary;

import eu.ijug.dukeops.infra.ui.vaadin.control.LinkUtil;
import eu.ijug.dukeops.test.BrowserTest;
import org.junit.jupiter.api.Test;

class DashboardViewBT extends BrowserTest {

    @Test
    void testDashboardNavigation() {
        login(TEST_USER);

        final var page = getPage();
        page.navigate(LinkUtil.getBaseUrl());
        page.waitForSelector(PAGE_NAME_SELECTOR);
        captureScreenshot("before-navigation");

        page.locator("vaadin-card.clickable").first().click();
        page.waitForURL("**/clubdesk/edit");
        captureScreenshot("after-navigation");
    }

}
