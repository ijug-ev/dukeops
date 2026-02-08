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

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.aura.Aura;
import eu.ijug.dukeops.infra.config.AppConfig;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The entry point of the Spring Boot application.
 */
@Push
@StyleSheet(Aura.STYLESHEET)
@StyleSheet("css/styles.css")
@Viewport("width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes, viewport-fit=cover")
@PWA(name = "DukeOps - iJUG Self-Service Portal", shortName = "DukeOps")
@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(AppConfig.class)
public class Application extends SpringBootServletInitializer implements AppShellConfigurator {

    public static void main(final @NotNull String... args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * <p>Configures global HTML page metadata and resources for the application shell.</p>
     *
     * <p>This method is invoked by Vaadin during application bootstrap and is used to
     * customize the generated HTML document head. It registers metadata such as the
     * author information, multiple favicon variants for different devices and resolutions,
     * and a legacy shortcut icon for broad browser compatibility.</p>
     *
     * <p>The configuration defined here applies to all views of the application and is
     * independent of individual UI components or layouts.</p>
     *
     * @param settings the {@link AppShellSettings} instance used to configure page-level
     *                 metadata, icons, and link elements
     */
    @Override
    public void configurePage(final @NotNull AppShellSettings settings) {
        settings.addMetaTag("author", "iJUG Interessenverbund der Java User Groups e. V.");
        settings.addFavIcon("icon", "icons/icon.png", "1024x1024");
        settings.addFavIcon("icon", "icons/favicon-512x512.png", "512x512");
        settings.addFavIcon("icon", "icons/favicon-192x192.png", "192x192");
        settings.addFavIcon("icon", "icons/favicon-180x180.png", "180x180");
        settings.addFavIcon("icon", "icons/favicon-32x32.png", "32x32");
        settings.addFavIcon("icon", "icons/favicon-16x16.png", "16x16");
        settings.addLink("shortcut icon", "icons/favicon.ico");
    }

}
