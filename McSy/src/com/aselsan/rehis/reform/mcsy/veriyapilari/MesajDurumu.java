package com.aselsan.rehis.reform.mcsy.veriyapilari;

import javafx.scene.paint.Color;

public enum MesajDurumu {

	OLUSTURULDU(Color.TRANSPARENT, Color.TRANSPARENT), GONDERILDI(Color.DARKGRAY, Color.TRANSPARENT),
	ULASTI(Color.DARKGRAY, Color.DARKGRAY), OKUNDU(Color.DEEPSKYBLUE, Color.DEEPSKYBLUE);

	private final Color beklemeRengi;
	private final Color iletildiRengi;

	private MesajDurumu(Color beklemeRengi, Color iletildiRengi) {

		this.beklemeRengi = beklemeRengi;
		this.iletildiRengi = iletildiRengi;

	}

	public Color getBeklemeRengi() {

		return beklemeRengi;

	}

	public Color getIletildiRengi() {

		return iletildiRengi;

	}

}
