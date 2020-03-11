package com.aselsan.rehis.reform.mcsy.veriyapilari;

import javafx.scene.paint.Color;

public enum KisiDurumu {

	MUSAIT(Color.LIMEGREEN), UZAKTA(Color.ORANGE), MESGUL(Color.RED), KISITLI(Color.BLUE), CEVRIMDISI(Color.GRAY);

	private final Color durumRengi;

	private KisiDurumu(Color durumRengi) {

		this.durumRengi = durumRengi;

	}

	public Color getDurumRengi() {

		return durumRengi;

	}

}
