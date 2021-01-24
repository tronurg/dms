package com.ogya.dms.commons.structures;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

class JsonFactory {

	private static final List<String> gsonRemoteExcludedNames = Arrays.asList("remoteInterfaces", "localInterfaces");

	private static final Gson gson = new GsonBuilder()
			.registerTypeAdapter(ContentType.class, new TypeAdapter<ContentType>() {

				@Override
				public ContentType read(JsonReader reader) throws IOException {
					if (reader.peek() == JsonToken.NULL) {
						reader.nextNull();
						return null;
					}
					return ContentType.values()[reader.nextInt()];
				}

				@Override
				public void write(JsonWriter writer, ContentType value) throws IOException {
					if (value == null) {
						writer.nullValue();
						return;
					}
					writer.value(value.ordinal());
				}

			}).create();

	private static Gson gsonRemote = new GsonBuilder().addSerializationExclusionStrategy(new ExclusionStrategy() {

		@Override
		public boolean shouldSkipField(FieldAttributes arg0) {
			return gsonRemoteExcludedNames.contains(arg0.getName());
		}

		@Override
		public boolean shouldSkipClass(Class<?> arg0) {
			return false;
		}

	}).create();

	static String toJson(Object src) {

		return gson.toJson(src);

	}

	static String toRemoteJson(Object src) {

		return gsonRemote.toJson(src);

	}

	static <T> T fromJson(String json, Class<T> classOfT) throws Exception {

		T result = gson.fromJson(json, classOfT);

		if (result == null)
			throw new Exception();

		return result;

	}

}
