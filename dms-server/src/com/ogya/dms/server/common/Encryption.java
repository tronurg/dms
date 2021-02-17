package com.ogya.dms.server.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.StreamingAead;
import com.google.crypto.tink.streamingaead.StreamingAeadConfig;

public class Encryption {

	private static StreamingAead streamingAead;

	public static OutputStream newCompressingAndEncryptingOutputStream(OutputStream outputStream) throws Exception {

		return new ZstdOutputStream(getStreamingAead().newEncryptingStream(outputStream, new byte[0]));

	}

	public static InputStream newDecryptingAndDecompressingInputStream(InputStream inputStream) throws Exception {

		return new ZstdInputStream(getStreamingAead().newDecryptingStream(inputStream, new byte[0]));

	}

	public static byte[] encrypt(byte[] data) throws Exception {

		try (ByteArrayOutputStream out = new ByteArrayOutputStream();
				OutputStream outputStream = getStreamingAead().newEncryptingStream(out, new byte[0])) {

			outputStream.write(data);

			outputStream.flush();

			outputStream.close();

			return out.toByteArray();

		}

	}

	public static byte[] decrypt(byte[] encryptedData, int length) throws Exception {

		try (InputStream inputStream = getStreamingAead()
				.newDecryptingStream(new ByteArrayInputStream(encryptedData, 0, length), new byte[0]);
				ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			int b;

			while ((b = inputStream.read()) != -1) {

				out.write(b);

			}

			out.flush();

			return out.toByteArray();

		}

	}

	private static StreamingAead getStreamingAead() throws Exception {

		if (streamingAead == null) {

			StreamingAeadConfig.register();

			KeysetHandle keysetHandle = CleartextKeysetHandle
					.read(JsonKeysetReader.withFile(new File("./sec/dms.key")));

			streamingAead = keysetHandle.getPrimitive(StreamingAead.class);

		}

		return streamingAead;

	}

}
