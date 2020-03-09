package com.aselsan.rehis.reform.mcsy.sunum;

import javafx.scene.layout.Pane;

class MesajPane extends Pane {

	MesajPane() {

		super();

	}

//	BorderPane pane = new BorderPane();
//
//	VBox vb = new VBox(5);
//	HBox hb = new HBox(5);
//
//	vb.setPadding(new Insets(5));
//	hb.setPadding(new Insets(5));
//
//	ScrollPane sp = new ScrollPane(vb);
//	sp.setFitToWidth(true);
//
//	TextArea ta = new TextArea();
//	ta.setPrefRowCount(1);
//	ta.setWrapText(true);
//	HBox.setHgrow(ta, Priority.ALWAYS);
//	Button btn = new Button("send");
//	ta.setOnKeyPressed(e -> {
//
//		if (e.getCode().equals(KeyCode.ENTER)) {
//
//			btn.fire();
//
//			e.consume();
//
//		}
//
//	});
//	final AtomicInteger sira = new AtomicInteger(0);
//	btn.setOnAction(e -> {
//
//		HBox hbb = new HBox();
//		Label lbl = new Label(ta.getText());
//		lbl.setWrapText(true);
//		lbl.setBorder(new Border(new BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID, new CornerRadii(5),
//				new BorderWidths(1))));
//		Region r = new Region();
//		HBox.setHgrow(r, Priority.ALWAYS);
//		lbl.setPadding(new Insets(5, 10, 5, 10));
//		if (sira.getAndIncrement() % 2 == 0) {
//			lbl.setBackground(new Background(new BackgroundFill(Color.SEAGREEN, null, null)));
//			hbb.getChildren().addAll(r, lbl);
//		} else {
//			lbl.setBackground(new Background(new BackgroundFill(Color.LIGHTSLATEGRAY, null, null)));
//			hbb.getChildren().addAll(lbl, r);
//		}
//		lbl.maxWidthProperty().bind(Bindings.createDoubleBinding(() -> hbb.getWidth() * 0.80, hbb.widthProperty()));
//		vb.getChildren().add(hbb);
//		vb.layout();
//
//		ta.setText("");
//
//		System.out.println(
//				btn.getMinWidth() + "\t" + btn.getPrefWidth() + "\t" + btn.getMaxWidth() + "\t" + btn.getWidth());
//
//	});
//	sp.vvalueProperty().bind(vb.heightProperty());
//
//	hb.getChildren().addAll(ta, btn);
//
//	pane.setCenter(sp);
//	pane.setBottom(hb);
//
//	Scene scene = new Scene(pane, 400, 300);
//
//	arg0.setScene(scene);
//
//	arg0.show();

}
