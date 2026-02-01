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

import org.junit.jupiter.api.Test;
import eu.ijug.dukeops.test.BrowserTest;
import eu.ijug.dukeops.util.LinkUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class NavigationBarBT extends BrowserTest {

    protected static final String DARK_MODE_BUTTON_SELECTOR = "vaadin-button.theme-toggle-button";
    protected static final String TITLE_COLOR_LIGHT_MODE = "oklch(1 0.002 260)";
    protected static final String TITLE_COLOR_DARK_MODE = "oklch(0.15 0.0038 248)";

    @Test
    @SuppressWarnings("java:S2925") // suppress warning about Thread.sleep, as this is a test for UI interaction
    void toggleDarkMode() throws InterruptedException {
        final var page = getPage();
        page.navigate(LinkUtil.getBaseUrl());
        page.waitForSelector(PAGE_NAME_SELECTOR);
        captureScreenshot("page-before-toggle");

        final var titleColorBeforeToggle = page.evaluate("""
          () => getComputedStyle(document.querySelector('h1'))
              .getPropertyValue('color')
          """).toString();

        page.click(DARK_MODE_BUTTON_SELECTOR);
        Thread.sleep(100);
        captureScreenshot("page-after-toggle");

        final var titleColorAfterToggle = page.evaluate("""
          () => getComputedStyle(document.querySelector('h1'))
              .getPropertyValue('color')
          """).toString();

        assertThat(titleColorBeforeToggle)
                .isIn(TITLE_COLOR_DARK_MODE, TITLE_COLOR_LIGHT_MODE);
        assertThat(titleColorAfterToggle)
                .isIn(TITLE_COLOR_DARK_MODE, TITLE_COLOR_LIGHT_MODE)
                .isNotSameAs(titleColorBeforeToggle);
    }

    @Test
    void checkMenuBarItemsWithoutLogin() {
        final var page = getPage();
        page.navigate(LinkUtil.getBaseUrl());
        page.waitForSelector(PAGE_NAME_SELECTOR);
        captureScreenshot("menubar-without-login");

        assertSoftly(softly -> {
            softly.assertThat(page.locator(".navigation-bar a[href='dashboard']").isVisible()).isFalse();
            softly.assertThat(page.locator(".navigation-bar a[href='login']").isVisible()).isTrue();
            softly.assertThat(page.locator(".navigation-bar a[href='logout']").isVisible()).isFalse();
            softly.assertThat(page.locator(".navigation-bar a[href='https://www.ijug.eu/impressum']").isVisible()).isTrue();
        });
    }

    @Test
    void checkMenuBarItemsWithLogin() {
        login(TEST_USER);

        final var page = getPage();
        page.navigate(LinkUtil.getBaseUrl());
        page.waitForSelector(PAGE_NAME_SELECTOR);
        captureScreenshot("menubar-with-login");

        assertSoftly(softly -> {
            softly.assertThat(page.locator(".navigation-bar a[href='dashboard']").isVisible()).isTrue();
            softly.assertThat(page.locator(".navigation-bar a[href='login']").isVisible()).isFalse();
            softly.assertThat(page.locator(".navigation-bar a[href='logout']").isVisible()).isTrue();
            softly.assertThat(page.locator(".navigation-bar a[href='https://www.ijug.eu/impressum']").isVisible()).isTrue();
        });
    }
}
