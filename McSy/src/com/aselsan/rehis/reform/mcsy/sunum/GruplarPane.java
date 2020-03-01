package com.aselsan.rehis.reform.mcsy.sunum;

import com.aselsan.rehis.reform.mcsy.ortak.OrtakMetotlar;

import javafx.beans.binding.Bindings;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

class GruplarPane extends TitledPane {

	private final VBox gruplar = new VBox();

	GruplarPane() {

		super();

		init();

	}

	private void init() {

		setText(OrtakMetotlar.cevir("GRUPLAR"));
		setContent(gruplar);

		disableProperty().bind(Bindings.isEmpty(gruplar.getChildren()));

	}

}
