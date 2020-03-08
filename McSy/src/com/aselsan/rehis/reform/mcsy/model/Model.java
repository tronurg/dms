package com.aselsan.rehis.reform.mcsy.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aselsan.rehis.reform.mcsy.model.intf.ModelDinleyici;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Grup;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kimlik;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kisi;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Mesaj;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

public class Model {

	private final Kimlik kimlik;

	private final AtomicBoolean sunucuBagli = new AtomicBoolean(false);

	private final ObservableMap<String, Kisi> kisiler = FXCollections.observableMap(new LinkedHashMap<String, Kisi>());
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

	public boolean isSunucuBagli() {

		return sunucuBagli.get();

	}

	public void setSunucuBaglantiDurumu(boolean arg0) {

		sunucuBagli.set(arg0);

	}

	public Kisi getKisi(String uuid) {

		return kisiler.get(uuid);

	}

	public ObservableMap<String, Kisi> getKisiler() {

		return kisiler;

	}

	public void kisiEkle(Kisi kisi) {

		kisiler.put(kisi.getUuid(), kisi);

	}

	public void grupEkle(Grup grup) {

		gruplar.put(grup.getUuid(), grup);

	}

	public void mesajEkle(Mesaj mesaj) {

		mesajlar.putIfAbsent(mesaj.getGonderenUuid(), new HashMap<Long, Mesaj>());

		mesajlar.get(mesaj.getGonderenUuid()).put(mesaj.getMesajId(), mesaj);

	}

	public void uuidKoptu(String uuid) {

		// TODO

	}

}
