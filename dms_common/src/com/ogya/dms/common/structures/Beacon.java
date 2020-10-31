package com.ogya.dms.common.structures;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Beacon {

	public final String uuid;
	public String name;
	public String comment;
	public Integer status;
	public Double lattitude;
	public Double longitude;
	public final List<InetAddress> addresses = Collections.synchronizedList(new ArrayList<InetAddress>());

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
