package com.aselsan.rehis.reform.mcsy.mcsunucu.model.intf;

public interface ModelDinleyici {

	void yerelKullaniciyaGonder(String aliciUuid, String mesaj);

	void uzakKullaniciyaGonder(String aliciUuid, String mesaj);

	void tumUzakKullanicilaraGonder(String mesaj);

	void uuidYayinla(String uuid);

}
