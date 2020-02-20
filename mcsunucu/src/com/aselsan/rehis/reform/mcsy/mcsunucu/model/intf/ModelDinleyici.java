package com.aselsan.rehis.reform.mcsy.mcsunucu.model.intf;

public interface ModelDinleyici {

	void yerelKullanicilaraGonder(String aliciUuid, String mesaj);

	void tumUzakKullanicilaraGonder(String mesaj);

	void uuidYayinla(String uuid);

}
