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

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.ServerSetupTest;
import eu.ijug.dukeops.domain.user.control.UserService;
import eu.ijug.dukeops.domain.user.entity.UserDto;
import eu.ijug.dukeops.domain.user.entity.UserRole;
import eu.ijug.dukeops.infra.ui.vaadin.control.LinkUtil;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.flywaydb.core.Flyway;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

/**
 * <p>Abstract base class for integration tests that run with a full Spring Boot context.</p>
 *
 * <p>It simplifies writing end-to-end or API-level integration tests by managing the
 * Spring Boot lifecycle and environment configuration.</p>
 *
 * <p>This class configures a random web environment port to avoid conflicts and excludes
 * the task scheduling autoconfiguration to prevent background tasks from interfering
 * with test execution.</p>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration")
@ActiveProfiles("test")
public abstract class IntegrationTest {

    protected static final @NotNull UserDto TEST_ADMIN = new UserDto(UUID.randomUUID(), null,null,"Default Test Admin", "admin@example.com", UserRole.ADMIN);
    protected static final @NotNull UserDto TEST_USER = new UserDto(UUID.randomUUID(), null,null,"Default Test User", "user@example.com", UserRole.USER);

    @Autowired
    private @NotNull UserService userService;

    /**
     * <p>Defines the maximum number of seconds to wait for incoming emails when using GreenMail in integration tests.
     * This timeout is applied to Awaitility-based waits that poll for messages delivered asynchronously by the
     * application under test.</p>
     */
    protected static final int GREENMAIL_WAIT_TIMEOUT = 2;

    /**
     * <p>Injected Flyway instance used to manage the test database schema during integration tests.</p>
     *
     * <p>The instance is configured through the test Spring context. Ensure that Flyway clean is enabled in the
     * test profile and that the database user has sufficient DDL privileges.</p>
     *
     * @see Flyway
     */
    @Autowired
    private Flyway flyway;

    /**
     * <p>Static instance of {@link GreenMailExtension} used to provide an in-memory SMTP server
     * for integration testing.</p>
     *
     * <p>This shared mail server allows tests to send and verify emails without requiring any
     * external SMTP infrastructure. It is configured with a single user account
     * (<code>dukeops</code>/<code>s3cr3t</code>) and persists across test methods to improve
     * performance.</p>
     */
    @RegisterExtension
    private static final @NotNull GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig()
                    .withUser("dukeops", "s3cr3t"))
            .withPerMethodLifecycle(false);

    /**
     * <p>The HTTP port on which the Spring Boot application under test is running.</p>
     *
     * <p>The port is assigned randomly by the Spring Boot test framework to prevent conflicts when multiple test
     * instances run in parallel.</p>
     */
    @LocalServerPort
    private int port;

    /**
     * <p>Returns the shared {@link GreenMailExtension} instance used for email testing.</p>
     *
     * <p>This method gives subclasses access to the in-memory SMTP server so that they can verify
     * sent messages, inspect inboxes, or reset the mail state between tests.</p>
     *
     * @return the global {@link GreenMailExtension} instance used by all integration tests
     */
    protected GreenMailExtension getGreenMail() {
        return greenMail;
    }

    /**
     * <p>Prepares the integration test environment before each test execution.</p>
     *
     * <p>This method ensures a consistent and isolated test setup by purging all emails, and resetting the database
     * schema via Flyway.</p>
     *
     * @throws FolderException if an error occurs while purging emails from the mailboxes
     *
     * @see Flyway
     */
    @BeforeEach
    void prepareIntegrationTest() throws FolderException {
        greenMail.purgeEmailFromAllMailboxes();

        flyway.clean();
        flyway.migrate();
        prepareTestData();

        final var baseUrl = "http://localhost:%d/".formatted(port);
        LinkUtil.setBaseUrl(baseUrl);
    }

    private void prepareTestData() {
        userService.storeUser(TEST_ADMIN);
        userService.storeUser(TEST_USER);
    }

    /**
     * <p>Retrieves the first email received by GreenMail whose subject matches the provided value. The method actively
     * waits until such an email arrives or the configured timeout is reached. This is intended for integration tests
     * where emails may be delivered asynchronously and must be awaited before assertions can be performed.</p>
     *
     * <p>The search is repeated during the waiting period, and the first matching message is returned immediately
     * once detected. If no matching email arrives before the timeout expires, the method fails through Awaitility's
     * timeout mechanism.</p>
     *
     * @param subject the subject line to match against incoming emails
     * @return the first {@link Message} instance whose subject matches the provided value
     */
    protected @NotNull Message getEmailBySubject(final @NotNull String subject) {
        final var found = new AtomicReference<Message>();

        await().atMost(GREENMAIL_WAIT_TIMEOUT, SECONDS).until(() -> {
            final var message = Stream.of(greenMail.getReceivedMessages())
                    .filter(m -> {
                        try {
                            return subject.equals(m.getSubject());
                        } catch (MessagingException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .findFirst()
                    .orElse(null);
            found.set(message);
            return message != null;
        });

        return found.get();
    }

}
