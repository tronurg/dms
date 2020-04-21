package com.aselsan.rehis.reform.mcsy.mcsunucu.model.intf;

import java.util.List;

public interface ModelDinleyici {

	void yerelKullaniciyaGonder(String aliciUuid, String mesaj);

	void uzakKullaniciyaGonder(String aliciUuid, String mesaj);

	void uzakKullanicilaraGonder(List<String> aliciUuidler, String mesaj);

	void tumUzakKullanicilaraGonder(String mesaj);

	void uuidYayinla(String uuid);

}
