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

import com.icegreen.greenmail.util.GreenMailUtil;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.ReducedMotion;
import com.microsoft.playwright.options.ScreenshotType;
import eu.ijug.dukeops.entity.UserDto;
import eu.ijug.dukeops.SecurityConfig;
import eu.ijug.dukeops.util.LinkUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static eu.ijug.dukeops.test.TestUtil.extractLinkFromText;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class BrowserTest extends IntegrationTest {

    protected static final @NotNull String PAGE_NAME_SELECTOR = "h1:has-text('DukeOps')";

    private static final @NotNull String LOGIN_SELECTOR = "a[href='login']";
    private static final @NotNull String LOGOUT_SELECTOR = "a[href='logout']";
    private static final @NotNull String CONFIRMATION_SUCCESSFUL_SELECTOR = "h3:has-text('Confirmation Successful')";

    private static final @NotNull Path SCREENSHOT_DIR = Path.of("target/playwright-screenshots");
    private static final @NotNull DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS");
    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(BrowserTest.class);

    private Playwright playwright;
    private Browser browser;
    private BrowserContext browserContext;
    private Page page;

    private Path screenshotDir;
    private Page.ScreenshotOptions screenshotOptions;

    @BeforeEach
    void startNewBrowser() {
        playwright = Playwright.create();

        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        Browser.NewContextOptions ctxOptions = new Browser.NewContextOptions()
                .setReducedMotion(ReducedMotion.REDUCE)
                .setViewportSize(1920, 1080);
        browserContext = browser.newContext(ctxOptions);
        disableAnimationsGlobally();
        browserContext.clearCookies();
        page = browserContext.newPage();
        assert browserContext.cookies(LinkUtil.getBaseUrl()).isEmpty() : "Context unexpectedly has cookies";

        screenshotDir = SCREENSHOT_DIR.resolve(getClass().getName()).resolve(browser.browserType().name());
        screenshotOptions = new Page.ScreenshotOptions()
                .setType(ScreenshotType.PNG)
                .setFullPage(true);
    }

    protected void disableAnimationsGlobally() {
        browserContext.addInitScript("""
            (() => {
                if (document.querySelector('[data-test-disable-animations]')) {
                    return;
                }
                const css = `
                    *,:before,:after {
                        transition: none !important;
                        animation: none !important;
                        scroll-behavior: auto !important;
                    }
                    html {
                        scroll-behavior: auto !important;
                    }
                    ::view-transition {
                        display: none !important;
                    }
                `;
                const style = document.createElement('style');
                style.setAttribute('data-test-disable-animations','true');
                style.textContent = css;
                document.documentElement.appendChild(style);
            })();
        """);
    }

    @AfterEach
    void stopBrowser() {
        if (page != null) {
            page.close();
        }
        if (browserContext != null) {
            browserContext.close();
        }
        browser.close();
        playwright.close();
    }

    /**
     * <p>Returns the current Playwright {@link Page} instance used in the test.</p>
     *
     * <p>This page is created before each test and closed afterward.
     * It can be used to interact with the browser, navigate to URLs,
     * or assert the presence of elements on the page.</p>
     *
     * @return the current {@link Page} instance
     */
    protected Page getPage() {
        return page;
    }

    /**
     * <p>Captures a screenshot with a given name and prefixed timestamp.</p>
     *
     * @param baseName the base file name (e.g., "home", "login-error")
     */
    protected void captureScreenshot(final @NotNull String baseName) {
        try {
            if (!Files.exists(screenshotDir)) {
                Files.createDirectories(screenshotDir);
            }
            final var fileName = TIMESTAMP_FORMAT.format(LocalDateTime.now()) + "_" + baseName + ".png";
            final var path = screenshotDir.resolve(fileName);
            page.screenshot(screenshotOptions.setPath(path));
            LOGGER.info("Screenshot captured and saved to: {}", path.toAbsolutePath());
        } catch (final IOException e) {
            throw new RuntimeException("Failed to capture screenshot", e);
        }
    }

    protected void login(final @NotNull UserDto user) {
        page.navigate(LinkUtil.getBaseUrl() + SecurityConfig.LOGIN_URL);
        page.waitForURL("**" + SecurityConfig.LOGIN_URL);
        page.waitForSelector(PAGE_NAME_SELECTOR);

        LOGGER.info("Logging in user with email: {}", user.email());

        // fill in email address
        final var emailInput = page.locator("vaadin-email-field").locator("input");
        emailInput.fill(user.email());

        // click on the request email button
        page.locator("vaadin-button:has-text('Request Login Link')").click();

        // wait for the confirmation email
        final var confirmationMessage = getEmailBySubject("Please confirm your email address");

        // extract the confirmation link
        final var mailBody = GreenMailUtil.getBody(confirmationMessage);
        final var confirmationLink = extractLinkFromText(mailBody);
        assertThat(confirmationLink).isNotNull();
        LOGGER.info("Extracted confirmation link: {}", confirmationLink);

        // open the confirmation link
        page.navigate(confirmationLink);
        page.waitForURL("**/confirm**");
        page.waitForSelector(CONFIRMATION_SUCCESSFUL_SELECTOR);
        LOGGER.info("Confirmation successful for user with email: {}", user.email());
    }

    protected void logout() {
        if (page.locator(LOGOUT_SELECTOR).count() == 0) {
            page.click(LOGOUT_SELECTOR);
            page.waitForURL("**" + SecurityConfig.LOGIN_URL);
        }
    }
}
