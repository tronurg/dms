package com.ogya.dms.commons;

import java.net.InetAddress;
import java.util.List;

import org.msgpack.jackson.dataformat.MessagePackFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogya.dms.commons.structures.Beacon;

public class DmsPackingFactory {

	private static ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory())
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
			.setSerializationInclusion(Include.NON_NULL);

	private static ObjectMapper objectMapperRemote = objectMapper.copy().addMixIn(Beacon.class,
			BeaconRemoteMixin.class);

	public static byte[] pack(Object src) {

		try {

			return objectMapper.writeValueAsBytes(src);

		} catch (Exception e) {

			e.printStackTrace();

		}

		return null;

	}

	public static byte[] packRemote(Object src) {

		try {

			return objectMapperRemote.writeValueAsBytes(src);

		} catch (Exception e) {

			e.printStackTrace();

		}

		return null;

	}

	public static <T> T unpack(byte[] bytes, Class<T> classOfT) throws Exception {

		return objectMapper.readValue(bytes, classOfT);

	}

	public static <T> List<T> unpackList(byte[] bytes, Class<T> classOfT) throws Exception {

		return objectMapper.readValue(bytes, objectMapper.getTypeFactory().constructArrayType(classOfT));

	}

	private static abstract class BeaconRemoteMixin {

		@JsonIgnore
		public List<InetAddress> remoteInterfaces;
		@JsonIgnore
		public List<InetAddress> localInterfaces;

	}

}
