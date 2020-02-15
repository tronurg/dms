package com.aselsan.rehis.reform.mcsy.mcistemci;

public class MesajNesnesi {

	public final String mesaj;
	public final String aliciUuid;
	public final String tip;

	public MesajNesnesi(String mesaj, String tip) {

		this(mesaj, "", tip);

	}

	public MesajNesnesi(String mesaj, String aliciUuid, String tip) {

		this.mesaj = mesaj;
		this.aliciUuid = aliciUuid;
		this.tip = tip;

	}

}
