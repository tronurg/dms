package com.aselsan.rehis.reform.mcsy.mcsunucu.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.aselsan.rehis.reform.mcsy.mcsunucu.model.intf.ModelDinleyici;
import com.aselsan.rehis.reform.mcsy.mcsunucu.veriyapilari.MesajNesnesi;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class Model {

	private final ModelDinleyici dinleyici;

	private final Gson gson = new Gson();

	private final Map<String, String> yerelKullaniciBeacon = Collections.synchronizedMap(new HashMap<String, String>());
	private final Map<String, String> uzakKullaniciBeacon = Collections.synchronizedMap(new HashMap<String, String>());

	public Model(ModelDinleyici dinleyici) {

		this.dinleyici = dinleyici;

	}

	public void yerelMesajAlindi(final String mesajNesnesiStr) {

		try {

			MesajNesnesi mesajNesnesi = gson.fromJson(mesajNesnesiStr, MesajNesnesi.class);

			String gonderenUuid = mesajNesnesi.gonderenUuid;

			switch (mesajNesnesi.tip) {

			case "BCON":

				if (!mesajNesnesiStr.equals(yerelKullaniciBeacon.get(gonderenUuid))) {

					// Yerel uuid yeni eklendi veya guncellendi.
					// Beacon, yerel beacon'lara eklenecek.
					// Yeni beacon tum yerel ve uzak kullanicilara dagitilacak.

					yerelKullaniciBeacon.put(gonderenUuid, mesajNesnesiStr);
					dinleyici.yerelKullanicilaraGonder("", mesajNesnesiStr);
					dinleyici.tumUzakKullanicilaraGonder(mesajNesnesiStr);

				}

				// Gonderen uuid agda yayinlanacak

				dinleyici.uuidYayinla(gonderenUuid);

				break;

			case "BCON?":

				yerelKullaniciBeacon.forEach((uuid, beacon) -> {

					if (gonderenUuid.equals(uuid))
						return;

					dinleyici.yerelKullanicilaraGonder(gonderenUuid, beacon);

				});

				uzakKullaniciBeacon.forEach((uuid, beacon) -> {

					dinleyici.yerelKullanicilaraGonder(gonderenUuid, beacon);

				});

				break;

			default:

			}

		} catch (JsonSyntaxException e) {

		}

	}

	public void uzakMesajAlindi(String mesajNesnesiStr) {

		try {

			MesajNesnesi mesajNesnesi = gson.fromJson(mesajNesnesiStr, MesajNesnesi.class);

			String gonderenUuid = mesajNesnesi.gonderenUuid;

			switch (mesajNesnesi.tip) {

			case "BCON":

				if (!mesajNesnesiStr.equals(uzakKullaniciBeacon.get(gonderenUuid))) {

					// Uzak uuid yeni eklendi veya guncellendi.
					// Beacon, uzak beacon'larda guncellenecek.
					// Yeni beacon tum yerel kullanicilara dagitilacak.

					uzakKullaniciBeacon.put(gonderenUuid, mesajNesnesiStr);
					dinleyici.yerelKullanicilaraGonder("", mesajNesnesiStr);

				}

				break;

			case "UUID_KOPTU":

				uzakKullaniciKoptu(mesajNesnesi.mesaj);

				break;

			default:

			}

		} catch (JsonSyntaxException e) {

		}

	}

	public Map<String, String> tumYerelBeaconlariAl() {

		return yerelKullaniciBeacon;

	}

	public void uzakKullaniciKoptu(String uuid) {

		String mesajNesnesiStr = gson.toJson(new MesajNesnesi(uuid, "", "UUID_KOPTU"));

		uzakKullaniciBeacon.remove(uuid);

		dinleyici.yerelKullanicilaraGonder("", mesajNesnesiStr);

	}

	public void yerelKullaniciKoptu(String uuid) {

		// TODO: kontrolde bu metodu cagiracak metot yazilacak

		String mesajNesnesiStr = gson.toJson(new MesajNesnesi(uuid, "", "UUID_KOPTU"));

		yerelKullaniciBeacon.remove(uuid);

		dinleyici.yerelKullanicilaraGonder("", mesajNesnesiStr);

		dinleyici.tumUzakKullanicilaraGonder(mesajNesnesiStr);

	}

}
