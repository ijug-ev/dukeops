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

import com.github.mvysny.kaributesting.v10.ButtonKt;
import com.github.mvysny.kaributesting.v10.GridKt;
import com.github.mvysny.kaributesting.v10.UploadKt;
import com.github.mvysny.kaributesting.v10.pro.ConfirmDialogKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.provider.Query;
import eu.ijug.dukeops.domain.clubdesk.entity.ImportRecord;
import eu.ijug.dukeops.domain.dashboard.boundary.DashboardView;
import eu.ijug.dukeops.test.KaribuTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Comparator;
import java.util.stream.Stream;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static eu.ijug.dukeops.test.TestUtil.findChildByClassName;
import static org.assertj.core.api.Assertions.assertThat;

final class ClubDeskImportViewKT extends KaribuTest {

    @BeforeEach
    void setUpView() {
        login(TEST_ADMIN);
        UI.getCurrent().navigate(ClubDeskImportView.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void initialState_shouldDisableGridAndSaveButton_andHideErrors() {
        final var view = _get(ClubDeskImportView.class);
        final var upload = _get(view, Upload.class);
        final var grid = _get(view, Grid.class);
        final Button saveButton = findChildByClassName(view, Button.class, "save-button");
        final Paragraph importError = findChildByClassName(view, Paragraph.class, "import-error");
        final Paragraph saveError = findChildByClassName(view, Paragraph.class, "save-error");

        assertThat(upload.isEnabled()).isTrue();

        assertThat(grid.isEnabled()).isFalse();
        assertThat(grid.getDataProvider().size(new Query<>())).isZero();

        assertThat(saveButton.isEnabled()).isFalse();

        assertThat(importError.isVisible()).isFalse();
        assertThat(importError.getText()).isEmpty();

        assertThat(saveError.isVisible()).isFalse();
        assertThat(saveError.getText()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void happyFlow_uploadValidCsv_thenSave_shouldShowConfirmationDialogWithCount() throws Exception {
        final var view = _get(ClubDeskImportView.class);

        final var csvBytes = readResourceBytes("clubdesk/import-file-valid.csv");

        final var upload = _get(view, Upload.class);

        // Upload via Karibu (triggers UploadHandler -> temp file -> view callback)
        UploadKt._upload(upload, "import-file-valid.csv", "text/csv", csvBytes);

        final Grid<ImportRecord> grid = _get(view, Grid.class);
        assertThat(grid.isEnabled()).isTrue();
        assertThat(GridKt._size(grid)).isEqualTo(2);

        final var items = Stream.of(
                        GridKt._get(grid, 0),
                        GridKt._get(grid, 1))
                .sorted(Comparator.comparing(ImportRecord::email))
                .toList();

        assertThat(items).extracting(ImportRecord::email)
                .containsExactly("jane.doe@example.com", "john.doe@example.com");

        assertThat(items).extracting(ImportRecord::firstname)
                .containsExactly("Jane", "John");

        assertThat(items).extracting(ImportRecord::lastname)
                .containsExactly("Doe", "Doe");

        final var saveButton = findChildByClassName(view, Button.class, "save-button");
        assertThat(saveButton.isEnabled()).isTrue();

        final var importError = findChildByClassName(view, Paragraph.class, "import-error");
        assertThat(importError.isVisible()).isFalse();
        assertThat(importError.getText()).isEmpty();

        final var saveError = findChildByClassName(view, Paragraph.class, "save-error");
        assertThat(saveError.isVisible()).isFalse();
        assertThat(saveError.getText()).isEmpty();

        // Save and verify UI state change + confirmation dialog
        ButtonKt._click(saveButton);
        assertThat(saveButton.isEnabled()).isFalse();

        final var dialog = _get(ConfirmDialog.class);
        assertThat(dialog.isOpened()).isTrue();

        final var dialogHeader = ConfirmDialogKt.getHeader(dialog);
        assertThat(dialogHeader).contains("Import abgeschlossen");

        final var dialogText = ConfirmDialogKt.getText(dialog);
        assertThat(dialogText).contains("Es wurden 2 Datensätze importiert.");

        // Confirm dialog
        ConfirmDialogKt._fireConfirm(dialog);

        // Navigation happened
        assertThat(_get(DashboardView.class)).isNotNull();
    }

    @Test
    void showError_afterUploadingCorruptCsv() throws Exception {
        final var view = _get(ClubDeskImportView.class);

        final var csvBytes = readResourceBytes("clubdesk/import-file-missing-email.csv");

        final var upload = _get(view, Upload.class);

        // Upload via Karibu (triggers UploadHandler -> temp file -> view callback)
        UploadKt._upload(upload, "import-file-missing-email.csv", "text/csv", csvBytes);

        final var grid = _get(view, Grid.class);
        assertThat(grid.isEnabled()).isFalse();

        final var saveButton = findChildByClassName(view, Button.class, "save-button");
        assertThat(saveButton.isEnabled()).isFalse();

        final var importError = findChildByClassName(view, Paragraph.class, "import-error");
        assertThat(importError.isVisible()).isTrue();
        assertThat(importError.getText()).isEqualTo("CSV header missing expected column: 'E-Mail'");

        final var saveError = findChildByClassName(view, Paragraph.class, "save-error");
        assertThat(saveError.isVisible()).isFalse();
        assertThat(saveError.getText()).isEmpty();
    }

    @Test
    void showError_afterSavingRecordWithInvalidSepaData() throws Exception {
        final var view = _get(ClubDeskImportView.class);

        final var csvBytes = readResourceBytes("clubdesk/import-file-invalid-sepa.csv");

        final var upload = _get(view, Upload.class);
        UploadKt._upload(upload, "import-file-invalid-sepa.csv", "text/csv", csvBytes);

        final var grid = _get(view, Grid.class);
        assertThat(grid.isEnabled()).isTrue();

        final var saveButton = findChildByClassName(view, Button.class, "save-button");
        assertThat(saveButton.isEnabled()).isTrue();

        // trigger save -> should fail due to DB column length constraint
        ButtonKt._click(saveButton);

        assertThat(saveButton.isEnabled()).isFalse();

        final var saveError = findChildByClassName(view, Paragraph.class, "save-error");
        assertThat(saveError.isVisible()).isTrue();
        assertThat(saveError.getText()).isNotBlank();

        assertThat(grid.isEnabled()).isFalse();
        assertThat(upload.isEnabled()).isTrue();

        final var importError = findChildByClassName(view, Paragraph.class, "import-error");
        assertThat(importError.isVisible()).isFalse();
        assertThat(importError.getText()).isEmpty();
    }

    private static byte[] readResourceBytes(
            @SuppressWarnings("SameParameterValue") final @NotNull String classpathResource) throws Exception {
        try (InputStream is = ClubDeskImportViewKT.class.getClassLoader().getResourceAsStream(classpathResource)) {
            assertThat(is).as("Missing test resource: %s", classpathResource).isNotNull();
            return is.readAllBytes();
        }
    }

}
