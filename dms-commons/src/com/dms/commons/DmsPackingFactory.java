package com.dms.commons;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.msgpack.jackson.dataformat.MessagePackFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.dms.commons.structures.Beacon;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class DmsPackingFactory {

	private static ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory())
			.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
			.setSerializationInclusion(Include.NON_NULL).setVisibility(PropertyAccessor.ALL, Visibility.NONE)
			.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

	private static ObjectMapper objectMapperBeaconServerToServer = objectMapper.copy().addMixIn(Beacon.class,
			BeaconServerToServerMixin.class);

	public static byte[] pack(Object src) {

		try {

			return objectMapper.writeValueAsBytes(src);

		} catch (Exception e) {

			e.printStackTrace();

		}

		return null;

	}

	public static byte[] packBeaconServerToServer(Beacon src) {

		try {

			return objectMapperBeaconServerToServer.writeValueAsBytes(src);

		} catch (Exception e) {

			e.printStackTrace();

		}

		return null;

	}

	public static <T> T unpack(byte[] bytes, Class<T> classOfT) throws Exception {

		return objectMapper.readValue(bytes, classOfT);

	}

	public static <T> T unpack(ByteBuffer byteBuffer, Class<T> classOfT) throws Exception {

		return objectMapper.readValue(byteBuffer.array(), byteBuffer.position(), byteBuffer.remaining(), classOfT);

	}

	public static <T> List<T> unpackList(byte[] bytes, Class<T> classOfT) throws Exception {

		return objectMapper.readValue(bytes,
				objectMapper.getTypeFactory().constructCollectionType(List.class, classOfT));

	}

	public static <T, U> Map<T, U> unpackMap(byte[] bytes, Class<T> classOfT, Class<U> classOfU) throws Exception {

		return objectMapper.readValue(bytes,
				objectMapper.getTypeFactory().constructMapType(Map.class, classOfT, classOfU));

	}

	private static abstract class BeaconServerToServerMixin {

		@JsonIgnore
		public Map<InetAddress, InetAddress> localRemoteServerIps;
		@JsonIgnore
		public String serverUuid;

	}

}
