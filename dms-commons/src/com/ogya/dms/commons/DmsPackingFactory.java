package com.ogya.dms.commons;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import org.msgpack.jackson.dataformat.MessagePackFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ogya.dms.commons.structures.Beacon;
import com.ogya.dms.commons.structures.MessagePojo;

public class DmsPackingFactory {

	private static ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory())
			.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
			.setSerializationInclusion(Include.NON_NULL).setVisibility(PropertyAccessor.ALL, Visibility.NONE)
			.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

	private static ObjectMapper objectMapperServerToClient = objectMapper.copy()
			.addMixIn(Beacon.class, BeaconServerToClientMixin.class)
			.addMixIn(MessagePojo.class, MessagePojoServerToClientMixin.class);
	private static ObjectMapper objectMapperServerToServer = objectMapper.copy()
			.addMixIn(Beacon.class, BeaconServerToServerMixin.class)
			.addMixIn(MessagePojo.class, MessagePojoServerToServerMixin.class);

	public static byte[] pack(Object src) {

		try {

			return objectMapper.writeValueAsBytes(src);

		} catch (Exception e) {

			e.printStackTrace();

		}

		return null;

	}

	public static byte[] packServerToClient(Object src) {

		try {

			return objectMapperServerToClient.writeValueAsBytes(src);

		} catch (Exception e) {

			e.printStackTrace();

		}

		return null;

	}

	public static byte[] packServerToServer(Object src) {

		try {

			return objectMapperServerToServer.writeValueAsBytes(src);

		} catch (Exception e) {

			e.printStackTrace();

		}

		return null;

	}

	public static <T> T unpack(byte[] bytes, Class<T> classOfT) throws Exception {

		return objectMapper.readValue(bytes, classOfT);

	}

	public static <T> List<T> unpackList(byte[] bytes, Class<T> classOfT) throws Exception {

		return objectMapper.readValue(bytes,
				objectMapper.getTypeFactory().constructCollectionType(List.class, classOfT));

	}

	public static <T, U> Map<T, U> unpackMap(byte[] bytes, Class<T> classOfT, Class<U> classOfU) throws Exception {

		return objectMapper.readValue(bytes,
				objectMapper.getTypeFactory().constructMapType(Map.class, classOfT, classOfU));

	}

	private static abstract class BeaconServerToClientMixin {

		@JsonIgnore
		public Boolean local;

	}

	private static abstract class BeaconServerToServerMixin {

		@JsonIgnore
		public Map<InetAddress, InetAddress> localRemoteServerIps;
		@JsonIgnore
		public Boolean local;

	}

	private static abstract class MessagePojoServerToClientMixin {

		@JsonIgnore
		public Long useTimeout;
		@JsonIgnore
		public InetAddress useLocalAddress;

	}

	private static abstract class MessagePojoServerToServerMixin {

		@JsonIgnore
		public Long useTrackingId;
		@JsonIgnore
		public Long useTimeout;
		@JsonIgnore
		public InetAddress useLocalAddress;

	}

}
