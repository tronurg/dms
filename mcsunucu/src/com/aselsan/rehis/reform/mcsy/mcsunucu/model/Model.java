package com.aselsan.rehis.reform.mcsy.mcsunucu.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.aselsan.rehis.reform.mcsy.mcsunucu.model.intf.ModelDinleyici;
import com.aselsan.rehis.reform.mcsy.mcsunucu.veriyapilari.MesajNesnesi;
import com.aselsan.rehis.reform.mcsy.mcsunucu.veriyapilari.MesajTipi;
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

			switch (mesajNesnesi.mesajTipi) {

			case BCON:

				if (!mesajNesnesiStr.equals(yerelKullaniciBeacon.get(gonderenUuid))) {

					boolean yeniEklendi = !yerelKullaniciBeacon.containsKey(gonderenUuid);

					// Yerel uuid yeni eklendi veya guncellendi.
					// Beacon, yerel beacon'lara eklenecek.
					// Yeni beacon tum yerel ve uzak kullanicilara dagitilacak.

					yerelKullaniciBeacon.put(gonderenUuid, mesajNesnesiStr);
					yerelKullaniciBeacon.forEach((aliciUuid, mesaj) -> {

						if (aliciUuid.equals(gonderenUuid))
							return;

						dinleyici.yerelKullaniciyaGonder(aliciUuid, mesajNesnesiStr);

					});
					dinleyici.tumUzakKullanicilaraGonder(mesajNesnesiStr);

					if (yeniEklendi)
						kullaniciyaTumBeaconlariGonder(gonderenUuid);

				}

				// Gonderen uuid agda yayinlanacak
				dinleyici.uuidYayinla(gonderenUuid);

				break;

			case REQ_BCON:

				kullaniciyaTumBeaconlariGonder(gonderenUuid);

				break;

			case MESAJ:

				String aliciUuid = mesajNesnesi.aliciUuid;

				if (yerelKullaniciBeacon.containsKey(aliciUuid))
					dinleyici.yerelKullaniciyaGonder(mesajNesnesi.aliciUuid, mesajNesnesiStr);
				else if (uzakKullaniciBeacon.containsKey(aliciUuid))
					dinleyici.uzakKullaniciyaGonder(mesajNesnesi.aliciUuid, mesajNesnesiStr);

				break;

			default:

			}

		} catch (JsonSyntaxException e) {

			e.printStackTrace();

		}

	}

	public void uzakMesajAlindi(String mesajNesnesiStr) {

		try {

			MesajNesnesi mesajNesnesi = gson.fromJson(mesajNesnesiStr, MesajNesnesi.class);

			String gonderenUuid = mesajNesnesi.gonderenUuid;

			switch (mesajNesnesi.mesajTipi) {

			case BCON:

				if (!mesajNesnesiStr.equals(uzakKullaniciBeacon.get(gonderenUuid))) {

					// Uzak uuid yeni eklendi veya guncellendi.
					// Beacon, uzak beacon'larda guncellenecek.
					// Yeni beacon tum yerel kullanicilara dagitilacak.

					uzakKullaniciBeacon.put(gonderenUuid, mesajNesnesiStr);
					yerelKullaniciBeacon.forEach(
							(aliciUuid, mesaj) -> dinleyici.yerelKullaniciyaGonder(aliciUuid, mesajNesnesiStr));

				}

				break;

			case UUID_KOPTU:

				uzakKullaniciKoptu(mesajNesnesi.mesaj);

				break;

			case MESAJ:

				if (yerelKullaniciBeacon.containsKey(mesajNesnesi.aliciUuid))
					dinleyici.yerelKullaniciyaGonder(mesajNesnesi.aliciUuid, mesajNesnesiStr);
				else if (uzakKullaniciBeacon.containsKey(mesajNesnesi.aliciUuid))
					dinleyici.uzakKullaniciyaGonder(mesajNesnesi.aliciUuid, mesajNesnesiStr);

				break;

			default:

			}

		} catch (JsonSyntaxException e) {

			e.printStackTrace();

		}

	}

	public void tumYerelBeaconlariIsle(Consumer<String> consumer) {

		yerelKullaniciBeacon.forEach((aliciUuid, mesaj) -> consumer.accept(mesaj));

	}

	public void tumYerelKullanicilariTestEt() {

		yerelKullaniciBeacon.forEach((aliciUuid, mesaj) -> dinleyici.yerelKullaniciyaGonder(aliciUuid, ""));

	}

	public void uzakKullaniciKoptu(String uuid) {

		if (!uzakKullaniciBeacon.containsKey(uuid))
			return;

		String mesajNesnesiStr = gson.toJson(new MesajNesnesi(uuid, "", MesajTipi.UUID_KOPTU));

		uzakKullaniciBeacon.remove(uuid);

		yerelKullaniciBeacon
				.forEach((aliciUuid, mesaj) -> dinleyici.yerelKullaniciyaGonder(aliciUuid, mesajNesnesiStr));

	}

	public void yerelKullaniciKoptu(String uuid) {

		if (!yerelKullaniciBeacon.containsKey(uuid))
			return;

		String mesajNesnesiStr = gson.toJson(new MesajNesnesi(uuid, "", MesajTipi.UUID_KOPTU));

		yerelKullaniciBeacon.remove(uuid);

		yerelKullaniciBeacon
				.forEach((aliciUuid, mesaj) -> dinleyici.yerelKullaniciyaGonder(aliciUuid, mesajNesnesiStr));

		dinleyici.tumUzakKullanicilaraGonder(mesajNesnesiStr);

	}

	private void kullaniciyaTumBeaconlariGonder(String aliciUuid) {

		yerelKullaniciBeacon.forEach((uuid, beacon) -> {

			if (aliciUuid.equals(uuid))
				return;

			dinleyici.yerelKullaniciyaGonder(aliciUuid, beacon);

		});

		uzakKullaniciBeacon.forEach((uuid, beacon) -> {

			dinleyici.yerelKullaniciyaGonder(aliciUuid, beacon);

		});

	}

}
