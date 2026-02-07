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
package eu.ijug.dukeops.domain.authentication.boundary;

import com.icegreen.greenmail.util.GreenMailUtil;
import eu.ijug.dukeops.SecurityConfig;
import eu.ijug.dukeops.infra.ui.vaadin.control.LinkUtil;
import eu.ijug.dukeops.test.BrowserTest;
import org.junit.jupiter.api.Test;

import static eu.ijug.dukeops.test.TestUtil.extractLinkFromText;
import static org.assertj.core.api.Assertions.assertThat;

class LoginViewBT extends BrowserTest {

    @Test
    void loginWithButtonPress() {
        final var page = getPage();
        page.navigate(LinkUtil.getBaseUrl() + SecurityConfig.LOGIN_URL);
        page.waitForURL("**" + SecurityConfig.LOGIN_URL);
        page.waitForSelector(PAGE_NAME_SELECTOR);

        // fill in email address
        final var emailInput = page.locator("vaadin-email-field").locator("input");
        emailInput.fill(TEST_USER.email());

        // use the button to submit the form
        page.locator("vaadin-button:has-text('Request Login Link')").click();

        // wait for the confirmation email
        final var confirmationMessage = getEmailBySubject("Please confirm your email address");

        // extract the confirmation link
        final var mailBody = GreenMailUtil.getBody(confirmationMessage);
        final var confirmationLink = extractLinkFromText(mailBody);
        assertThat(confirmationLink).isNotNull();

        // open the confirmation link
        page.navigate(confirmationLink);
        page.waitForURL("**/confirm**");
        page.waitForSelector(CONFIRMATION_SUCCESSFUL_SELECTOR);
    }

    @Test
    void loginWithEnterKey() {
        final var page = getPage();
        page.navigate(LinkUtil.getBaseUrl() + SecurityConfig.LOGIN_URL);
        page.waitForURL("**" + SecurityConfig.LOGIN_URL);
        page.waitForSelector(PAGE_NAME_SELECTOR);

        // fill in email address
        final var emailInput = page.locator("vaadin-email-field").locator("input");
        emailInput.fill(TEST_USER.email());

        // use the enter key to submit the form
        emailInput.press("Enter");

        // wait for the confirmation email
        final var confirmationMessage = getEmailBySubject("Please confirm your email address");

        // extract the confirmation link
        final var mailBody = GreenMailUtil.getBody(confirmationMessage);
        final var confirmationLink = extractLinkFromText(mailBody);
        assertThat(confirmationLink).isNotNull();

        // open the confirmation link
        page.navigate(confirmationLink);
        page.waitForURL("**/confirm**");
        page.waitForSelector(CONFIRMATION_SUCCESSFUL_SELECTOR);
    }

}
