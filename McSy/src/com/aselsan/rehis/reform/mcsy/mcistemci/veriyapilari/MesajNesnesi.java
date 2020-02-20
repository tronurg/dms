package com.aselsan.rehis.reform.mcsy.mcistemci.veriyapilari;

public class MesajNesnesi {

	public final String mesaj;
	public final String gonderenUuid;
	public final String aliciUuid;
	public final String tip;

	public MesajNesnesi(String mesaj, String gonderenUuid, String tip) {

		this(mesaj, gonderenUuid, "", tip);

	}

	public MesajNesnesi(String mesaj, String gonderenUuid, String aliciUuid, String tip) {

		this.mesaj = mesaj;
		this.gonderenUuid = gonderenUuid;
		this.aliciUuid = aliciUuid;
		this.tip = tip;

	}

}
