package com.ogya.dms.core.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ogya.dms.core.view.factory.ViewFactory;

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
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class ReportsPane extends GridPane {

	private static final double GAP = ViewFactory.GAP;

	private final ComboBox<String> reportsComboBox = new ComboBox<String>();
	private final Button cancelBtn = ViewFactory.newCancelBtn();
	private final GridPane reportPaneHolder = new GridPane();
	private final Button sendBtn = ViewFactory.newSendBtn();

	private final List<ReportPane> reportPanes = Collections.synchronizedList(new ArrayList<ReportPane>());

	private final List<ReportsListener> reportListeners = Collections
			.synchronizedList(new ArrayList<ReportsListener>());

	public ReportsPane(List<ReportTemplate> templates) {

		super();

		init(templates);

	}

	private void init(List<ReportTemplate> templates) {

		setPadding(new Insets(2 * GAP));
		setHgap(2 * GAP);
		setVgap(2 * GAP);

		setBorder(new Border(
				new BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

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

	public void addReportsListener(ReportsListener listener) {

		reportListeners.add(listener);

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

		cancelBtn.setOnAction(e -> reportListeners.forEach(listener -> listener.cancelReportClicked()));

	}

	private void initReportPaneHolder() {

		GridPane.setHgrow(reportPaneHolder, Priority.ALWAYS);
		GridPane.setVgrow(reportPaneHolder, Priority.ALWAYS);

	}

	private void initSendBtn() {

		GridPane.setMargin(sendBtn, new Insets(10.0));
		GridPane.setHalignment(sendBtn, HPos.RIGHT);
		GridPane.setValignment(sendBtn, VPos.BOTTOM);

		sendBtn.opacityProperty()
				.bind(Bindings.createDoubleBinding(() -> sendBtn.isHover() ? 1.0 : 0.5, sendBtn.hoverProperty()));
		sendBtn.disableProperty().bind(Bindings.size(reportsComboBox.getItems()).isEqualTo(0));

		sendBtn.setOnAction(
				e -> reportListeners.forEach(listener -> listener.sendReportClicked(reportsComboBox.getValue(),
						reportPanes.get(reportsComboBox.getSelectionModel().getSelectedIndex()).getParagraphs())));

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

	public static interface ReportsListener {

		void sendReportClicked(String reportHeading, List<String> reportParagraphs);

		void cancelReportClicked();

	}

	private final class ReportPane extends HBox {

		private final GridPane valuesPane = new GridPane();
		private final ScrollPane valuesScrollPane = new ScrollPane(valuesPane) {
			@Override
			public void requestFocus() {
			}
		};
		private final VBox preview = new VBox();
		private final ScrollPane previewScrollPane = new ScrollPane(preview) {
			@Override
			public void requestFocus() {
			}
		};

		private final List<TextField> textFields = Collections.synchronizedList(new ArrayList<TextField>());
		private final List<List<Label>> lines = Collections.synchronizedList(new ArrayList<List<Label>>());

		private ReportPane(String templateBody) {

			super(2 * GAP);

			fillTemplate(templateBody);

			init();

		}

		private void init() {

			valuesPane.setPadding(new Insets(2 * GAP));
			valuesPane.setHgap(GAP);
			valuesPane.setVgap(GAP);

			preview.setPadding(new Insets(2 * GAP));

			HBox.setHgrow(previewScrollPane, Priority.ALWAYS);

			if (textFields.size() > 0)
				getChildren().addAll(valuesScrollPane, new Separator(Orientation.VERTICAL));

			getChildren().add(previewScrollPane);

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
						TextField textField = new TextField();
						textField.setStyle("-fx-border-color: gray;-fx-border-width: 0 0 1 0;");
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