package com.aselsan.rehis.reform.mcsy.mcistemci.intf;

public interface McIstemciDinleyici {

	void beaconAlindi(String mesaj);

	void kullaniciKoptu(String uuid);

	void sunucuBaglantiDurumuGuncellendi(boolean baglantiDurumu);

}
