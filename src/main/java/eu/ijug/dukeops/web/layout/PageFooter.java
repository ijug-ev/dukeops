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

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Footer;
import org.jetbrains.annotations.NotNull;

import java.time.Year;

public final class PageFooter extends Footer {

    public PageFooter(final @NotNull String version) {
        super();
        addClassName("page-footer");

        final var dukeOpsFooter = getTranslation("web.layout.PageFooter.dukeops",
                version, String.valueOf(Year.now().getValue()));
        add(new Anchor("https://github.com/ijug-ev/dukeops", dukeOpsFooter, AnchorTarget.BLANK));
    }

}
