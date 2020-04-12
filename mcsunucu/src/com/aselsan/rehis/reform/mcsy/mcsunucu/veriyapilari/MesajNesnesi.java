package com.aselsan.rehis.reform.mcsy.mcsunucu.veriyapilari;

public class MesajNesnesi {

	public final String mesaj;
	public final String gonderenUuid;
	public final String aliciUuid;
	public final String proxyUuid;
	public final IcerikTipi icerikTipi;

	public MesajNesnesi(String mesaj, String gonderenUuid, IcerikTipi icerikTipi) {

		this(mesaj, gonderenUuid, "", "", icerikTipi);

	}

	public MesajNesnesi(String mesaj, String gonderenUuid, String aliciUuid, IcerikTipi icerikTipi) {

		this(mesaj, gonderenUuid, aliciUuid, aliciUuid, icerikTipi);

	}

	public MesajNesnesi(String mesaj, String gonderenUuid, String aliciUuid, String proxyUuid, IcerikTipi icerikTipi) {

		this.mesaj = mesaj;
		this.gonderenUuid = gonderenUuid;
		this.aliciUuid = aliciUuid;
		this.proxyUuid = proxyUuid;
		this.icerikTipi = icerikTipi;

	}

}
