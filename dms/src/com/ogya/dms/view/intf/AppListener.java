package com.ogya.dms.view.intf;

import java.util.List;

public interface AppListener {

	void aciklamaGuncellendi(String aciklama);

	void durumGuncelleTiklandi();

	void kisiMesajPaneliAcildi(String uuid);

	void kisiMesajPaneliKapandi(String uuid);

	void mesajGonderTiklandi(String mesajTxt, String aliciUuid);

	void sayfaBasaKaydirildi(String uuid);

	void grupOlusturTalepEdildi(String grupAdi, List<String> seciliUuidler);

}
