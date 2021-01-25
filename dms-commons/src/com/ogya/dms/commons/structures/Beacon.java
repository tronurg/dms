package com.ogya.dms.commons.structures;

import java.net.InetAddress;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class Beacon {

	@SerializedName("a")
	public final String uuid;
	@SerializedName("b")
	public String name;
	@SerializedName("c")
	public String comment;
	@SerializedName("d")
	public Integer status;
	@SerializedName("e")
	public Double lattitude;
	@SerializedName("f")
	public Double longitude;
	@SerializedName("g")
	public String secretId;
	@SerializedName("h")
	public List<InetAddress> remoteInterfaces;
	@SerializedName("i")
	public List<InetAddress> localInterfaces;

	public Beacon(String uuid) {

		this.uuid = uuid;

	}

	public Beacon(String uuid, String name, String comment, Integer status, Double lattitude, Double longitude,
			String secretId) {

		this(uuid);

		this.name = name;
		this.comment = comment;
		this.status = status;
		this.lattitude = lattitude;
		this.longitude = longitude;
		this.secretId = secretId;

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
