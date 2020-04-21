package com.aselsan.rehis.reform.mcsy.mcistemci.veriyapilari;

public class MesajNesnesi {

	public final String mesaj;
	public final String gonderenUuid;
	public final String proxyUuid;
	public final String aliciUuid;
	public final IcerikTipi icerikTipi;

	public MesajNesnesi(String mesaj, String gonderenUuid, IcerikTipi icerikTipi) {

		this(mesaj, gonderenUuid, "", icerikTipi);

	}

	public MesajNesnesi(String mesaj, String gonderenUuid, String aliciUuid, IcerikTipi icerikTipi) {

		this(mesaj, gonderenUuid, aliciUuid, aliciUuid, icerikTipi);

	}

	public MesajNesnesi(String mesaj, String gonderenUuid, String proxyUuid, String aliciUuid, IcerikTipi icerikTipi) {

		this.mesaj = mesaj;
		this.gonderenUuid = gonderenUuid;
		this.proxyUuid = proxyUuid;
		this.aliciUuid = aliciUuid;
		this.icerikTipi = icerikTipi;

	}

}
