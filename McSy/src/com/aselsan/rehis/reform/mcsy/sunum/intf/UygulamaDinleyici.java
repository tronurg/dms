package com.aselsan.rehis.reform.mcsy.sunum.intf;

public interface UygulamaDinleyici {

	void aciklamaGuncellendi(String aciklama);

	void durumGuncelleTiklandi();

	void kisiMesajPaneliAcildi(String uuid);

	void kisiMesajPaneliKapandi(String uuid);

	void mesajGonderTiklandi(String mesaj, String aliciUuid);

}
