package com.aselsan.rehis.reform.mcsy.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aselsan.rehis.reform.mcsy.model.intf.ModelDinleyici;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kimlik;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kisi;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class Model {

	private final Kimlik kimlik;

	private final Gson gson = new Gson();

	private final AtomicBoolean sunucuBagli = new AtomicBoolean(false);

	private final List<ModelDinleyici> dinleyiciler = Collections.synchronizedList(new ArrayList<ModelDinleyici>());

	public Model(Kimlik kimlik) {

		this.kimlik = kimlik;

		kimlik.setId(null);

	}

	public void dinleyiciEkle(ModelDinleyici dinleyici) {

		dinleyiciler.add(dinleyici);

	}

	public Kimlik getKimlik() {

		return kimlik;

	}

	public String getBeaconMesaji() {

		return gson.toJson(kimlik);

	}

	public boolean isSunucuBagli() {

		return sunucuBagli.get();

	}

	public void setSunucuBaglantiDurumu(boolean arg0) {

		sunucuBagli.set(arg0);

	}

	public void beaconAlindi(String mesaj) {

		try {

			Kisi kisi = gson.fromJson(mesaj, Kisi.class);

		} catch (JsonSyntaxException e) {

		}

	}

	public void uuidKoptu(String uuid) {

		// TODO

	}

}
