package com.aselsan.rehis.reform.mcsy.sunum;

import javafx.geometry.Insets;
import javafx.scene.control.Accordion;
import javafx.scene.layout.VBox;

public class McPanel extends VBox {

	private final KimlikPane kimlikPane = new KimlikPane();
	private final KisilerPane kisilerPane = new KisilerPane();
	private final GruplarPane gruplarPane = new GruplarPane();
	private final Accordion kisilerGruplarPane = new Accordion();

	public McPanel() {

		super();

		init();

	}

	private void init() {

		setMargin(kimlikPane, new Insets(10));

		kisilerGruplarPane.getPanes().addAll(kisilerPane, gruplarPane);

		getChildren().addAll(kimlikPane, kisilerGruplarPane);

	}

}
