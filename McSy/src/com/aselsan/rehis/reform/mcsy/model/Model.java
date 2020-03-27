package com.aselsan.rehis.reform.mcsy.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aselsan.rehis.reform.mcsy.model.intf.ModelDinleyici;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Grup;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kimlik;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kisi;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Mesaj;

public class Model {

	private final Kimlik kimlik;

	private final AtomicBoolean sunucuBagli = new AtomicBoolean(false);

	private final Map<String, Kisi> kisiler = Collections.synchronizedMap(new HashMap<String, Kisi>());
	private final Map<String, Grup> gruplar = Collections.synchronizedMap(new HashMap<String, Grup>());
	private final Map<String, Map<Long, Mesaj>> mesajlar = Collections
			.synchronizedMap(new HashMap<String, Map<Long, Mesaj>>());

	private final List<ModelDinleyici> dinleyiciler = Collections.synchronizedList(new ArrayList<ModelDinleyici>());

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

	public void addMesaj(Mesaj mesaj) {

		String uuid = kimlik.getUuid().equals(mesaj.getAliciUuid()) ? mesaj.getGonderenUuid() : mesaj.getAliciUuid();

		mesajlar.putIfAbsent(uuid, new HashMap<Long, Mesaj>());

		mesajlar.get(uuid).put(mesaj.getMesajId(), mesaj);

	}

	public Mesaj getMesaj(String uuid, Long mesajId) {

		if (!mesajlar.containsKey(uuid))
			return null;

		return mesajlar.get(uuid).get(mesajId);

	}

	public Map<String, Map<Long, Mesaj>> getMesajlar() {

		return mesajlar;

	}

}
