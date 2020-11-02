package com.ogya.dms.common.structures;

import java.net.InetAddress;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class Beacon {

	@SerializedName(value = "a")
	public final String uuid;
	@SerializedName(value = "b")
	public String name;
	@SerializedName(value = "c")
	public String comment;
	@SerializedName(value = "d")
	public Integer status;
	@SerializedName(value = "e")
	public Double lattitude;
	@SerializedName(value = "f")
	public Double longitude;
	@SerializedName(value = "g")
	public List<InetAddress> addresses;

	public Beacon(String uuid) {

		this.uuid = uuid;

	}

	public Beacon(String uuid, String name, String comment, Integer status, Double lattitude, Double longitude) {

		this(uuid);

		this.name = name;
		this.comment = comment;
		this.status = status;
		this.lattitude = lattitude;
		this.longitude = longitude;

	}

	public String toJson() {
		return JsonFactory.toJson(this);
	}

	public String toRemoteJson() {
		return JsonFactory.toRemoteJson(this);
	}

	public static Beacon fromJson(String json) throws Exception {
		return JsonFactory.fromJson(json, Beacon.class);
	}

}
