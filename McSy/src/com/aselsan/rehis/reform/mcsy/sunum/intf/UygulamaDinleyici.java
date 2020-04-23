package com.aselsan.rehis.reform.mcsy.sunum.intf;

import java.util.List;

public interface UygulamaDinleyici {

	void aciklamaGuncellendi(String aciklama);

	void durumGuncelleTiklandi();

	void kisiMesajPaneliAcildi(String uuid);

	void kisiMesajPaneliKapandi(String uuid);

	void mesajGonderTiklandi(String mesajTxt, String aliciUuid);

	void sayfaBasaKaydirildi(String uuid);

	void grupOlusturTalepEdildi(String grupAdi, List<String> seciliUuidler);

}
