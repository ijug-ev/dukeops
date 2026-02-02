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
package eu.ijug.dukeops.infra.ui.vaadin.init;

import com.vaadin.flow.component.UI;
import eu.ijug.dukeops.infra.ui.vaadin.i18n.LocaleUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

/**
 * <p>Initializes {@link LocaleUtil} for a newly created Vaadin {@link UI} by detecting the client locale.</p>
 *
 * <p>The detected locale is used to configure locale-dependent behavior such as translations and formatting.</p>
 */
@Component
public class LocaleUtilInitializer implements UIInitializer {

    /**
     * <p>Detects and applies the client locale for the given Vaadin {@link UI} instance.</p>
     *
     * @param ui the Vaadin UI instance being initialized
     */
    @Override
    public void initialize(final @NotNull UI ui) {
        LocaleUtil.detectClientLocale(ui);
    }

}
