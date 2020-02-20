package com.aselsan.rehis.reform.mcsy.mcsunucu.haberlesme.intf;

public interface TcpYoneticiDinleyici {

	void baglantiKuruldu(int id);

	void uzakUuidKoptu(String uuid);

	void mesajAlindi(String mesaj);

}
