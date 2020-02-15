package com.aselsan.rehis.reform.mcsy.model;

import com.aselsan.rehis.reform.mcsy.veritabani.tablolar.Kimlik;
import com.google.gson.Gson;

public class Model {

	private final Kimlik kimlik;

	private final Gson gson = new Gson();

//	private final Map<>

	public Model(Kimlik kimlik) {

		this.kimlik = kimlik;

		kimlik.setId(null);

	}

	public String getBeaconMesaji() {

		return gson.toJson(kimlik);

	}

}
