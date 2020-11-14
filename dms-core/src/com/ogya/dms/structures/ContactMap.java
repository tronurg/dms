package com.ogya.dms.structures;

import java.util.HashMap;
import java.util.Map;

import com.ogya.dms.common.CommonMethods;

public class ContactMap {

	public final Map<Long, String> map = new HashMap<Long, String>();

	public String toJson() {
		return CommonMethods.toJson(this);
	}

	public static ContactMap fromJson(String json) throws Exception {
		return CommonMethods.fromJson(json, ContactMap.class);
	}

}
