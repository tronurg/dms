package com.aselsan.rehis.reform.mcsy.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aselsan.rehis.reform.mcsy.model.intf.ModelDinleyici;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Grup;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kimlik;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kisi;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Mesaj;
import com.aselsan.rehis.reform.mcsy.veriyapilari.KisiDurumu;

public class Model {

	private final Kimlik kimlik;

	private final AtomicBoolean sunucuBagli = new AtomicBoolean(false);

	private final Map<String, Kisi> kisiler = Collections.synchronizedMap(new HashMap<String, Kisi>());
	private final Map<String, Grup> gruplar = Collections.synchronizedMap(new HashMap<String, Grup>());
	private final Map<String, Mesaj> mesajlar = Collections.synchronizedMap(new LinkedHashMap<String, Mesaj>());

	private final List<ModelDinleyici> dinleyiciler = Collections.synchronizedList(new ArrayList<ModelDinleyici>());

	private final List<String> acikUuidler = Collections.synchronizedList(new ArrayList<String>());

	private final Map<String, Set<String>> gidenMesajlarBekleyen = Collections
			.synchronizedMap(new LinkedHashMap<String, Set<String>>());
	private final Map<String, Set<String>> gelenMesajlarBekleyen = Collections
			.synchronizedMap(new LinkedHashMap<String, Set<String>>());

	public Model(Kimlik kimlik) {

		this.kimlik = kimlik;

	}

	public void dinleyiciEkle(ModelDinleyici dinleyici) {

		dinleyiciler.add(dinleyici);

	}

	public Kimlik getKimlik() {

		return kimlik;

	}

	public void aciklamaGuncelle(String aciklama) {

		kimlik.setAciklama(aciklama);

	}

	public void durumGuncelle(KisiDurumu durum) {

		kimlik.setDurum(durum);

	}

	public boolean isSunucuBagli() {

		return sunucuBagli.get();

	}

	public void setSunucuBaglantiDurumu(boolean arg0) {

		sunucuBagli.set(arg0);

	}

	public void addKisi(Kisi kisi) {

		kisiler.put(kisi.getUuid(), kisi);

	}

	public Kisi getKisi(String uuid) {

		return kisiler.get(uuid);

	}

	public boolean isKisiCevrimici(String uuid) {

		return !getKisi(uuid).getDurum().equals(KisiDurumu.CEVRIMDISI);

	}

	public Map<String, Kisi> getKisiler() {

		return kisiler;

	}

	public void addGrup(Grup grup) {

		gruplar.put(grup.getUuid(), grup);

	}

	public Grup getGrup(String uuid) {

		return gruplar.get(uuid);

	}

	public Map<String, Grup> getGruplar() {

		return gruplar;

	}

	public void addMesaj(String mesajId, Mesaj mesaj) {

		mesajlar.put(mesajId, mesaj);

		if (kimlik.getUuid().equals(mesaj.getGonderenUuid())) {
			// Giden mesaj

			String baglantiUuid = mesaj.getAliciUuid();

			gidenMesajlarBekleyen.putIfAbsent(baglantiUuid, new LinkedHashSet<String>());

			switch (mesaj.getMesajDurumu()) {

			case OLUSTURULDU:
			case GONDERILDI:
			case ULASTI:

				gidenMesajlarBekleyen.get(baglantiUuid).add(mesajId);

				break;

			case OKUNDU:

				gidenMesajlarBekleyen.get(baglantiUuid).remove(mesajId);

				break;

			}

		} else if (kimlik.getUuid().equals(mesaj.getAliciUuid())) {
			// Gelen mesaj

			String baglantiUuid = mesaj.getGonderenUuid();

			gelenMesajlarBekleyen.putIfAbsent(baglantiUuid, new LinkedHashSet<String>());

			switch (mesaj.getMesajDurumu()) {

			case ULASTI:

				gelenMesajlarBekleyen.get(baglantiUuid).add(mesajId);

				break;

			case OKUNDU:

				gelenMesajlarBekleyen.get(baglantiUuid).remove(mesajId);

				break;

			default:

				break;

			}

		}

	}

	public Map<String, Mesaj> getMesajlar() {

		return mesajlar;

	}

	public void mesajPaneliAcildi(String uuid) {

		acikUuidler.add(uuid);

	}

	public void mesajPaneliKapandi(String uuid) {

		acikUuidler.remove(uuid);

	}

	public boolean isMesajPaneliAcik(String uuid) {

		return acikUuidler.contains(uuid);

	}

	public Set<String> getGidenBekleyenMesajIdleri(String uuid) {

		gidenMesajlarBekleyen.putIfAbsent(uuid, new LinkedHashSet<String>());

		return gidenMesajlarBekleyen.get(uuid);

	}

	public Set<String> getGelenBekleyenMesajIdleri(String uuid) {

		gelenMesajlarBekleyen.putIfAbsent(uuid, new LinkedHashSet<String>());

		return gelenMesajlarBekleyen.get(uuid);

	}

	public Mesaj getMesajByMesajId(String mesajId) {

		return mesajlar.get(mesajId);

	}

}
