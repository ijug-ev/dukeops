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

import com.github.mvysny.kaributesting.v10.pro.ConfirmDialogKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import eu.ijug.dukeops.domain.clubdesk.control.ClubDeskService;
import eu.ijug.dukeops.domain.clubdesk.entity.ClubDeskDto;
import eu.ijug.dukeops.domain.clubdesk.entity.Country;
import eu.ijug.dukeops.domain.clubdesk.entity.NewsletterStatus;
import eu.ijug.dukeops.domain.dashboard.boundary.DashboardView;
import eu.ijug.dukeops.domain.user.control.UserService;
import eu.ijug.dukeops.domain.user.entity.UserDto;
import eu.ijug.dukeops.domain.user.entity.UserRole;
import eu.ijug.dukeops.test.KaribuTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.github.mvysny.kaributesting.v10.BasicUtilsKt._fireDomEvent;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

public class ClubDeskEditViewKT extends KaribuTest {

    @Autowired
    private ClubDeskService clubDeskService;

    @Autowired
    private UserService userService;

    @Test
    void navigateToView_withUser_withoutClubDeskRecord_showsErrorDialog() {
        login(TEST_USER);

        final var clubDeskDto = clubDeskService.getClubDeskForCurrentUser();
        assertThat(clubDeskDto).isEmpty();

        UI.getCurrent().navigate(ClubDeskEditView.class);

        final var dialog = _get(ConfirmDialog.class);
        assertThat(dialog.isOpened()).isTrue();
        assertThat(ConfirmDialogKt.getHeader(dialog)).contains("Error");
        assertThat(ConfirmDialogKt.getText(dialog))
                .contains("No data could be loaded from ClubDesk. Please contact the iJUG Office.");

        // Confirm dialog
        ConfirmDialogKt._fireConfirm(dialog);

        // Navigation happened
        assertThat(_get(DashboardView.class)).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void navigateToView_withUser_withClubDeskRecord_showsEditForm() {
        final var userDto = userService.storeUser(
                new UserDto(null, null, null,
                        "John Doe", "john.doe@example.com", UserRole.USER));
        assertThat(userDto.id()).isNotNull();

        clubDeskService.save(new ClubDeskDto(userDto.id(), null, null,
                "John", "Doe", "", "", "", "", null,
                "john.doe@example.com", "", "", "", "",
                false, "", "", "", "",
                "", true));

        login(userDto);

        final var clubDeskDto = clubDeskService.getClubDeskForCurrentUser();
        assertThat(clubDeskDto).isNotEmpty();

        UI.getCurrent().navigate(ClubDeskEditView.class);

        final var firstname = _get(TextField.class, spec -> spec.withLabel("First name"));
        final var lastname = _get(TextField.class, spec -> spec.withLabel("Last name"));

        final var address = _get(TextField.class, spec -> spec.withLabel("Street and house number"));
        final var addressAddition = _get(TextField.class, spec -> spec.withLabel("Address addition"));
        final var zipCode = _get(TextField.class, spec -> spec.withLabel("ZIP code"));
        final var city = _get(TextField.class, spec -> spec.withLabel("City"));
        final var country = _get(Select.class, spec -> spec.withLabel("Country"));

        final var email = _get(EmailField.class, spec -> spec.withLabel("Email"));
        final var alternativeEmail = _get(EmailField.class, spec -> spec.withLabel("Alternative email"));
        final var matrix = _get(TextField.class, spec -> spec.withLabel("Matrix"));
        final var mastodon = _get(TextField.class, spec -> spec.withLabel("Mastodon"));
        final var linkedIn = _get(TextField.class, spec -> spec.withLabel("LinkedIn"));

        final var sepaEnabled = _get(Checkbox.class, spec -> spec.withLabel("SEPA direct debit enabled"));
        final var sepaAccountHolder = _get(TextField.class, spec -> spec.withLabel("Account holder"));
        final var sepaIban = _get(TextField.class, spec -> spec.withLabel("IBAN"));
        final var sepaBic = _get(TextField.class, spec -> spec.withLabel("BIC"));

        final var newsletter = _get(Select.class, spec -> spec.withLabel("Club information"));
        final var javaUserGroup = _get(Select.class, spec -> spec.withLabel("Java User Group"));

        final var saveButton = _get(Button.class, spec -> spec.withText("Submit changes"));

        assertThat(firstname.getValue()).isEqualTo("John");
        assertThat(lastname.getValue()).isEqualTo("Doe");

        assertThat(address.getValue()).isEmpty();
        assertThat(addressAddition.getValue()).isEmpty();
        assertThat(zipCode.getValue()).isEmpty();
        assertThat(city.getValue()).isEmpty();
        assertThat(country.isEmpty()).isTrue();

        assertThat(email.getValue()).isEqualTo("john.doe@example.com");
        assertThat(alternativeEmail.getValue()).isEmpty();
        assertThat(matrix.getValue()).isEmpty();
        assertThat(mastodon.getValue()).isEmpty();
        assertThat(linkedIn.getValue()).isEmpty();

        assertThat(sepaEnabled.getValue()).isFalse();
        assertThat(sepaAccountHolder.isEnabled()).isFalse();
        assertThat(sepaAccountHolder.getValue()).isEmpty();
        assertThat(sepaIban.isEnabled()).isFalse();
        assertThat(sepaIban.getValue()).isEmpty();
        assertThat(sepaBic.isEnabled()).isFalse();
        assertThat(sepaBic.getValue()).isEmpty();

        assertThat(newsletter.getValue()).isEqualTo(NewsletterStatus.ON);
        assertThat(javaUserGroup.getValue()).isEqualTo("");

        assertThat(saveButton.isEnabled()).isFalse();

        // set required fields step by step and check save button state
        address.setValue("Example Street 1");
        assertThat(saveButton.isEnabled()).isFalse();
        zipCode.setValue("12345");
        assertThat(saveButton.isEnabled()).isFalse();
        city.setValue("Example City");
        assertThat(saveButton.isEnabled()).isFalse();
        country.setValue(Country.ofIso2("US"));
        assertThat(saveButton.isEnabled()).isFalse();
        javaUserGroup.setValue("Java User Group Switzerland");
        assertThat(saveButton.isEnabled()).isTrue();

        // save and verify success dialog
        saveButton.click();
        final var dialog1 = _get(ConfirmDialog.class);
        assertThat(dialog1.isOpened()).isTrue();
        assertThat(ConfirmDialogKt.getHeader(dialog1)).contains("Changes saved");
        assertThat(ConfirmDialogKt.getText(dialog1))
                .contains("Your changes have been successfully saved and submitted to the iJUG office.");
        ConfirmDialogKt._fireConfirm(dialog1);
        assertThat(saveButton.isEnabled()).isFalse();

        // check that data was actually saved in the backend
        final var updatedClubDeskForCurrentUser1 = clubDeskService.getClubDeskForCurrentUser();
        assertThat(updatedClubDeskForCurrentUser1).isPresent();
        final var updatedClubDeskDto1 = updatedClubDeskForCurrentUser1.orElseThrow();
        assertThat(updatedClubDeskDto1.firstname()).isEqualTo("John");
        assertThat(updatedClubDeskDto1.lastname()).isEqualTo("Doe");
        assertThat(updatedClubDeskDto1.address()).isEqualTo("Example Street 1");
        assertThat(updatedClubDeskDto1.zipCode()).isEqualTo("12345");
        assertThat(updatedClubDeskDto1.city()).isEqualTo("Example City");
        assertThat(updatedClubDeskDto1.country()).isEqualTo(Country.ofIso2("US"));
        assertThat(updatedClubDeskDto1.email()).isEqualTo("john.doe@example.com");
        assertThat(updatedClubDeskDto1.newsletter()).isTrue();
        assertThat(updatedClubDeskDto1.jug()).isEqualTo("Java User Group Switzerland");

        // enable SEPA and check that related fields are enabled
        sepaEnabled.setValue(true);
        assertThat(sepaAccountHolder.isEnabled()).isTrue();
        assertThat(sepaIban.isEnabled()).isTrue();
        assertThat(sepaBic.isEnabled()).isTrue();
        assertThat(saveButton.isEnabled()).isFalse();

        // fill SEPA fields with incorrect IBAN/BIC and check that save button is disabled
        sepaAccountHolder.setValue("John Doe");
        assertThat(saveButton.isEnabled()).isFalse();
        sepaIban.setValue("xx00000000000000000000");
        _fireDomEvent(sepaIban, "blur");
        assertThat(sepaIban.getValue()).isEqualTo("XX00000000000000000000");
        assertThat(saveButton.isEnabled()).isFalse();
        sepaBic.setValue("xx000000000");
        _fireDomEvent(sepaBic, "blur");
        assertThat(sepaBic.getValue()).isEqualTo("XX000000000");
        assertThat(saveButton.isEnabled()).isFalse();

        // disable SEPA should enable save button, even with incorrect IBAN/BIC, as SEPA data will be ignored when saving
        sepaEnabled.setValue(false);
        addressAddition.setValue("Foobar");
        assertThat(saveButton.isEnabled()).isTrue();
        addressAddition.setValue("");

        // fill SEPA fields with correct IBAN/BIC and check that save button is enabled
        sepaEnabled.setValue(true);
        sepaAccountHolder.setValue("John Doe");
        assertThat(saveButton.isEnabled()).isFalse();
        sepaIban.setValue("DE02120300000000202051");
        assertThat(saveButton.isEnabled()).isFalse();
        sepaBic.setValue("BYLADEM1001");
        assertThat(saveButton.isEnabled()).isTrue();

        // disable SEPA and check that related fields are disabled
        sepaEnabled.setValue(false);
        assertThat(sepaAccountHolder.isEnabled()).isFalse();
        assertThat(sepaIban.isEnabled()).isFalse();
        assertThat(sepaBic.isEnabled()).isFalse();
        assertThat(saveButton.isEnabled()).isFalse();

        // enable SEPA again
        sepaEnabled.setValue(true);
        assertThat(sepaAccountHolder.isEnabled()).isTrue();
        assertThat(sepaIban.isEnabled()).isTrue();
        assertThat(sepaBic.isEnabled()).isTrue();
        assertThat(saveButton.isEnabled()).isTrue();

        // save and verify success dialog
        saveButton.click();
        final var dialog2 = _get(ConfirmDialog.class);
        assertThat(dialog2.isOpened()).isTrue();
        assertThat(ConfirmDialogKt.getHeader(dialog2)).contains("Changes saved");
        assertThat(ConfirmDialogKt.getText(dialog2))
                .contains("Your changes have been successfully saved and submitted to the iJUG office.");
        ConfirmDialogKt._fireConfirm(dialog2);
        assertThat(saveButton.isEnabled()).isFalse();

        // check that data was actually saved in the backend
        final var updatedClubDeskForCurrentUser2 = clubDeskService.getClubDeskForCurrentUser();
        assertThat(updatedClubDeskForCurrentUser2).isPresent();
        final var updatedClubDeskDto2 = updatedClubDeskForCurrentUser2.orElseThrow();
        assertThat(updatedClubDeskDto2.sepaEnabled()).isTrue();
        assertThat(updatedClubDeskDto2.sepaAccountHolder()).isEqualTo("John Doe");
        assertThat(updatedClubDeskDto2.sepaIban()).isEqualTo("DE02120300000000202051");
        assertThat(updatedClubDeskDto2.sepaBic()).isEqualTo("BYLADEM1001");

        // disable newsletter and check that save button is enabled
        newsletter.setValue(NewsletterStatus.OFF);
        assertThat(saveButton.isEnabled()).isTrue();

        // save and verify success dialog
        saveButton.click();
        final var dialog3 = _get(ConfirmDialog.class);
        assertThat(dialog3.isOpened()).isTrue();
        assertThat(ConfirmDialogKt.getHeader(dialog3)).contains("Changes saved");
        assertThat(ConfirmDialogKt.getText(dialog3))
                .contains("Your changes have been successfully saved and submitted to the iJUG office.");
        ConfirmDialogKt._fireConfirm(dialog3);
        assertThat(saveButton.isEnabled()).isFalse();

        // check that data was actually saved in the backend
        final var updatedClubDeskForCurrentUser3 = clubDeskService.getClubDeskForCurrentUser();
        assertThat(updatedClubDeskForCurrentUser3).isPresent();
        final var updatedClubDeskDto3 = updatedClubDeskForCurrentUser3.orElseThrow();
        assertThat(updatedClubDeskDto3.newsletter()).isFalse();

        // change email and check that save button is enabled
        email.setValue("john_doe@example.com");
        assertThat(saveButton.isEnabled()).isTrue();

        // save and verify success dialog
        saveButton.click();

        final var dialog4 = _get(ConfirmDialog.class);
        assertThat(dialog4.isOpened()).isTrue();
        assertThat(ConfirmDialogKt.getHeader(dialog4)).contains("Changes saved");
        assertThat(ConfirmDialogKt.getText(dialog4))
                .contains("Your changes have been successfully saved and submitted to the iJUG office.");
        ConfirmDialogKt._fireConfirm(dialog4);
        assertThat(saveButton.isEnabled()).isFalse();

        // check that data was actually saved in the backend
        final var updatedClubDeskForCurrentUser4 = clubDeskService.getClubDeskForCurrentUser();
        assertThat(updatedClubDeskForCurrentUser4).isPresent();
        final var updatedClubDeskDto4 = updatedClubDeskForCurrentUser4.orElseThrow();
        assertThat(updatedClubDeskDto4.email()).isEqualTo("john_doe@example.com");

        // check that email change was also updated in the user record
        final var userAfterEmailChange = userService.getUserById(userDto.id()).orElseThrow();
        assertThat(userAfterEmailChange.email()).isEqualTo("john_doe@example.com");
    }

    @Test
    @SuppressWarnings({"unchecked", "DataFlowIssue"})
    void navigateToView_modifyEverythingOnce() {
        final var userDto = userService.storeUser(
                new UserDto(null, null, null,
                        "John Doe", "john.doe@example.com", UserRole.USER));
        assertThat(userDto.id()).isNotNull();

        clubDeskService.save(new ClubDeskDto(userDto.id(), null, null,
                "John", "Doe", "", "", "", "", null,
                "john.doe@example.com", "", "", "", "",
                true, "test", "test", "test", "test",
                "", true));

        login(userDto);

        final var clubDeskDto = clubDeskService.getClubDeskForCurrentUser();
        assertThat(clubDeskDto).isNotEmpty();

        UI.getCurrent().navigate(ClubDeskEditView.class);

        final var firstname = _get(TextField.class, spec -> spec.withLabel("First name"));
        final var lastname = _get(TextField.class, spec -> spec.withLabel("Last name"));

        final var address = _get(TextField.class, spec -> spec.withLabel("Street and house number"));
        final var addressAddition = _get(TextField.class, spec -> spec.withLabel("Address addition"));
        final var zipCode = _get(TextField.class, spec -> spec.withLabel("ZIP code"));
        final var city = _get(TextField.class, spec -> spec.withLabel("City"));
        final var country = _get(Select.class, spec -> spec.withLabel("Country"));

        final var email = _get(EmailField.class, spec -> spec.withLabel("Email"));
        final var alternativeEmail = _get(EmailField.class, spec -> spec.withLabel("Alternative email"));
        final var matrix = _get(TextField.class, spec -> spec.withLabel("Matrix"));
        final var mastodon = _get(TextField.class, spec -> spec.withLabel("Mastodon"));
        final var linkedIn = _get(TextField.class, spec -> spec.withLabel("LinkedIn"));

        final var sepaEnabled = _get(Checkbox.class, spec -> spec.withLabel("SEPA direct debit enabled"));
        final var sepaAccountHolder = _get(TextField.class, spec -> spec.withLabel("Account holder"));
        final var sepaIban = _get(TextField.class, spec -> spec.withLabel("IBAN"));
        final var sepaBic = _get(TextField.class, spec -> spec.withLabel("BIC"));

        final var newsletter = _get(Select.class, spec -> spec.withLabel("Club information"));
        final var javaUserGroup = _get(Select.class, spec -> spec.withLabel("Java User Group"));

        final var saveButton = _get(Button.class, spec -> spec.withText("Submit changes"));

        assertThat(firstname.getValue()).isEqualTo("John");
        assertThat(lastname.getValue()).isEqualTo("Doe");

        assertThat(address.getValue()).isEmpty();
        assertThat(addressAddition.getValue()).isEmpty();
        assertThat(zipCode.getValue()).isEmpty();
        assertThat(city.getValue()).isEmpty();
        assertThat(country.isEmpty()).isTrue();

        assertThat(email.getValue()).isEqualTo("john.doe@example.com");
        assertThat(alternativeEmail.getValue()).isEmpty();
        assertThat(matrix.getValue()).isEmpty();
        assertThat(mastodon.getValue()).isEmpty();
        assertThat(linkedIn.getValue()).isEmpty();

        assertThat(sepaEnabled.getValue()).isTrue();
        assertThat(sepaAccountHolder.isEnabled()).isTrue();
        assertThat(sepaAccountHolder.getValue()).isEqualTo("test");
        assertThat(sepaIban.isEnabled()).isTrue();
        assertThat(sepaIban.getValue()).isEqualTo("test");
        assertThat(sepaBic.isEnabled()).isTrue();
        assertThat(sepaBic.getValue()).isEqualTo("test");

        assertThat(newsletter.getValue()).isEqualTo(NewsletterStatus.ON);
        assertThat(javaUserGroup.getValue()).isEqualTo("");

        assertThat(saveButton.isEnabled()).isFalse();

        newsletter.setValue(NewsletterStatus.OFF);
        assertThat(newsletter.getValue()).isEqualTo(NewsletterStatus.OFF);

        javaUserGroup.setValue("Java User Group Switzerland");
        assertThat(javaUserGroup.getValue()).isEqualTo("Java User Group Switzerland");

        sepaBic.setValue("COBADEFFXXX");
        assertThat(sepaBic.isEnabled()).isTrue();
        assertThat(sepaBic.getValue()).isEqualTo("COBADEFFXXX");

        sepaIban.setValue("US89370400440532013000");
        assertThat(sepaIban.isEnabled()).isTrue();
        assertThat(sepaIban.getValue()).isEqualTo("US89370400440532013000");

        sepaAccountHolder.setValue("Jane Doe-Smith");
        assertThat(sepaAccountHolder.isEnabled()).isTrue();
        assertThat(sepaAccountHolder.getValue()).isEqualTo("Jane Doe-Smith");

        sepaEnabled.setValue(false);
        assertThat(sepaEnabled.getValue()).isFalse();

        linkedIn.setValue("https://www.linkedin.com/in/janedoe");
        assertThat(linkedIn.getValue()).isEqualTo("https://www.linkedin.com/in/janedoe");

        mastodon.setValue("@janedoe@example.com");
        assertThat(mastodon.getValue()).isEqualTo("@janedoe@example.com");

        matrix.setValue("@janedoe:example.com");
        assertThat(matrix.getValue()).isEqualTo("@janedoe:example.com");

        alternativeEmail.setValue("jane.doe-smith@example.com");
        assertThat(alternativeEmail.getValue()).isEqualTo("jane.doe-smith@example.com");

        email.setValue("jane.doe@example.com");
        assertThat(email.getValue()).isEqualTo("jane.doe@example.com");

        country.setValue(Country.ofIso2("US"));
        assertThat(country.getValue()).isEqualTo(Country.ofIso2("US"));

        city.setValue("Example City");
        assertThat(city.getValue()).isEqualTo("Example City");

        zipCode.setValue("12345");
        assertThat(zipCode.getValue()).isEqualTo("12345");

        addressAddition.setValue("Siute 2");
        assertThat(addressAddition.getValue()).isEqualTo("Siute 2");

        address.setValue("Example Street 1");
        assertThat(address.getValue()).isEqualTo("Example Street 1");

        lastname.setValue("Doe-Smith");
        assertThat(lastname.getValue()).isEqualTo("Doe-Smith");

        firstname.setValue("Jane");
        assertThat(firstname.getValue()).isEqualTo("Jane");

        assertThat(saveButton.isEnabled()).isTrue();

        // save and verify success dialog
        saveButton.click();

        final var dialog = _get(ConfirmDialog.class);
        assertThat(dialog.isOpened()).isTrue();
        assertThat(ConfirmDialogKt.getHeader(dialog)).contains("Changes saved");
        assertThat(ConfirmDialogKt.getText(dialog))
                .contains("Your changes have been successfully saved and submitted to the iJUG office.");
        ConfirmDialogKt._fireConfirm(dialog);
        assertThat(saveButton.isEnabled()).isFalse();

        // check that data was actually saved in the backend
        final var updatedClubDeskForCurrentUser = clubDeskService.getClubDeskForCurrentUser();
        assertThat(updatedClubDeskForCurrentUser).isPresent();
        final var updatedClubDeskDto = updatedClubDeskForCurrentUser.orElseThrow();
        assertThat(updatedClubDeskDto.firstname()).isEqualTo("Jane");
        assertThat(updatedClubDeskDto.lastname()).isEqualTo("Doe-Smith");
        assertThat(updatedClubDeskDto.address()).isEqualTo("Example Street 1");
        assertThat(updatedClubDeskDto.addressAddition()).isEqualTo("Siute 2");
        assertThat(updatedClubDeskDto.zipCode()).isEqualTo("12345");
        assertThat(updatedClubDeskDto.city()).isEqualTo("Example City");
        assertThat(updatedClubDeskDto.country().iso2()).isEqualTo("US");
        assertThat(updatedClubDeskDto.email()).isEqualTo("jane.doe@example.com");
        assertThat(updatedClubDeskDto.emailAlternative()).isEqualTo("jane.doe-smith@example.com");
        assertThat(updatedClubDeskDto.matrix()).isEqualTo("@janedoe:example.com");
        assertThat(updatedClubDeskDto.mastodon()).isEqualTo("@janedoe@example.com");
        assertThat(updatedClubDeskDto.linkedin()).isEqualTo("https://www.linkedin.com/in/janedoe");
        assertThat(updatedClubDeskDto.sepaEnabled()).isFalse();
        assertThat(updatedClubDeskDto.sepaAccountHolder()).isEqualTo("");
        assertThat(updatedClubDeskDto.sepaIban()).isEqualTo("");
        assertThat(updatedClubDeskDto.sepaBic()).isEqualTo("");
        assertThat(updatedClubDeskDto.newsletter()).isFalse();
        assertThat(updatedClubDeskDto.jug()).isEqualTo("Java User Group Switzerland");

        // check that email change was also updated in the user record
        final var userAfterEmailChange = userService.getUserById(userDto.id()).orElseThrow();
        assertThat(userAfterEmailChange.email()).isEqualTo("jane.doe@example.com");
    }

}
