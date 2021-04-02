package com.ogya.dms.commons.structures;

import java.net.InetAddress;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Beacon {

	@JsonProperty("a")
	public String uuid;
	@JsonProperty("b")
	public String name;
	@JsonProperty("c")
	public String comment;
	@JsonProperty("d")
	public Integer status;
	@JsonProperty("e")
	public Double lattitude;
	@JsonProperty("f")
	public Double longitude;
	@JsonProperty("g")
	public String secretId;
	@JsonProperty("h")
	public Map<InetAddress, InetAddress> localRemoteServerIps;

	public Beacon() {
		super();
	}

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

}
