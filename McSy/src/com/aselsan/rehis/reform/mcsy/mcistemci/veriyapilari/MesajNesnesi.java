package com.aselsan.rehis.reform.mcsy.mcistemci.veriyapilari;

public class MesajNesnesi {

	public final String mesaj;
	public final String gonderenUuid;
	public final String aliciUuid;
	public final MesajTipi mesajTipi;

	public MesajNesnesi(String mesaj, String gonderenUuid, MesajTipi mesajTipi) {

		this(mesaj, gonderenUuid, "", mesajTipi);

	}

	public MesajNesnesi(String mesaj, String gonderenUuid, String aliciUuid, MesajTipi mesajTipi) {

		this.mesaj = mesaj;
		this.gonderenUuid = gonderenUuid;
		this.aliciUuid = aliciUuid;
		this.mesajTipi = mesajTipi;

	}

}
