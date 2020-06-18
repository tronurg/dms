package com.ogya.dms.dmsclient.intf;

public interface DmsClientListener {

	void beaconAlindi(String mesaj);

	void mesajAlindi(String mesaj);

	void kullaniciKoptu(String uuid);

	void sunucuBaglantiDurumuGuncellendi(boolean baglantiDurumu);

	void mesajDurumuIstendi(String mesaj, String karsiTarafUuid);

	void karsiTarafMesajiAlmadi(String mesaj, String karsiTarafUuid);

	void karsiTarafMesajiAldi(String mesaj, String karsiTarafUuid);

	void karsiTarafMesajiOkudu(String mesaj, String karsiTarafUuid);

}
