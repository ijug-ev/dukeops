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
package eu.ijug.dukeops.domain.authentication.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationSignalTest {

    private AuthenticationSignal signal;

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
        signal = new AuthenticationSignal();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uninitialized_isNotAuthenticated_andNotAdmin() {
        assertThat(signal.isAuthenticated()).isFalse();
        assertThat(signal.isAdmin()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "true,  true,  true,  true",
            "true,  false, true,  false",
            "false, true,  false, false",
            "false, false, false, false"
    })
    void setAuthenticated(final boolean inputAuth, final boolean inputAdmin,
                          final boolean expectedAuth, final boolean expectedAdmin) {
        signal.setAuthenticated(inputAuth, inputAdmin);

        assertThat(signal.isAuthenticated()).isEqualTo(expectedAuth);
        assertThat(signal.isAdmin()).isEqualTo(expectedAdmin);
    }

}
