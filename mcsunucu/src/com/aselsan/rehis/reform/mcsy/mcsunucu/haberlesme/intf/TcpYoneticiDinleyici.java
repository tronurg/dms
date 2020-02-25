package com.aselsan.rehis.reform.mcsy.mcsunucu.haberlesme.intf;

public interface TcpYoneticiDinleyici {

	void uzakSunucuyaBaglanildi(String mcUuid);

	void uzakKullaniciKoptu(String uuid);

	void mesajAlindi(String mesaj);

}
