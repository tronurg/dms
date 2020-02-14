package com.onurg.mc.sunucu;

class UdpNesnesi {

	final String mesaj;
	final int gonderenPort;
	final int aliciPort;

	UdpNesnesi(String mesaj, int gonderenPort, int aliciPort) {

		this.mesaj = mesaj;
		this.gonderenPort = gonderenPort;
		this.aliciPort = aliciPort;

	}

}
