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
package eu.ijug.dukeops.web.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.jetbrains.annotations.NotNull;
import eu.ijug.dukeops.security.SecurityConfig;
import eu.ijug.dukeops.service.ConfirmationService;
import eu.ijug.dukeops.web.layout.WebsiteLayout;

@AnonymousAllowed
@Route(value = SecurityConfig.LOGIN_URL, layout = WebsiteLayout.class)
public final class LoginView extends AbstractView {

    public LoginView(final @NotNull ConfirmationService confirmationService) {
        super();
        setId("login-view");
        add(new H3(getTranslation("web.view.LoginView.login.title")));

        final var emailField = new EmailField();
        emailField.setPlaceholder(getTranslation("web.view.LoginView.login.email.placeholder"));
        emailField.setRequired(true);
        emailField.setValueChangeMode(ValueChangeMode.EAGER);
        emailField.setClearButtonVisible(true);
        add(emailField);

        final var submitButton = new Button(getTranslation("web.view.LoginView.login.button"));
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.addClickListener(_ -> {
            emailField.setEnabled(false);
            submitButton.setEnabled(false);

            final var locale = getLocale();
            final var email = emailField.getValue().trim();
            final var timeout = confirmationService.getConfirmationTimeoutText(locale);

            confirmationService.sendConfirmationMail(locale, email);

            removeAll();
            add(new H3(getTranslation("web.view.LoginView.confirm.title")));
            add(new Markdown(getTranslation("web.view.LoginView.confirm.description", email, timeout)));
        });
        add(submitButton);

        final var binder = new Binder<DummyBean>();
        binder.forField(emailField)
                .asRequired("")
                .withValidator(new EmailValidator(getTranslation("web.view.LoginView.login.email.validationError")))
                .bind(_ -> null, (_, _) -> { });
        binder.setBean(new DummyBean());
        binder.addStatusChangeListener(_ -> submitButton.setEnabled(binder.isValid()));
        binder.validate();

        emailField.focus();
    }

    @Override
    protected @NotNull String getViewTitle() {
        return getTranslation("web.view.LoginView.title");
    }

    @SuppressWarnings("java:S2094") // DummyBean for Binder (to use validation only)
    private static final class DummyBean { }
}
