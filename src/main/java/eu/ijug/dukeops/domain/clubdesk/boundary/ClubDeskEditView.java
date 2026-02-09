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
package eu.ijug.dukeops.domain.clubdesk.boundary;

import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import eu.ijug.dukeops.domain.clubdesk.control.ClubDeskService;
import eu.ijug.dukeops.domain.clubdesk.entity.ClubDeskDto;
import eu.ijug.dukeops.domain.clubdesk.entity.Country;
import eu.ijug.dukeops.domain.clubdesk.entity.NewsletterStatus;
import eu.ijug.dukeops.domain.dashboard.boundary.DashboardView;
import eu.ijug.dukeops.infra.ui.vaadin.control.Navigator;
import eu.ijug.dukeops.infra.ui.vaadin.layout.AbstractView;
import eu.ijug.dukeops.infra.ui.vaadin.layout.WebsiteLayout;
import jakarta.annotation.security.RolesAllowed;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.stream.Stream;

import static eu.ijug.dukeops.domain.clubdesk.control.ClubDeskService.generateSepaMandateReference;

@RolesAllowed("USER")
@Route(value = "clubdesk/edit", layout = WebsiteLayout.class)
public final class ClubDeskEditView extends AbstractView {

    private final @NotNull ClubDeskService clubDeskService;

    private final @NotNull TextField firstname = new TextField();
    private final @NotNull TextField lastname = new TextField();

    private final @NotNull TextField address = new TextField();
    private final @NotNull TextField addressAddition = new TextField();
    private final @NotNull TextField zipCode = new TextField();
    private final @NotNull TextField city = new TextField();
    private final @NotNull Select<Country> country = new Select<>();

    private final @NotNull EmailField email = new EmailField();
    private final @NotNull EmailField emailAlternative = new EmailField();
    private final @NotNull TextField matrix = new TextField();
    private final @NotNull TextField mastodon = new TextField();
    private final @NotNull TextField linkedin = new TextField();

    private final @NotNull Checkbox sepaEnabled = new Checkbox();
    private final @NotNull Paragraph sepaAllowInfo = new Paragraph();
    private final @NotNull Paragraph sepaCancelInfo = new Paragraph();
    private final @NotNull TextField sepaAccountHolder = new TextField();
    private final @NotNull TextField sepaMandateReference = new TextField();
    private final @NotNull TextField sepaIban = new TextField();
    private final @NotNull TextField sepaBic = new TextField();

    private final @NotNull Select<String> javaUserGroup = new Select<>();
    private final @NotNull Select<NewsletterStatus> newsletter = new Select<>();

    private final @NotNull Binder<ClubDeskDto> binder = new Binder<>(ClubDeskDto.class);
    private ClubDeskDto clubDeskOriginal;

    public ClubDeskEditView(final @NotNull ClubDeskService clubDeskService,
                            final @NotNull Navigator navigator) {
        super();
        this.clubDeskService = clubDeskService;

        addClassName("clubdesk-edit-view");
        add(new H3(getViewTitle()));

        final var clubDeskData = clubDeskService.getClubDeskForCurrentUser();
        if (clubDeskData.isEmpty()) {
            final var dialog = new ConfirmDialog();
            dialog.setHeader(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.errorDialog.title"));
            dialog.setText(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.errorDialog.textNoClubDeskData"));

            dialog.setCloseOnEsc(false);
            dialog.setCancelable(false);

            dialog.setConfirmText(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.errorDialog.button"));
            dialog.addConfirmListener(_ -> {
                dialog.close();
                navigator.navigate(getUI().orElseThrow(), DashboardView.class);
            });
            dialog.open();
        } else {
            clubDeskOriginal = clubDeskData.orElseThrow();

            Stream.of(firstname, lastname, address, addressAddition, zipCode, city, country,
                            email, emailAlternative, matrix, mastodon, linkedin,
                            sepaEnabled, sepaAccountHolder, sepaMandateReference, sepaIban, sepaBic,
                            javaUserGroup, newsletter)
                    .forEach(HasSize::setWidthFull);

            final var formLayout = createFormLayout();
            final var saveButton = new Button();
            saveButton.setText(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.saveButton"));
            saveButton.addClickListener(_ -> {
                final var clubDeskUpdated = clubDeskService.save(buildDtoFromFields());
                saveButton.setEnabled(false);
                clubDeskService.notifyOffice(clubDeskOriginal, clubDeskUpdated, getLocale());
                clubDeskOriginal = clubDeskUpdated;
                readBean();

                final var dialog = new ConfirmDialog();
                dialog.setHeader(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.successDialog.title"));
                dialog.setText(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.successDialog.text"));

                dialog.setCancelable(false);
                dialog.setCloseOnEsc(false);
                dialog.setConfirmText(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.successDialog.button"));

                dialog.addConfirmListener(_ -> dialog.close());
                dialog.open();
            });
            saveButton.setEnabled(false);
            add(formLayout, saveButton);

            readBean();

            Runnable updateSaveState = () ->
                    saveButton.setEnabled(hasChanges() && binder.validate().isOk());

            Stream.of(firstname, lastname, address, addressAddition, zipCode, city,
                            email, emailAlternative, matrix, mastodon, linkedin,
                            sepaAccountHolder, sepaMandateReference, sepaIban, sepaBic)
                    .forEach(field -> {
                        field.setValueChangeMode(ValueChangeMode.EAGER);
                        field.addValueChangeListener(_ -> updateSaveState.run());
                    });

            Stream.of(country, javaUserGroup, newsletter)
                    .forEach(field -> field.addValueChangeListener(_ -> updateSaveState.run()));

            sepaEnabled.addValueChangeListener(_ -> {
                checkSepaEnabled();
                binder.validate();
                updateSaveState.run();
            });
        }
    }

    private void readBean() {
        binder.readBean(clubDeskOriginal);
        sepaEnabled.setValue(clubDeskOriginal.sepaEnabled());
        newsletter.setValue(clubDeskOriginal.newsletter() ? NewsletterStatus.ON : NewsletterStatus.OFF);
        checkSepaEnabled();
    }

    private FormLayout createFormLayout() {
        final var formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("30em", 2),
                new FormLayout.ResponsiveStep("40em", 6)
        );

        addNames(formLayout);
        addAddress(formLayout);
        addCommunication(formLayout);
        addSepaMandate(formLayout);
        addNewsletter(formLayout);
        addJavaUserGroup(formLayout);

        return formLayout;
    }

    private void addNames(final @NotNull FormLayout formLayout) {
        firstname.setLabel(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.label.firstname"));
        firstname.setRequiredIndicatorVisible(true);
        binder.forField(firstname)
                .asRequired(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.error.firstname"))
                .bind(ClubDeskDto::firstname, null);

        lastname.setLabel(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.label.lastname"));
        lastname.setRequiredIndicatorVisible(true);
        binder.forField(lastname)
                .asRequired(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.error.lastname"))
                .bind(ClubDeskDto::lastname, null);

        formLayout.add(firstname, lastname);
        formLayout.setColspan(firstname, 3);
        formLayout.setColspan(lastname, 3);
    }

    private void addAddress(final @NotNull FormLayout formLayout) {
        address.setLabel(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.label.address"));
        address.setRequiredIndicatorVisible(true);
        binder.forField(address)
                .asRequired(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.error.address"))
                .bind(ClubDeskDto::address, null);

        addressAddition.setLabel(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.label.addressAddition"));
        binder.forField(addressAddition)
                .bind(ClubDeskDto::addressAddition, null);

        zipCode.setLabel(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.label.zipCode"));
        zipCode.setRequiredIndicatorVisible(true);
        binder.forField(zipCode)
                .asRequired(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.error.zipCode"))
                .bind(ClubDeskDto::zipCode, null);

        city.setLabel(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.label.city"));
        city.setRequiredIndicatorVisible(true);
        binder.forField(city)
                .asRequired(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.error.city"))
                .bind(ClubDeskDto::city, null);

        final var locale = getLocale();
        country.setLabel(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.label.country"));
        country.setItems(Country.all(locale));
        country.setItemLabelGenerator(item -> item != null ? item.displayName(locale) : "");
        country.setEmptySelectionAllowed(true);
        country.setEmptySelectionCaption(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.select.emptyCaption"));
        country.setRequiredIndicatorVisible(true);
        binder.forField(country)
                .asRequired(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.error.country"))
                .bind(ClubDeskDto::country, null);

        formLayout.add(address, addressAddition, zipCode, city, country);
        formLayout.setColspan(address, 3);
        formLayout.setColspan(addressAddition, 3);
        formLayout.setColspan(zipCode, 1);
        formLayout.setColspan(city, 3);
        formLayout.setColspan(country, 2);
    }

    private void addCommunication(final @NotNull FormLayout formLayout) {
        email.setLabel(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.label.email"));
        email.setRequiredIndicatorVisible(true);
        email.setErrorMessage(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.error.email"));
        binder.forField(email)
                .withValidator(new EmailValidator(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.error.email"), false))
                .asRequired(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.error.email"))
                .bind(ClubDeskDto::email, null);

        emailAlternative.setLabel(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.label.emailAlternative"));
        emailAlternative.setErrorMessage(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.error.emailAlternative"));
        binder.forField(emailAlternative)
                .withValidator(new EmailValidator(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.error.emailAlternative"), true))
                .bind(ClubDeskDto::emailAlternative, null);

        matrix.setLabel(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.label.matrix"));
        matrix.setPlaceholder("@username:example.com");
        binder.forField(matrix)
                .bind(ClubDeskDto::matrix, null);

        mastodon.setLabel(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.label.mastodon"));
        mastodon.setPlaceholder("@username@example.com");
        binder.forField(mastodon)
                .bind(ClubDeskDto::mastodon, null);

        linkedin.setLabel(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.label.linkedin"));
        linkedin.setPlaceholder("https://www.linkedin.com/in/username/");
        binder.forField(linkedin)
                .bind(ClubDeskDto::linkedin, null);

        formLayout.add(email, emailAlternative, matrix, mastodon, linkedin);
        formLayout.setColspan(email, 3);
        formLayout.setColspan(emailAlternative, 3);
        formLayout.setColspan(matrix, 3);
        formLayout.setColspan(mastodon, 3);
        formLayout.setColspan(linkedin, 6);
    }

    private void addSepaMandate(final @NotNull FormLayout formLayout) {
        sepaEnabled.setLabel(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.label.sepaEnabled"));

        sepaAccountHolder.setLabel(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.label.sepaAccountHolder"));
        binder.forField(sepaAccountHolder)
                .withValidator(value -> !sepaEnabled.getValue() || !value.isBlank(),
                        getTranslation("domain.clubdesk.boundary.ClubDeskEditView.error.sepaAccountHolder"))
                .bind(ClubDeskDto::sepaAccountHolder, null);

        sepaMandateReference.setLabel(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.label.sepaMandateReference"));
        sepaMandateReference.setReadOnly(true);
        binder.forField(sepaMandateReference)
                .bind(ClubDeskDto::sepaMandateReference, null);

        sepaIban.setLabel(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.label.sepaIban"));
        binder.forField(sepaIban)
                .withValidator(value -> !sepaEnabled.getValue() || !value.isBlank(),
                        getTranslation("domain.clubdesk.boundary.ClubDeskEditView.error.sepaIban"))
                .bind(ClubDeskDto::sepaIban, null);

        sepaBic.setLabel(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.label.sepaBic"));
        binder.forField(sepaBic)
                .withValidator(value -> !sepaEnabled.getValue() || !value.isBlank(),
                        getTranslation("domain.clubdesk.boundary.ClubDeskEditView.error.sepaBic"))
                .bind(ClubDeskDto::sepaBic, null);

        sepaAllowInfo.setText(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.info.sepa.allow"));
        sepaAllowInfo.addClassName("sepa-info");
        sepaCancelInfo.setText(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.info.sepa.cancel"));
        sepaCancelInfo.addClassName("sepa-info");

        formLayout.add(sepaEnabled, sepaAllowInfo, sepaCancelInfo, sepaAccountHolder, sepaMandateReference, sepaIban, sepaBic);
        formLayout.setColspan(sepaEnabled, 6);
        formLayout.setColspan(sepaAllowInfo, 6);
        formLayout.setColspan(sepaCancelInfo, 6);
        formLayout.setColspan(sepaAccountHolder, 4);
        formLayout.setColspan(sepaMandateReference, 2);
        formLayout.setColspan(sepaIban, 4);
        formLayout.setColspan(sepaBic, 2);
    }

    private void checkSepaEnabled() {
        final var isSepaEnabled = sepaEnabled.getValue();
        sepaAllowInfo.getClassNames().set("disabled", !isSepaEnabled);
        sepaCancelInfo.getClassNames().set("disabled", !isSepaEnabled);
        sepaAccountHolder.setEnabled(isSepaEnabled);
        sepaAccountHolder.setRequiredIndicatorVisible(isSepaEnabled);
        sepaMandateReference.setEnabled(isSepaEnabled);
        sepaMandateReference.setRequiredIndicatorVisible(isSepaEnabled);
        sepaIban.setEnabled(isSepaEnabled);
        sepaIban.setRequiredIndicatorVisible(isSepaEnabled);
        sepaBic.setEnabled(isSepaEnabled);
        sepaBic.setRequiredIndicatorVisible(isSepaEnabled);

        if (isSepaEnabled && sepaMandateReference.getValue().isBlank()) {
            sepaMandateReference.setValue(generateSepaMandateReference(clubDeskOriginal));
        }
    }

    private void addJavaUserGroup(final @NotNull FormLayout formLayout) {
        javaUserGroup.setLabel(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.label.javaUserGroup"));
        javaUserGroup.setRequiredIndicatorVisible(true);
        binder.forField(javaUserGroup)
                .withValidator(value -> !value.isBlank(),
                        getTranslation("domain.clubdesk.boundary.ClubDeskEditView.error.javaUserGroup"))
                .bind(ClubDeskDto::jug, null);

        javaUserGroup.setItems(clubDeskService.getAllJavaUserGroups());
        javaUserGroup.setEmptySelectionAllowed(true);
        javaUserGroup.setEmptySelectionCaption(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.select.emptyCaption"));

        formLayout.add(javaUserGroup);
        formLayout.setColspan(javaUserGroup, 3);
    }

    private void addNewsletter(final @NotNull FormLayout formLayout) {
        final var locale = getLocale();
        newsletter.setLabel(getTranslation("domain.clubdesk.boundary.ClubDeskEditView.label.newsletter"));
        newsletter.setItems(NewsletterStatus.values());
        newsletter.setItemLabelGenerator(newsletterStatus ->
                getTranslation("domain.clubdesk.boundary.ClubDeskEditView.label.newsletter."
                        + newsletterStatus.name().toLowerCase(locale)));
        newsletter.setRequiredIndicatorVisible(true);

        formLayout.add(newsletter);
        formLayout.setColspan(newsletter, 6);
    }

    private boolean hasChanges() {
        final var isSepaEnabled = sepaEnabled.getValue();
        final var sepaAccountHolderValue = isSepaEnabled ? sepaAccountHolder.getValue().trim() : "";
        final var sepaIbanValue = isSepaEnabled ? sepaIban.getValue().trim() : "";
        final var sepaBicValue = isSepaEnabled ? sepaBic.getValue().trim() : "";

        return !Objects.equals(firstname.getValue().trim(), clubDeskOriginal.firstname())
                || !Objects.equals(lastname.getValue().trim(), clubDeskOriginal.lastname())
                || !Objects.equals(address.getValue().trim(), clubDeskOriginal.address())
                || !Objects.equals(addressAddition.getValue().trim(), clubDeskOriginal.addressAddition())
                || !Objects.equals(zipCode.getValue().trim(), clubDeskOriginal.zipCode())
                || !Objects.equals(city.getValue().trim(), clubDeskOriginal.city())
                || !Objects.equals(country.getValue(), clubDeskOriginal.country())
                || !Objects.equals(email.getValue().trim(), clubDeskOriginal.email())
                || !Objects.equals(emailAlternative.getValue().trim(), clubDeskOriginal.emailAlternative())
                || !Objects.equals(matrix.getValue().trim(), clubDeskOriginal.matrix())
                || !Objects.equals(mastodon.getValue().trim(), clubDeskOriginal.mastodon())
                || !Objects.equals(linkedin.getValue().trim(), clubDeskOriginal.linkedin())
                || sepaEnabled.getValue() != clubDeskOriginal.sepaEnabled()
                || !Objects.equals(sepaAccountHolderValue, clubDeskOriginal.sepaAccountHolder())
                || !Objects.equals(sepaIbanValue, clubDeskOriginal.sepaIban())
                || !Objects.equals(sepaBicValue, clubDeskOriginal.sepaBic())
                || !Objects.equals(javaUserGroup.getValue().trim(), clubDeskOriginal.jug())
                || (newsletter.getValue() == NewsletterStatus.ON) != clubDeskOriginal.newsletter();
    }

    private ClubDeskDto buildDtoFromFields() {
        final var isSepaEnabled = sepaEnabled.getValue();
        return new ClubDeskDto(
                clubDeskOriginal.id(),
                clubDeskOriginal.created(),
                clubDeskOriginal.updated(),
                firstname.getValue().trim(),
                lastname.getValue().trim(),
                address.getValue().trim(),
                addressAddition.getValue().trim(),
                zipCode.getValue().trim(),
                city.getValue().trim(),
                country.getValue(),
                email.getValue().trim(),
                emailAlternative.getValue().trim(),
                matrix.getValue().trim(),
                mastodon.getValue().trim(),
                linkedin.getValue().trim(),
                isSepaEnabled,
                isSepaEnabled ? sepaAccountHolder.getValue().trim() : "",
                isSepaEnabled ? sepaMandateReference.getValue().trim() : "",
                isSepaEnabled ? sepaIban.getValue().trim() : "",
                isSepaEnabled ? sepaBic.getValue().trim() : "",
                javaUserGroup.getValue(),
                newsletter.getValue() == NewsletterStatus.ON
        );
    }

    @Override
    protected @NotNull String getViewTitle() {
        return getTranslation("domain.clubdesk.boundary.ClubDeskEditView.title");
    }

}
