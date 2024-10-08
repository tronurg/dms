package com.dms.core.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dms.core.view.component.DmsBox;
import com.dms.core.view.component.DmsScrollPane;
import com.dms.core.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ReportsPane extends GridPane {

	private final ComboBox<String> reportsComboBox = new ComboBox<String>();
	private final Button cancelBtn = ViewFactory.newCancelBtn();
	private final GridPane reportPaneHolder = new GridPane();
	private final Button attachBtn = ViewFactory.newAttachBtn();

	private final List<ReportPane> reportPanes = Collections.synchronizedList(new ArrayList<ReportPane>());

	private final List<ReportsListener> reportListeners = Collections
			.synchronizedList(new ArrayList<ReportsListener>());

	ReportsPane() {
		super();
	}

	void init(List<ReportTemplate> templates) {

		getStyleClass().addAll("gray-border", "padding-2", "hgap-2", "vgap-2");

		templates.forEach(template -> {

			if (reportsComboBox.getItems().contains(template.heading)) {
				return;
			}

			reportsComboBox.getItems().add(template.heading);

			ReportPane reportPane = new ReportPane(template.reportId, template.body);
			GridPane.setHgrow(reportPane, Priority.ALWAYS);
			GridPane.setVgrow(reportPane, Priority.ALWAYS);

			reportPanes.add(reportPane);

		});

		initReportsComboBox();
		initCancelBtn();
		initReportPaneHolder();
		initAttachBtn();

		add(reportsComboBox, 0, 0);
		add(cancelBtn, 0, 0);
		add(reportPaneHolder, 0, 1);
		add(DmsBox.wrap(attachBtn, Pos.BOTTOM_RIGHT, "padding-2"), 0, 1);

	}

	void addReportsListener(ReportsListener listener) {

		reportListeners.add(listener);

	}

	void reset() {

		reportPanes.forEach(reportPane -> reportPane.reset());
		reportsComboBox.getSelectionModel().selectFirst();

	}

	void setOnCancelAction(Runnable action) {

		cancelBtn.setOnAction(e -> action.run());

	}

	private void initReportsComboBox() {

		reportsComboBox.getSelectionModel().selectedIndexProperty().addListener((e0, e1, e2) -> updateReportPane());
		reportsComboBox.disableProperty().bind(Bindings.size(reportsComboBox.getItems()).isEqualTo(0));
		reportsComboBox.getSelectionModel().selectFirst();

	}

	private void initCancelBtn() {

		GridPane.setHalignment(cancelBtn, HPos.RIGHT);
		GridPane.setValignment(cancelBtn, VPos.TOP);

	}

	private void initReportPaneHolder() {

		GridPane.setHgrow(reportPaneHolder, Priority.ALWAYS);
		GridPane.setVgrow(reportPaneHolder, Priority.ALWAYS);

	}

	private void initAttachBtn() {

		attachBtn.opacityProperty()
				.bind(Bindings.createDoubleBinding(() -> attachBtn.isHover() ? 1.0 : 0.5, attachBtn.hoverProperty()));
		attachBtn.disableProperty().bind(Bindings.size(reportsComboBox.getItems()).isEqualTo(0));

		attachBtn.setOnAction(e -> {
			final ReportPane selectedReportPane = reportPanes
					.get(reportsComboBox.getSelectionModel().getSelectedIndex());
			reportListeners.forEach(listener -> listener.sendReportClicked(selectedReportPane.reportId,
					reportsComboBox.getValue(), selectedReportPane.getParagraphs()));
		});

	}

	private void updateReportPane() {

		reportPaneHolder.getChildren().clear();

		int selectedIndex = reportsComboBox.getSelectionModel().getSelectedIndex();

		if (selectedIndex < 0) {
			return;
		}

		if (selectedIndex < reportPanes.size()) {
			reportPaneHolder.add(reportPanes.get(selectedIndex), 0, 0);
		}

	}

	public static final class ReportTemplate {

		private final Integer reportId;
		private final String heading;
		private final String body;

		public ReportTemplate(Integer reportId, String heading, String body) {

			this.reportId = reportId;
			this.heading = heading;
			this.body = body;

		}

	}

	public static interface ReportsListener {

		void sendReportClicked(Integer reportId, String reportHeading, List<String> reportParagraphs);

	}

	private final class ReportPane extends SplitPane {

		private final Integer reportId;

		private final GridPane valuesPane = new GridPane();
		private final ScrollPane valuesScrollPane = new DmsScrollPane(valuesPane);
		private final VBox preview = new VBox();
		private final ScrollPane previewScrollPane = new DmsScrollPane(preview);

		private final List<TextField> textFields = Collections.synchronizedList(new ArrayList<TextField>());
		private final List<List<Label>> lines = Collections.synchronizedList(new ArrayList<List<Label>>());

		private ReportPane(Integer reportId, String templateBody) {

			super();

			this.reportId = reportId;

			fillTemplate(templateBody);

			init();

		}

		private void init() {

			valuesPane.getStyleClass().addAll("padding-2", "hgap-1", "vgap-1");
			preview.getStyleClass().addAll("padding-2");

			if (textFields.size() > 0) {
				getItems().add(valuesScrollPane);
			}

			getItems().add(previewScrollPane);

		}

		private void fillTemplate(String templateBody) {

			int valuesLine = 0;

			try (Scanner scanner = new Scanner(templateBody)) {

				while (scanner.hasNextLine()) {

					String lineStr = scanner.nextLine().replaceAll("\t", "    ");
					List<Label> words = new ArrayList<Label>();
					lines.add(words);

					HBox lineBox = new HBox();
					preview.getChildren().add(lineBox);

					Matcher matcher = Pattern.compile("<.*?>").matcher(lineStr);

					int regionStart = 0;

					while (matcher.find()) {

						if (regionStart < matcher.start()) {
							Label word = new Label(lineStr.substring(regionStart, matcher.start()));
							words.add(word);
							lineBox.getChildren().add(word);
						}

						Label word = new Label();
						words.add(word);
						lineBox.getChildren().add(word);

						String tag = matcher.group();

						Label label = new Label(tag.substring(1, tag.length() - 1));
						label.getStyleClass().addAll("max-width-17em");
						label.setTooltip(new Tooltip(label.getText()));
						TextField textField = new TextField();
						textField.getStyleClass().addAll("gray-underline");
						textFields.add(textField);

						word.textProperty().bind(textField.textProperty());

						valuesPane.add(label, 0, valuesLine, 1, 1);
						valuesPane.add(new Label(":"), 1, valuesLine, 1, 1);
						valuesPane.add(textField, 2, valuesLine, 1, 1);
						++valuesLine;

						regionStart = matcher.end();

					}

					if (regionStart < templateBody.length()) {
						Label word = new Label(lineStr.substring(regionStart));
						words.add(word);
						lineBox.getChildren().add(word);
					}

				}

			}

		}

		private List<String> getParagraphs() {

			List<String> paragraphs = new ArrayList<String>();

			lines.forEach(words -> {

				StringBuilder stringBuilder = new StringBuilder();

				words.forEach(word -> {

					stringBuilder.append(word.getText());

				});

				paragraphs.add(stringBuilder.toString());

			});

			return paragraphs;

		}

		private void reset() {

			textFields.forEach(textField -> textField.setText(""));

		}

	}

}
