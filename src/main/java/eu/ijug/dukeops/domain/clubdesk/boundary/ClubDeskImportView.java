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

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadI18N;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;
import eu.ijug.dukeops.domain.clubdesk.control.ClubDeskService;
import eu.ijug.dukeops.domain.clubdesk.entity.ImportRecord;
import eu.ijug.dukeops.domain.dashboard.boundary.DashboardView;
import eu.ijug.dukeops.infra.ui.vaadin.control.Navigator;
import eu.ijug.dukeops.infra.ui.vaadin.layout.AbstractView;
import eu.ijug.dukeops.infra.ui.vaadin.layout.WebsiteLayout;
import jakarta.annotation.security.RolesAllowed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@RolesAllowed("ADMIN")
@Route(value = "clubdesk/import", layout = WebsiteLayout.class)
public final class ClubDeskImportView extends AbstractView {

    private static final int MAX_FILE_SIZE_IN_BYTES = 10 * 1024 * 1024; // 10MB
    private static final @NotNull String[] ACCEPTED_MIME_TYPES = {"text/csv"};
    private static final @NotNull Logger LOGGER = getLogger(ClubDeskImportView.class);

    private final @NotNull ClubDeskService clubDeskService;
    private final @NotNull Navigator navigator;
    private final @NotNull Upload upload;
    private final @NotNull Paragraph importError;
    private final @NotNull Grid<ImportRecord> grid;
    private final @NotNull Button saveButton;
    private final @NotNull Paragraph saveError;

    private List<ImportRecord> importRecords = List.of();

    public ClubDeskImportView(final @NotNull ClubDeskService clubDeskService,
                              final @NotNull Navigator navigator) {
        super();
        this.clubDeskService = clubDeskService;
        this.navigator = navigator;
        addClassName("clubdesk-import-view");

        add(new H3(getViewTitle()));

        add(new H4(getTranslation("domain.clubdesk.boundary.ClubDeskImportView.step1")));

        final var uploadHandler = UploadHandler
                .toTempFile(this::processUploadSuccess);

        final var uploadI18N = new UploadI18N();
        uploadI18N.setAddFiles(new UploadI18N.AddFiles().setOne(
                getTranslation("domain.clubdesk.boundary.ClubDeskImportView.uploadButton")));
        uploadI18N.setDropFiles(new UploadI18N.DropFiles().setOne(
                getTranslation("domain.clubdesk.boundary.ClubDeskImportView.uploadDrop")));
        uploadI18N.setError(new UploadI18N.Error().setIncorrectFileType(
                getTranslation("domain.clubdesk.boundary.ClubDeskImportView.uploadIncorrectFileType")));

        upload = new Upload(uploadHandler);
        upload.setSizeFull();
        upload.setMaxFileSize(MAX_FILE_SIZE_IN_BYTES);
        upload.setMaxFiles(1);
        upload.setDropAllowed(true);
        upload.setAcceptedFileTypes(ACCEPTED_MIME_TYPES);
        upload.setI18n(uploadI18N);
        add(upload);

        importError = new Paragraph();
        importError.addClassNames("error", "import-error");
        importError.setVisible(false);
        add(importError);

        add(new H4(getTranslation("domain.clubdesk.boundary.ClubDeskImportView.step2")));
        grid = new Grid<>(ImportRecord.class, false);
        grid.addColumn(ImportRecord::firstname).setHeader(getFieldName("firstname"));
        grid.addColumn(ImportRecord::lastname).setHeader(getFieldName("lastname"));
        grid.addColumn(ImportRecord::address).setHeader(getFieldName("address"));
        grid.addColumn(ImportRecord::addressAddition).setHeader(getFieldName("addressAddition"));
        grid.addColumn(ImportRecord::zipCode).setHeader(getFieldName("zipCode"));
        grid.addColumn(ImportRecord::city).setHeader(getFieldName("city"));
        grid.addColumn(ImportRecord::country).setHeader(getFieldName("country"));
        grid.addColumn(ImportRecord::email).setHeader(getFieldName("email"));
        grid.addColumn(ImportRecord::emailAlternative).setHeader(getFieldName("emailAlternative"));
        grid.addColumn(ImportRecord::matrix).setHeader(getFieldName("matrix"));
        grid.addColumn(ImportRecord::mastodon).setHeader(getFieldName("mastodon"));
        grid.addColumn(ImportRecord::linkedin).setHeader(getFieldName("linkedin"));
        grid.addColumn(ImportRecord::sepaEnabled).setHeader(getFieldName("sepaEnabled"));
        grid.addColumn(ImportRecord::sepaAccountHolder).setHeader(getFieldName("sepaAccountHolder"));
        grid.addColumn(ImportRecord::sepaIban).setHeader(getFieldName("sepaIban"));
        grid.addColumn(ImportRecord::sepaBic).setHeader(getFieldName("sepaBic"));
        grid.addColumn(ImportRecord::jug).setHeader(getFieldName("jug"));
        grid.setEnabled(false);
        add(grid);

        add(new H4(getTranslation("domain.clubdesk.boundary.ClubDeskImportView.step3")));
        saveButton = new Button(getTranslation("domain.clubdesk.boundary.ClubDeskImportView.saveButton"));
        saveButton.addClassName("save-button");
        saveButton.addClickListener(this::saveImportRecords);
        saveButton.setEnabled(false);
        add(saveButton);

        saveError = new Paragraph();
        saveError.addClassNames("error", "save-error");
        saveError.setVisible(false);
        add(saveError);
    }

    private void processUploadSuccess(final @NotNull UploadMetadata metadata, final @NotNull File file) {
        try {
            upload.setEnabled(false);
            importError.setText("");
            importError.setVisible(false);
            saveError.setText("");
            saveError.setVisible(false);
            file.deleteOnExit();
            importRecords = clubDeskService.importClubDeskFile(file);
            grid.setItems(importRecords);
            grid.setEnabled(true);
            saveButton.setEnabled(true);
        } catch (final Exception e) {
            LOGGER.error("Error importing from CSV file: {}", e.getMessage(), e);
            importError.setText(e.getMessage());
            importError.setVisible(true);
            grid.setEnabled(false);
            upload.setEnabled(true);
            saveButton.setEnabled(false);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    private String getFieldName(final @NotNull String key) {
        return getTranslation("domain.clubdesk.boundary.ClubDeskImportView.fieldName." + key);
    }

    private void saveImportRecords(final @Nullable ClickEvent<Button> clickEvent) {
        try {
            saveButton.setEnabled(false);
            final var success = clubDeskService.saveImportRecords(importRecords);

            final var dialog = new ConfirmDialog();
            dialog.setHeader(getTranslation("domain.clubdesk.boundary.ClubDeskImportView.successDialog.title"));
            dialog.setText(getTranslation("domain.clubdesk.boundary.ClubDeskImportView.successDialog.text", success));

            dialog.setCloseOnEsc(false);
            dialog.setCancelable(false);

            dialog.setConfirmText(getTranslation("domain.clubdesk.boundary.ClubDeskImportView.successDialog.button"));
            dialog.addConfirmListener(_ -> {
                dialog.close();
                navigator.navigate(getUI().orElseThrow(), DashboardView.class);
            });
            dialog.open();
        } catch (final Exception e) {
            LOGGER.error("Error saving import record: {}", e.getMessage(), e);
            saveError.setText(e.getMessage());
            saveError.setVisible(true);
            grid.setEnabled(false);
            upload.setEnabled(true);
        }
    }

    @Override
    protected @NotNull String getViewTitle() {
        return getTranslation("domain.clubdesk.boundary.ClubDeskImportView.title");
    }

}
