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
package eu.ijug.dukeops;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;
import eu.ijug.dukeops.test.BrowserTest;
import eu.ijug.dukeops.util.LinkUtil;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

class HealthCheckBT extends BrowserTest {

    @Test
    void healthEndpointShouldReturn200() throws Exception {
        try (final var client = HttpClient.newHttpClient()) {
            final var uri = UriComponentsBuilder.fromUriString(LinkUtil.getBaseUrl())
                    .path("/actuator/health")
                    .build()
                    .toUri();
            final var request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();

            final var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);

            final var json = new JSONObject(response.body());
            assertThat(json.getString("status")).isEqualToIgnoringCase("up");
        }
    }

}
