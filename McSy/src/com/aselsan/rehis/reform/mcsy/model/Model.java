package com.aselsan.rehis.reform.mcsy.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aselsan.rehis.reform.mcsy.model.intf.ModelDinleyici;
import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kimlik;
import com.google.gson.Gson;

public class Model {

	private final Kimlik kimlik;

	private final Gson gson = new Gson();

	private final AtomicBoolean sunucuBagli = new AtomicBoolean(false);

	private final List<ModelDinleyici> dinleyiciler = Collections.synchronizedList(new ArrayList<ModelDinleyici>());

//	private final Map<>

	public Model(Kimlik kimlik) {

		this.kimlik = kimlik;

		kimlik.setId(null);

	}

	public void dinleyiciEkle(ModelDinleyici dinleyici) {

		dinleyiciler.add(dinleyici);

	}

	public String getBeaconMesaji() {

		return gson.toJson(kimlik);

	}

	public void setSunucuBagli(boolean arg0) {

		if (sunucuBagli.getAndSet(arg0) != arg0) {

			dinleyicilereSunucuBaglantiDurumuGuncellendi(arg0);

		}

	}

	private void dinleyicilereSunucuBaglantiDurumuGuncellendi(final boolean arg0) {

		dinleyiciler.forEach(e -> e.sunucuBaglantiDurumuGuncellendi(arg0));

	}

}
