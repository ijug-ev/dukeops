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
package eu.ijug.dukeops.web.init;

import com.vaadin.flow.component.UI;
import org.junit.jupiter.api.Test;
import eu.ijug.dukeops.config.AppConfig;
import eu.ijug.dukeops.util.LinkUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LinkUtilInitializerTest {

    @Test
    void shouldSetBaseUrlWhenConfigured() {
        AppConfig appConfig = mock(AppConfig.class);
        when(appConfig.baseUrl()).thenReturn("https://example.com/");

        new LinkUtilInitializer(appConfig).initialize(new UI());

        assertThat(LinkUtil.getBaseUrl()).isEqualTo("https://example.com/");
    }

}
