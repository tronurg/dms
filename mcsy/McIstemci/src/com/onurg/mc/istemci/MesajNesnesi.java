package com.onurg.mc.istemci;

import java.util.Arrays;

public class MesajNesnesi {

	public final byte[] mesaj;
	public final String gonderenIp;
	public final int gonderenPort;
	public final String gonderenId;
	public final String aliciIp;
	public final int aliciPort;
	public final String aliciId;

	MesajNesnesi(byte[] mesaj, String gonderenIp, String gonderenId, String aliciIp) {

		this(mesaj, gonderenIp, gonderenId, aliciIp, "");

	}

	MesajNesnesi(byte[] mesaj, String gonderenIp, String gonderenId, String aliciIp, String aliciId) {

		this.mesaj = mesaj;
		this.gonderenIp = gonderenIp;
		this.gonderenPort = 0;
		this.gonderenId = gonderenId;
		this.aliciIp = aliciIp;
		this.aliciPort = 0;
		this.aliciId = aliciId;

	}

	MesajNesnesi(MesajNesnesi mesajNesnesi, int gonderenPort, int aliciPort) {

		this.mesaj = mesajNesnesi.mesaj;
		this.gonderenIp = mesajNesnesi.gonderenIp;
		this.gonderenPort = gonderenPort;
		this.gonderenId = mesajNesnesi.gonderenId;
		this.aliciIp = mesajNesnesi.aliciIp;
		this.aliciPort = aliciPort;
		this.aliciId = mesajNesnesi.aliciId;

	}

	@Override
	public String toString() {
		return "MesajNesnesi [mesaj=" + Arrays.toString(mesaj) + ", gonderenIp=" + gonderenIp + ", gonderenPort="
				+ gonderenPort + ", gonderenId=" + gonderenId + ", aliciIp=" + aliciIp + ", aliciPort=" + aliciPort
				+ ", aliciId=" + aliciId + "]";
	}

}
