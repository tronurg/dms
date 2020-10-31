package com.ogya.dms.common.structures;

import java.net.InetAddress;
import java.util.List;

public class Beacon {

	public final String uuid;
	public String name;
	public String comment;
	public Integer status;
	public Double lattitude;
	public Double longitude;
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

}
