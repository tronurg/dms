package com.ogya.dms.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ogya.dms.view.factory.ViewFactory;

import javafx.beans.binding.Bindings;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class ReportsPane extends GridPane {

	private final ComboBox<String> reportsComboBox = new ComboBox<String>();
	private final Button cancelBtn = ViewFactory.newCancelBtn();
	private final GridPane reportPaneHolder = new GridPane();
	private final Button sendBtn = ViewFactory.newSendBtn();

	private final List<ReportPane> reportPanes = Collections.synchronizedList(new ArrayList<ReportPane>());

	public ReportsPane(List<ReportTemplate> templates) {

		super();

		init(templates);

	}

	private void init(List<ReportTemplate> templates) {

		setPadding(new Insets(10.0));
		setHgap(10.0);
		setVgap(10.0);

		templates.forEach(template -> {

			if (reportsComboBox.getItems().contains(template.heading))
				return;

			reportsComboBox.getItems().add(template.heading);

			ReportPane reportPane = new ReportPane(template.body);
			GridPane.setHgrow(reportPane, Priority.ALWAYS);
			GridPane.setVgrow(reportPane, Priority.ALWAYS);

			reportPanes.add(reportPane);

		});

		initReportsComboBox();
		initCancelBtn();
		initReportPaneHolder();
		initSendBtn();

		add(reportsComboBox, 0, 0);
		add(cancelBtn, 0, 0);
		add(reportPaneHolder, 0, 1);
		add(sendBtn, 0, 1);

	}

	public void reset() {

		reportPanes.forEach(reportPane -> reportPane.reset());

		reportsComboBox.getSelectionModel().selectFirst();

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

	private void initSendBtn() {

		GridPane.setMargin(sendBtn, new Insets(10.0));
		GridPane.setHalignment(sendBtn, HPos.RIGHT);
		GridPane.setValignment(sendBtn, VPos.BOTTOM);

	}

	private void updateReportPane() {

		reportPaneHolder.getChildren().clear();

		int selectedIndex = reportsComboBox.getSelectionModel().getSelectedIndex();

		if (selectedIndex < 0)
			return;

		if (selectedIndex < reportPanes.size())
			reportPaneHolder.add(reportPanes.get(selectedIndex), 0, 0);

	}

	public static final class ReportTemplate {

		private final String heading;
		private final String body;

		public ReportTemplate(String heading, String body) {

			this.heading = heading;
			this.body = body;

		}

	}

	private final class ReportPane extends HBox {

		private final GridPane valuesPane = new GridPane();
		private final ScrollPane valuesScrollPane = new ScrollPane(valuesPane) {
			@Override
			public void requestFocus() {
			}
		};
		private final TextFlow preview = new TextFlow();
		private final ScrollPane previewScrollPane = new ScrollPane(preview) {
			@Override
			public void requestFocus() {
			}
		};

		private final List<TextField> textFields = Collections.synchronizedList(new ArrayList<TextField>());
		private final List<Text> texts = Collections.synchronizedList(new ArrayList<Text>());

		private ReportPane(String templateBody) {

			super(10.0);

			init();

			fillTemplate(templateBody);

		}

		private void init() {

			valuesPane.setPadding(new Insets(10.0));
			valuesPane.setHgap(5.0);
			valuesPane.setVgap(5.0);

			preview.setPadding(new Insets(10.0));

			HBox.setHgrow(previewScrollPane, Priority.ALWAYS);

			getChildren().addAll(valuesScrollPane, new Separator(Orientation.VERTICAL), previewScrollPane);

		}

		private void fillTemplate(String templateBody) {

			Matcher matcher = Pattern.compile("<.*?>").matcher(templateBody);

			int regionStart = 0;
			int line = 0;

			while (matcher.find()) {

				if (regionStart < matcher.start()) {
					Text text = new Text(templateBody.substring(regionStart, matcher.start()));
					texts.add(text);
					preview.getChildren().add(text);
				}

				Text text = new Text();
				texts.add(text);
				preview.getChildren().add(text);

				String tag = matcher.group();

				Label label = new Label(tag.substring(1, tag.length() - 1));
				TextField textField = new TextField();
				text.textProperty().bind(textField.textProperty());

				textFields.add(textField);

				valuesPane.add(label, 0, line, 1, 1);
				valuesPane.add(new Label(":"), 1, line, 1, 1);
				valuesPane.add(textField, 2, line, 1, 1);
				++line;

				regionStart = matcher.end();

			}

			if (regionStart < templateBody.length()) {
				Text text = new Text(templateBody.substring(regionStart));
				texts.add(text);
				preview.getChildren().add(text);
			}

		}

		private String getText() {

			final StringBuilder stringBuilder = new StringBuilder();

			texts.forEach(text -> stringBuilder.append(text.getText()));

			return stringBuilder.toString();

		}

		private void reset() {

			textFields.forEach(textField -> textField.setText(""));

		}

	}

}
